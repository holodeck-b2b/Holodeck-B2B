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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.UserMessageFlow;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 12:07 15.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessGeneratedErrorsTest {


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
    }

    @After
    public void tearDown() throws Exception {
        core.getPModeSet().removeAll();
    }

    /**
     * Test the case when there is a single error that references the specific
     * message unit, which is not a pull request
     */
    @Test
    public void testDoProcessingTheErrorRefsUserMessage() throws Exception {
        UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));

        final String pmodeId = "pmode_id_01";
        final String msgId = "some_msg_id_01";
        userMessage.setMessageId(msgId);
        userMessage.setPModeId(pmodeId);

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

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);

        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.OUT_ERRORS));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        														mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertTrue(!Utils.isNullOrEmpty(errors));
        assertEquals(1, errors.size());
        assertEquals(userMessage.getMessageId(), errors.iterator().next().getRefToMessageId());
    }

    /**
     * Test the case when there is a single error that references the pull request
     */
    @Test
    public void testDoProcessingTheErrorRefsPullRequest() throws Exception {
        String msgId = "some_msg_id_01";
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setMessageId(msgId);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        IPullRequestEntity pullRequestEntity =
                storageManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST, pullRequestEntity);

        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(pullRequestEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");

        // Adding generated error
        MessageContextUtils.addGeneratedError(mc, error1);
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue((Boolean)mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));

        assertNotNull(mc.getProperty(MessageContextProperties.OUT_ERRORS));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        														mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertTrue(!Utils.isNullOrEmpty(errors));
        assertEquals(1, errors.size());
        assertEquals(pullRequest.getMessageId(), errors.iterator().next().getRefToMessageId());
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

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

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

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

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

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        storageManager.setProcessingState(userMessageEntity, ProcessingState.FAILURE);
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

    }
}