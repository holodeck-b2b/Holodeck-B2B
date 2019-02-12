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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.ErrorHandlingConfig;
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
public class DetermineErrorReportingTest {


    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private DetermineErrorReporting handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = DetermineErrorReportingTest.class.getClassLoader()
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
        handler = new DetermineErrorReporting();
        ModuleConfiguration moduleDescr = new ModuleConfiguration("test", null);
        moduleDescr.addParameter(new Parameter("HandledMessagingProtocol", "TEST"));
        HandlerDescription handlerDescr = new HandlerDescription();
        handlerDescr.setParent(moduleDescr);
        handler.init(handlerDescr);        
    }

    @After
    public void tearDown() throws Exception {
        core.getPModeSet().removeAll();
    }

    /**
     * Error that references the specific message unit, which is not a pull request and has no specific error handling
     * config
     */
    @Test
    public void testDefaultConfig() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
    	final String pmodeId = "pmode_id_01";
        final String msgId = "some_msg_id_01";
        
        // Prepare message in error
    	UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
        userMessage.setMessageId(msgId);
        userMessage.setPModeId(pmodeId);
        IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);

        // Setup P-Mode
        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId(pmodeId);
        Leg leg = new Leg();
        leg.setUserMessageFlow(new UserMessageFlow());
        pmode.addLeg(leg);
        core.getPModeSet().add(pmode);

        // Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);
        
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);

        // Create the Error Signal referencing the message unit
        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");
        ErrorMessage errorMsg = new ErrorMessage(error1);
        errorMsg.setRefToMessageId(userMessage.getMessageId());
        errorMsg.setPModeId(pmodeId);
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
        newSignals.add(storageManager.storeOutGoingMessageUnit(errorMsg));
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        assertTrue((Boolean) mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        															mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertFalse(Utils.isNullOrEmpty(errors));
        assertEquals(1, errors.size());
        assertEquals(userMessage.getMessageId(), errors.iterator().next().getRefToMessageId());
    }

    /**
     * Error that references the pull request
     */
    @Test
    public void testErrorRefsPullRequest() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        String msgId = "some_msg_id_01";
        
        // Prepare message in error
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setMessageId(msgId);
        IPullRequestEntity pullRequestEntity = storageManager.storeIncomingMessageUnit(pullRequest);
        
        // Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST, pullRequestEntity);

        // Create the Error Signal referencing the message unit
        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(pullRequestEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");
        ErrorMessage errorMsg = new ErrorMessage(error1);
        errorMsg.setRefToMessageId(pullRequest.getMessageId());
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
        newSignals.add(storageManager.storeOutGoingMessageUnit(errorMsg));
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
       
        assertNotNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        assertTrue((Boolean) mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        assertNotNull(mc.getProperty(MessageContextProperties.OUT_ERRORS));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        														mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertFalse(Utils.isNullOrEmpty(errors));
        assertEquals(1, errors.size());
        assertEquals(pullRequest.getMessageId(), errors.iterator().next().getRefToMessageId());
    }

    /**
     * Error that references the specific message unit, which is not a pull request and has set error handling
     * config to report async
     */
    @Test
    public void testAsyncReporting() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
    	final String pmodeId = "pmode_id_01";
        final String msgId = "some_msg_id_01";
        
        // Prepare message in error
    	UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
        userMessage.setMessageId(msgId);
        userMessage.setPModeId(pmodeId);
        IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);

        // Setup P-Mode
        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId(pmodeId);
        Leg leg = new Leg();
        UserMessageFlow umFlow = new UserMessageFlow();
        ErrorHandlingConfig errHandling = new ErrorHandlingConfig();
        errHandling.setPattern(ReplyPattern.CALLBACK);
        umFlow.setErrorHandlingConfiguration(errHandling);
        leg.setUserMessageFlow(umFlow);
        pmode.addLeg(leg);
        core.getPModeSet().add(pmode);

        // Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);
        
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);

        // Create the Error Signal referencing the message unit
        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");
        ErrorMessage errorMsg = new ErrorMessage(error1);
        errorMsg.setRefToMessageId(userMessage.getMessageId());
        errorMsg.setPModeId(pmodeId);
        IErrorMessageEntity errorSig = storageManager.storeOutGoingMessageUnit(errorMsg);
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
        newSignals.add(errorSig);
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        															mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertTrue(Utils.isNullOrEmpty(errors));
        assertEquals(ProcessingState.READY_TO_PUSH, errorSig.getCurrentProcessingState().getState());
    }

    /**
     * Error that references a specific message unit but has no P-Mode associated
     */
    @Test
    public void testNoPModeButWithRef() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        final String msgId = "some_msg_id_01";
        
        // Prepare message in error
    	UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
        userMessage.setMessageId(msgId);
        IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);

        // Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);        
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);

        // Create the Error Signal referencing the message unit
        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");
        ErrorMessage errorMsg = new ErrorMessage(error1);
        errorMsg.setRefToMessageId(userMessage.getMessageId());
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
        newSignals.add(storageManager.storeOutGoingMessageUnit(errorMsg));
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        assertTrue((Boolean) mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        															mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertFalse(Utils.isNullOrEmpty(errors));
        assertEquals(1, errors.size());
        assertEquals(userMessage.getMessageId(), errors.iterator().next().getRefToMessageId());
    }

    /**
     * Error without reference but with only failed messages
     */
    @Test
    public void testNoRefAllFailed() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
    	final String msgId = "some_msg_id_01";
    	
    	// Prepare message in error
    	UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
    	userMessage.setMessageId(msgId);
    	IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);
    	storageManager.setProcessingState(userMessageEntity, ProcessingState.FAILURE);
    	
    	// Prepare msg ctx
    	MessageContext mc = new MessageContext();
    	mc.setServerSide(true);
    	mc.setFLOW(MessageContext.IN_FLOW);        
    	mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);
    	
    	// Create the Error Signal referencing the message unit
    	EbmsError error1 = new EbmsError();
    	error1.setErrorDetail("Some error for testing.");
    	ErrorMessage errorMsg = new ErrorMessage(error1);
    	IErrorMessageEntity errorSig = storageManager.storeOutGoingMessageUnit(errorMsg);
    	final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
    	newSignals.add(errorSig);
    	mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
    	
    	try {
    		Handler.InvocationResponse invokeResp = handler.invoke(mc);
    		assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
		assertNotNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
		assertTrue((Boolean) mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
		Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
																	mc.getProperty(MessageContextProperties.OUT_ERRORS);
		assertFalse(Utils.isNullOrEmpty(errors));
		assertEquals(1, errors.size());
    }
    
    /**
     * Error without reference conflicting with successfully processed messages
     */
    @Test
    public void testNoRefConflict() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
    	final String msgId = "some_msg_id_01";
    	
    	// Prepare message in error
    	UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
    	userMessage.setMessageId(msgId);
    	IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);
    	storageManager.setProcessingState(userMessageEntity, ProcessingState.DELIVERED);
    	
    	// Prepare msg ctx
    	MessageContext mc = new MessageContext();
    	mc.setServerSide(true);
    	mc.setFLOW(MessageContext.IN_FLOW);        
    	mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);
    	
    	// Create the Error Signal referencing the message unit
    	EbmsError error1 = new EbmsError();
    	error1.setErrorDetail("Some error for testing.");
    	ErrorMessage errorMsg = new ErrorMessage(error1);
    	IErrorMessageEntity errorSig = storageManager.storeOutGoingMessageUnit(errorMsg);
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
        newSignals.add(errorSig);
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
    	
    	try {
    		Handler.InvocationResponse invokeResp = handler.invoke(mc);
    		assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
        assertNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        															mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertTrue(Utils.isNullOrEmpty(errors));
        assertEquals(ProcessingState.DONE, errorSig.getCurrentProcessingState().getState()); 
    }

    /**
     * Error that references a specific message unit that was received as response but has no P-Mode associated
     */
    @Test
    public void testNoPModeOnResponse() throws Exception {
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        final String msgId = "some_msg_id_01";
        
        // Prepare message in error
    	UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
        userMessage.setMessageId(msgId);
        IUserMessageEntity userMessageEntity = storageManager.storeIncomingMessageUnit(userMessage);

        // Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setServerSide(false);
        mc.setFLOW(MessageContext.IN_FLOW);        
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);

        // Create the Error Signal referencing the message unit
        EbmsError error1 = new EbmsError();
        error1.setRefToMessageInError(userMessageEntity.getMessageId());
        error1.setErrorDetail("Some error for testing.");
        ErrorMessage errorMsg = new ErrorMessage(error1);
        errorMsg.setRefToMessageId(userMessage.getMessageId());
        IErrorMessageEntity errorSig = storageManager.storeOutGoingMessageUnit(errorMsg);
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>();
        newSignals.add(errorSig);
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNull(mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>) 
        															mc.getProperty(MessageContextProperties.OUT_ERRORS);
        assertTrue(Utils.isNullOrEmpty(errors));
        assertEquals(ProcessingState.DONE, errorSig.getCurrentProcessingState().getState());        
    }
    
    
}