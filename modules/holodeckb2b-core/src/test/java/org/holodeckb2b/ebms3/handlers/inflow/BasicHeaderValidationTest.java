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
import org.holodeckb2b.common.messagemodel.*;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PullRequestElement;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.holodeckb2b.core.testhelpers.TestUtils.eventContainsMsg;
import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 23:49 29.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicHeaderValidationTest {
    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    static final QName RECEIPT_CHILD_ELEMENT_NAME = new QName("ReceiptChild");

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private BasicHeaderValidation handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = TestUtils.getPath(BasicHeaderValidationTest.class, "handlers");
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }
    @Before
    public void setUp() throws Exception {
        handler = new BasicHeaderValidation();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testDoProcessingOfUserMessage() throws Exception {
        // todo Can we remove dependency from the xml data?
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);
        // Setting input message property
        StorageManager updateManager = core.getStorageManager();
        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log messages to make sure handler validated
        // the user message successfully
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Received User Message satisfies basic validations";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    @Test
    public void testDoProcessingOfPullRequest() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding PullRequest
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setMessageId("some_id");
        // there should not be ref to message id
//        pullRequest.setRefToMessageId("some_ref_to_message_id");
        pullRequest.setTimestamp(new Date());
        PullRequestElement.createElement(headerBlock, pullRequest);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        // Setting input PullRequest property
        StorageManager updateManager = core.getStorageManager();
        IPullRequestEntity pullRequestEntity =
                updateManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST,
                pullRequestEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log messages to make sure handler validated
        // the pull request successfully
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Received Pull Request satisfies basic validations";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    // todo the following test fails. Correct it and uncomment
    @Test
    public void testDoProcessingOfReciepts() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding Receipts
        Receipt receipt = new Receipt();
        receipt.setMessageId("some_message_id");
        receipt.setRefToMessageId("some_ref_to_message_id");
        receipt.setTimestamp(new Date());
        ArrayList<OMElement> receiptContent = new ArrayList<>();

        OMElement receiptChildElement =
                headerBlock.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        receiptChildElement.setText("eb3:UserMessage");
        System.out.println("receiptChildElement: " + receiptChildElement);

        receiptContent.add(receiptChildElement);

        receipt.setContent(receiptContent);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        // Setting input Receipt property
        StorageManager updateManager = core.getStorageManager();

        IReceiptEntity receiptEntity =
                updateManager.storeIncomingMessageUnit(receipt);
        ArrayList<IReceiptEntity> receiptEntities = new ArrayList<>();
        receiptEntities.add(receiptEntity);
        mc.setProperty(MessageContextProperties.IN_RECEIPTS,
                receiptEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log messages to make sure handler validated
        // the pull request successfully
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Received Receipt satisfies basic validations";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    @Test
    public void testDoProcessingOfErrors() throws Exception {
        // Initialising Errors
        ErrorMessage error = new ErrorMessage();
        error.setMessageId("some_message_id");
        error.setTimestamp(new Date());
        ArrayList<IEbmsError> errors = new ArrayList<>();
        errors.add(new EbmsError());
        error.setErrors(errors);

        MessageContext mc = new MessageContext();

        // Setting input Receipt property
        StorageManager updateManager = core.getStorageManager();

        IErrorMessageEntity errorMessageEntity =
                updateManager.storeIncomingMessageUnit(error);
        System.out.println("errors: " + errorMessageEntity.getErrors());
        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.IN_ERRORS,
                errorMessageEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log messages to make sure handler validated
        // the pull request successfully
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Received Error satisfies basic validations";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }
}