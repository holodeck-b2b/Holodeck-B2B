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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handlers.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.ReceiptConfiguration;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 23:40 29.01.17
 *
 * Checked for cases coverage (11.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CheckSentResultTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    
    @After
    public void tearDown() throws Exception {
        HolodeckB2BCore.getPModeSet().removeAll();
    }

    @Test
    public void testDoProcessing() throws Exception {
        
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        
    	PMode pmode = TestUtils.create1WaySendPushPMode();        
        HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        // Setting input message property
    	UserMessage usrMessage = new UserMessage();
    	usrMessage.setPModeId(pmode.getId());
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(usrMessage);
        
        procCtx.setUserMessage(userMessageEntity);
        
        try {
            Handler.InvocationResponse invokeResp = new CheckSentResult().invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.SENDING, userMessageEntity.getCurrentProcessingState().getState());
    }
    
    @Test
    public void testSuccessNoReceipt() throws Exception {
        
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        
    	PMode pmode = TestUtils.create1WaySendPushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
        HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        // Setting input message property
    	UserMessage usrMessage = new UserMessage();
    	usrMessage.setPModeId(pmode.getId());
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(usrMessage);
        
        procCtx.setUserMessage(userMessageEntity);
        try {
        	new CheckSentResult().flowComplete(mc);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals(ProcessingState.DELIVERED, userMessageEntity.getCurrentProcessingState().getState());
    }
    
    @Test
    public void testSuccessAwaitReceipt() throws Exception {
    	
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	
    	PMode pmode = TestUtils.create1WaySendPushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
    	leg.setReceiptConfiguration(new ReceiptConfiguration());
    	HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	StorageManager storageManager = HolodeckB2BCore.getStorageManager();
    	// Setting input message property
    	UserMessage usrMessage = new UserMessage();
    	usrMessage.setPModeId(pmode.getId());
    	IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(usrMessage);
    	
    	procCtx.setUserMessage(userMessageEntity);
    	try {
    		new CheckSentResult().flowComplete(mc);
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	assertEquals(ProcessingState.AWAITING_RECEIPT, userMessageEntity.getCurrentProcessingState().getState());
    }

}