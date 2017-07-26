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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.customvalidation.CustomValidationFailedEvent;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.general.EbMSConstants;
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

import static org.junit.Assert.*;

/**
 * Created at 17:39 24.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PerformCustomValidationsTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private PerformCustomValidations handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PerformCustomValidationsTest.class.getClassLoader()
                .getResource("customvalidation").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PerformCustomValidations();
        LogManager.getRootLogger().addAppender(mockAppender);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
        core.getPModeSet().removeAll();
    }

    @Test
     public void testDoProcessing() throws Exception {
        String stopValidationOn = "warn";
        String rejectMessageOn = "warn";
        ProcessingState expPS = ProcessingState.RECEIVED;
        performCustomValidations(stopValidationOn, rejectMessageOn, expPS);
    }

    @Test
    public void testDoProcessingRejectOnFailure() throws Exception {
        String stopValidationOn = "warn";
        String rejectMessageOn = "failure";
        ProcessingState expPS = ProcessingState.FAILURE;
        performCustomValidations(stopValidationOn, rejectMessageOn, expPS);
    }

    /**
     * Performing common custom validation testing logic
     * @param stopValidationOn
     * @param rejectMessageOn
     * @param expPS expected ProcessingState
     * @throws Exception
     */
    private void performCustomValidations(String stopValidationOn, String rejectMessageOn,
                                          ProcessingState expPS) throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("customvalidation/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        String pmodeId = "some_pmode_id";
        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);

        // We need to set the PMode id value separately because
        // the agreement pmode & userMessage pmode are different
        // Currently we just set the same value
        userMessage.setPModeId(pmodeId);

        // Setting input message property
        StorageManager updateManager = core.getStorageManager();
        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        Leg leg = new Leg();

        EventHandlerConfig eventHandlerConfig = new EventHandlerConfig();
        eventHandlerConfig.setFactoryClass(
                "org.holodeckb2b.common.testhelpers.events.TestEventHandlerFactory");

        ArrayList<Class<? extends IMessageProcessingEvent>> eventsList = new ArrayList<>();
        eventsList.add(CustomValidationFailedEvent.class);
        eventHandlerConfig.setHandledEvents(eventsList);

        leg.addMessageProcessingEventConfiguration(eventHandlerConfig);

        UserMessageFlow flow = new UserMessageFlow();
        CustomValidationSpec validationSpec =
                new CustomValidationSpec(stopValidationOn, rejectMessageOn);
        List<IMessageValidatorConfiguration> validators = new ArrayList<>();
        ValidatorConfig vc = new ValidatorConfig();
        vc.setId("some_validator_id");
        vc.setFactory("org.holodeckb2b.customvalidation.helpers.CustomValidator$Factory");
        validators.add(vc);
        validationSpec.setValidators(validators);
        flow.setCustomValidationConfiguration(validationSpec);

        leg.setUserMessageFlow(flow);

        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(expPS, userMessageEntity.getCurrentProcessingState().getState());
        // When validation is successful there should be no error in the message context
        if(!expPS.equals(ProcessingState.FAILURE)) {
            assertNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
        }
    }
}