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
package org.holodeckb2b.ebms3.handlers.outflow;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.testhelpers.Config;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.*;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.namespace.QName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.holodeckb2b.core.testhelpers.TestUtils.eventContainsMsg;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created at 15:44 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PrepareResponseMessageTest {

    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static HolodeckB2BTestCore core;

    private static String baseDir;

    private PrepareResponseMessage handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = TestUtils.getPath(PrepareResponseMessageTest.class, "handlers");
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PrepareResponseMessage();
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testDoProcessingOfTheUserMessage() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        UserMessage userMessage = UserMessageElement.readElement(umElement);

        String pmodeId =
                userMessage.getCollaborationInfo().getAgreement().getPModeId();
        userMessage.setPModeId(pmodeId);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.OUT_FLOW);

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                core.getStorageManager().storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE,
                userMessageEntity);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);

        mc.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Response contains an user message unit";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    @Test
    public void testDoProcessingOfTheResponseReceipt() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.OUT_FLOW);

        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                env.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);

        ReceiptElement.createElement(headerBlock, receipt);

        StorageManager updateManager = core.getStorageManager();
        IReceiptEntity receiptEntity =
                updateManager.storeIncomingMessageUnit(receipt);
        mc.setProperty(MessageContextProperties.RESPONSE_RECEIPT, receiptEntity);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);

        mc.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Response contains a receipt signal";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    @Test
    public void testDoProcessingOfTheErrors() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        ErrorSignalElement.createElement(headerBlock, errorMessage);

        StorageManager updateManager = core.getStorageManager();
        IErrorMessageEntity errorMessageEntity =
                updateManager.storeIncomingMessageUnit(errorMessage);

        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.OUT_ERRORS,
                errorMessageEntities);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);

        mc.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Response does contain one error signal";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    @Test
    public void testDoProcessingOfMultipleErrors() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        StorageManager updateManager = core.getStorageManager();

        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();

        // Adding first error message
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code1");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        ErrorSignalElement.createElement(headerBlock, errorMessage);
        IErrorMessageEntity errorMessageEntity =
                updateManager.storeIncomingMessageUnit(errorMessage);
        errorMessageEntities.add(errorMessageEntity);

        // Adding second error message
        errorMessage = new ErrorMessage();
        errors = new ArrayList<>();
        ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code2");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        ErrorSignalElement.createElement(headerBlock, errorMessage);
        errorMessageEntity =
                updateManager.storeIncomingMessageUnit(errorMessage);
        errorMessageEntities.add(errorMessageEntity);

        mc.setProperty(MessageContextProperties.OUT_ERRORS,
                errorMessageEntities);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);

        mc.setOperationContext(operationContext);

        InternalConfiguration config = core.getConfiguration();

        Field f = Config.class.getDeclaredField("allowSignalBundling");
        f.setAccessible(true);
        f.setBoolean(config, true);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Response contains multiple error signals";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }
}