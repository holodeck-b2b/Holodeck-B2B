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

import java.util.Collection;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProcessGeneratedErrorsTest {

	private ProcessGeneratedErrors handler;

	@BeforeClass
    public static void setUpClass() throws Exception {
        String baseDir = ProcessGeneratedErrorsTest.class.getClassLoader().getResource("handlers").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));
    }
	
    @AfterClass
    public static void tearDownClass() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
    }

    @Before
    public void setUp() throws Exception {
        handler = new ProcessGeneratedErrors();
        ModuleConfiguration moduleDescr = new ModuleConfiguration("test", null);
        moduleDescr.addParameter(new Parameter("HandledMessagingProtocol", "TEST"));
        HandlerDescription handlerDescr = new HandlerDescription();
        handlerDescr.setParent(moduleDescr);
        handler.init(handlerDescr);        
    }
    
    /**
     * Test anonymous errors
     */
    @Test
    public void testErrorsWithoutRef() {
    	// Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        
        // Create the Error Signal 
        EbmsError error1 = new EbmsError();
        error1.setErrorDetail("Some error for testing.");
        procCtx.addGeneratedError(error1);
        EbmsError error2 = new EbmsError();
        error2.setErrorDetail("Some other error for testing.");
        procCtx.addGeneratedError(error2);
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, handler.invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        Collection<IErrorMessageEntity> errorSigs = procCtx.getSendingErrors();
        assertFalse(Utils.isNullOrEmpty(errorSigs));
        assertEquals(1, errorSigs.size());
        IErrorMessageEntity errSig = errorSigs.iterator().next();
        assertNull(errSig.getPModeId());
        assertNull(errSig.getRefToMessageId());
        assertNotNull(errSig.getErrors());
        assertEquals(2, errSig.getErrors().size());
    }
    
    /**
     * Test errors with same reference
     * @throws PersistenceException 
     */
    @Test
    public void testErrorsSameMsg() throws PersistenceException {
    	// Prepare message in error
    	UserMessage usrMsg = new UserMessage();
    	usrMsg.setMessageId(MessageIdUtils.createMessageId());
    	usrMsg.setPModeId("some-pmodeid-001");
    	IUserMessageEntity usrMsgEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(usrMsg);
    	
    	// Prepare msg ctx
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);    	
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
       
    	procCtx.setUserMessage(usrMsgEntity);
    	
    	// Create the Error Signal 
    	EbmsError error1 = new EbmsError();
    	error1.setErrorDetail("Some error for testing.");
    	error1.setRefToMessageInError(usrMsg.getMessageId());
    	procCtx.addGeneratedError(error1);
    	EbmsError error2 = new EbmsError();
    	error2.setErrorDetail("Some other error for testing.");
    	error2.setRefToMessageInError(usrMsg.getMessageId());
    	procCtx.addGeneratedError(error2);
    	
    	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, handler.invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	Collection<IErrorMessageEntity> errorSigs = procCtx.getSendingErrors();
        assertFalse(Utils.isNullOrEmpty(errorSigs));
    	assertEquals(1, errorSigs.size());
    	IErrorMessageEntity errSig = errorSigs.iterator().next();
    	assertEquals(usrMsg.getPModeId(), errSig.getPModeId());
    	assertEquals(usrMsg.getMessageId(), errSig.getRefToMessageId());
    	assertNotNull(errSig.getErrors());
    	assertEquals(2, errSig.getErrors().size());
    }
    
    /**
     * Test errors with different references
     * @throws PersistenceException 
     */
    @Test
    public void testErrorsDifferentMsg() throws PersistenceException {
    	// Prepare message in error
    	UserMessage usrMsg = new UserMessage();
    	usrMsg.setMessageId(MessageIdUtils.createMessageId());
    	usrMsg.setPModeId("some-pmodeid-001");
    	IUserMessageEntity usrMsgEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(usrMsg);
    	
    	PullRequest pullReq = new PullRequest();
    	pullReq.setMessageId(MessageIdUtils.createMessageId());
    	pullReq.setPModeId("some-pmodeid-002");
    	IPullRequestEntity pullReqEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(pullReq);
    	
    	// Prepare msg ctx
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(usrMsgEntity);
    	procCtx.setPullRequest(pullReqEntity);
    	
    	// Create the Errors 
    	EbmsError error1 = new EbmsError();
    	error1.setErrorDetail("error-1");
    	error1.setRefToMessageInError(usrMsg.getMessageId());
    	procCtx.addGeneratedError(error1);
    	EbmsError error2 = new EbmsError();
    	error2.setErrorDetail("error-2");
    	error2.setRefToMessageInError(pullReq.getMessageId());
    	procCtx.addGeneratedError(error2);
    	
       	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, handler.invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	Collection<IErrorMessageEntity> errorSigs = procCtx.getSendingErrors();
    	assertFalse(Utils.isNullOrEmpty(errorSigs));
    	assertEquals(2, errorSigs.size());
    	
    	assertTrue(errorSigs.parallelStream()
    						.allMatch(e -> !Utils.isNullOrEmpty(e.getRefToMessageId())
    									&& !Utils.isNullOrEmpty(e.getErrors())
    									&& ( ( usrMsg.getMessageId().equals(e.getRefToMessageId())
    										  && e.getErrors().iterator().next().getErrorDetail().equals(error1.getErrorDetail())
    										 )
    									   || ( pullReq.getMessageId().equals(e.getRefToMessageId())
    										   && e.getErrors().iterator().next().getErrorDetail().equals(error2.getErrorDetail())
    	    								  )
    									   )));
    	
    }
    
    
    
}
