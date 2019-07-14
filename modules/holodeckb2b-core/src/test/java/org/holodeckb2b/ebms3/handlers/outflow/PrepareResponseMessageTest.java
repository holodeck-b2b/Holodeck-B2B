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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handlers.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:44 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PrepareResponseMessageTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }


    @Test
    public void testSingleError() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.OUT_FLOW);

        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setMessageId(MessageIdUtils.createMessageId());

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IErrorMessageEntity errorMessageEntity = updateManager.storeOutGoingMessageUnit(errorMessage);

        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addSendingError(errorMessageEntity);
        
        try {
            assertNotNull(new PrepareResponseMessage().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertTrue(procCtx.getSendingErrors().size() == 1);
        assertEquals(errorMessage.getMessageId(), procCtx.getSendingErrors().iterator().next().getMessageId());
    }
    
    @Test
    public void testMultiErrorNoRef() throws Exception {
    	StorageManager updateManager = HolodeckB2BCore.getStorageManager();
    	
    	MessageContext mc = new MessageContext();
    	mc.setServerSide(true);
    	mc.setFLOW(MessageContext.IN_FLOW);
    	
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	
    	UserMessage usrMessage = new UserMessage();
    	usrMessage.setMessageId(MessageIdUtils.createMessageId());
    	procCtx.setUserMessage(updateManager.storeIncomingMessageUnit(usrMessage));
    	
    	ErrorMessage errorWRef = new ErrorMessage();
    	errorWRef.setMessageId(MessageIdUtils.createMessageId());
    	errorWRef.setRefToMessageId(usrMessage.getMessageId());
    	IErrorMessageEntity entityErrorWRef = updateManager.storeOutGoingMessageUnit(errorWRef);
    	procCtx.addSendingError(entityErrorWRef);
    	ErrorMessage errorNoRef = new ErrorMessage();
    	errorNoRef.setMessageId(MessageIdUtils.createMessageId());    	
    	procCtx.addSendingError(updateManager.storeOutGoingMessageUnit(errorNoRef));
    	
    	try {
    		assertNotNull(new PrepareResponseMessage().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertTrue(procCtx.getSendingErrors().size() == 1);
    	assertEquals(errorNoRef.getMessageId(), procCtx.getSendingErrors().iterator().next().getMessageId());
    	assertEquals(ProcessingState.FAILURE, entityErrorWRef.getCurrentProcessingState().getState());    	
    }
    
    @Test
    public void testMultiErrorUserMessage() throws Exception {
    	StorageManager updateManager = HolodeckB2BCore.getStorageManager();

    	MessageContext mc = new MessageContext();
    	mc.setServerSide(true);
    	mc.setFLOW(MessageContext.IN_FLOW);
    	
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	
    	UserMessage usrMessage = new UserMessage();
    	usrMessage.setMessageId(MessageIdUtils.createMessageId());
    	procCtx.setUserMessage(updateManager.storeIncomingMessageUnit(usrMessage));
    	Receipt rcpt = new Receipt();
    	rcpt.setMessageId(MessageIdUtils.createMessageId());
    	procCtx.addReceivedReceipt(updateManager.storeIncomingMessageUnit(rcpt));
    	
    	ErrorMessage errorRcpt = new ErrorMessage();
    	errorRcpt.setMessageId(MessageIdUtils.createMessageId());
    	errorRcpt.setRefToMessageId(rcpt.getMessageId());
    	IErrorMessageEntity entityErrorRcpt = updateManager.storeOutGoingMessageUnit(errorRcpt);
    	procCtx.addSendingError(entityErrorRcpt);
    	ErrorMessage errorUsrMsg = new ErrorMessage();
    	errorUsrMsg.setMessageId(MessageIdUtils.createMessageId());
    	errorUsrMsg.setRefToMessageId(usrMessage.getMessageId());
    	procCtx.addSendingError(updateManager.storeOutGoingMessageUnit(errorUsrMsg));
    	
    	try {
    		assertNotNull(new PrepareResponseMessage().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertTrue(procCtx.getSendingErrors().size() == 1);
    	assertEquals(errorUsrMsg.getMessageId(), procCtx.getSendingErrors().iterator().next().getMessageId());
    	assertEquals(ProcessingState.FAILURE, entityErrorRcpt.getCurrentProcessingState().getState());    	
    }
}