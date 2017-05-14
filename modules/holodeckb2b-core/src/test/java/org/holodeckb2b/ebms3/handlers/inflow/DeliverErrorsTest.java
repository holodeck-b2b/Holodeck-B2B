/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.holodeckb2b.core.testhelpers.TestUtils.eventContainsMsg;
import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 12:04 15.03.17
 *
 * Checked for cases coverage (24.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class DeliverErrorsTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ProcessErrors processErrorsHandler;

    private DeliverErrors handler;

    private UserMessage userMessage;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = TestUtils.getPath(DeliverErrorsTest.class, "handlers");
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }
    @Before
    public void setUp() throws Exception {
        processErrorsHandler = new ProcessErrors();
        // Executed after org.holodeckb2b.ebms3.handlers.inflow.ProcessErrors handler
        handler = new DeliverErrors();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
        LogManager.getRootLogger().removeAppender(mockAppender);
        core.getPModeSet().removeAll();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        String msgId = "some_msg_id_01";
        UserMessage userMessage = UserMessageElement.readElement(umElement);

        String pmodeId =
                userMessage.getCollaborationInfo().getAgreement().getPModeId();
        userMessage.setPModeId(pmodeId);
        userMessage.setMessageId(msgId);

        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);

        Leg leg = new Leg();

        UserMessageFlow umFlow = new UserMessageFlow();
        ErrorHandlingConfig errorHandlingConfig = new ErrorHandlingConfig();
        errorHandlingConfig.setNotifyErrorToBusinessApplication(true);
        DeliverySpecification deliverySpecification = new DeliverySpecification();
        deliverySpecification.setId("delivery_spec_id");
        errorHandlingConfig.setErrorDelivery(deliverySpecification);
        umFlow.setErrorHandlingConfiguration(errorHandlingConfig);

        leg.setUserMessageFlow(umFlow);
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        String errorId = "error_id";
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        ebmsError.setRefToMessageInError(msgId);
        ebmsError.setMessage("some error message");

        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        errorMessage.setRefToMessageId(msgId);
        errorMessage.setMessageId("error_id");

        StorageManager storageManager = core.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        // Setting input errors property
        ErrorSignalElement.createElement(headerBlock, errorMessage);
        IErrorMessageEntity errorMessageEntity =
                storageManager.storeIncomingMessageUnit(errorMessage);
        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.IN_ERRORS,
                errorMessageEntities);

        try {
            Handler.InvocationResponse invokeResp = processErrorsHandler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.READY_FOR_DELIVERY,
                errorMessageEntity.getCurrentProcessingState().getState());

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.DONE,
                errorMessageEntity.getCurrentProcessingState().getState());

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        // Check that error is successfully processed by ProcessErrors handler
        String msg1 = "Processed Error Signal ["+errorId+"]";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg1));
        // Check that error is successfully delivered by DeliverErrors handler
        String msg2 = "Error successfully delivered!";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg2));
    }
}