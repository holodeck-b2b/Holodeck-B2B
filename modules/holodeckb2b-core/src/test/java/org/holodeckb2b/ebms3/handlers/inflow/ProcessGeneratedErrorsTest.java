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
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PullRequestElement;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.UserMessageFlow;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created at 12:07 15.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessGeneratedErrorsTest {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ProcessGeneratedErrors handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = ProcessGeneratedErrorsTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
    }

    @Before
    public void setUp() throws Exception {
        handler = new ProcessGeneratedErrors();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
        core.getPModeSet().removeAll();
    }

    /**
     * Test the case when there is a single error that references the specific
     * message unit, which is not a pull request
     */
    @Test
    public void testDoProcessingTheErrorRefsUserMessage() throws Exception {
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
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId(pmodeId);

        Leg leg = new Leg();
        leg.setUserMessageFlow(new UserMessageFlow());
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = core.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);
        mc.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 error(s) were generated during this in flow";
        String msg1 = "Create the Error Signal and save to database";
        String msg2 = "Check type of the message unit in error";
        String msg3 = "This error signal should be sent as a response";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg3));
    }

    /**
     * Test the case when there is a single error that references the pull request
     */
    @Test
    public void testDoProcessingTheErrorRefsPullRequest() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        String msgId = "some_msg_id_01";
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setMessageId(msgId);

        PullRequestElement.createElement(headerBlock, pullRequest);

        StorageManager storageManager = core.getStorageManager();
        IPullRequestEntity pullRequestEntity =
                storageManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST, pullRequestEntity);

        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(pullRequestEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);
        mc.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue((Boolean)mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 error(s) were generated during this in flow";
        String msg1 = "Create the Error Signal and save to database";
        String msg2 = "Check type of the message unit in error";
        String msg3 = "Message unit in error is PullRequest, error must be sent as response";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg3));
    }

    /**
     * Test the case when there is a single error that has no reference to the specific
     * message unit
     * @throws Exception
     */
    @Test
    public void testDoProcessingIfNoRefToMU() throws Exception {
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
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId(pmodeId);

        Leg leg = new Leg();
        leg.setUserMessageFlow(new UserMessageFlow());
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = core.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        EbmsError error1 = new EbmsError();
        // We test the case when ref to message unit is not set
//        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(mc);
        mc.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 error(s) were generated during this in flow";
        String msg1 = "Create the Error Signal and save to database";
        String msg2 = "Error without reference can not be sent because "
                + "successful message units exist or message received as response";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.WARN, msg2));
    }

    /**
     * Test the case when there is a single error that has no reference to the specific
     * message unit and we are initiating the flow
     * @throws Exception
     */
    @Test
    public void testDoProcessingIfNoRefToMUAndInflow() throws Exception {
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
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId(pmodeId);

        Leg leg = new Leg();
        leg.setUserMessageFlow(new UserMessageFlow());
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = core.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        EbmsError error1 = new EbmsError();
        // We test the case when ref to message unit is not set
//        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));

        assertTrue((Boolean)mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 error(s) were generated during this in flow";
        String msg1 = "Create the Error Signal and save to database";
        String msg2 = "All message units failed to process successfully, "
                + "anonymous error can be sent";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
    }

    /**
     * Test the case when there is a single error that has no reference to the specific
     * message unit and all message unit failed to process successfully
     * @throws Exception
     */
    @Test
    public void testDoProcessingIfNoRefToMUAndAllMUFailed() throws Exception {
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
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId(pmodeId);

        Leg leg = new Leg();
        leg.setUserMessageFlow(new UserMessageFlow());
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = core.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        EbmsError error1 = new EbmsError();
        // We test the case when ref to message unit is not set
//        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));

        assertTrue((Boolean)mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 error(s) were generated during this in flow";
        String msg1 = "Create the Error Signal and save to database";
        String msg2 = "All message units failed to process successfully, "
                + "anonymous error can be sent";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
    }
}