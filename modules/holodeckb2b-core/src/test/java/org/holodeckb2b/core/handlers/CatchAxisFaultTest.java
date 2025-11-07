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
package org.holodeckb2b.core.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 18:06 19.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CatchAxisFaultTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    	HolodeckB2BTestCore core = new HolodeckB2BTestCore(TestUtils.getTestClassBasePath());
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Test
    public void testAllGood() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);

        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        procCtx.setUserMessage(updateManager.storeReceivedMessageUnit(userMessage));

        Receipt receipt = new Receipt();
        receipt.setMessageId(MessageIdUtils.createMessageId());
        procCtx.addSendingReceipt(updateManager.storeOutGoingMessageUnit(receipt));

    	try {
            new CatchAxisFault().flowComplete(procCtx.getParentContext());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertFalse(Utils.isNullOrEmpty(procCtx.getReceivedMessageUnits()));
        assertFalse(Utils.isNullOrEmpty(procCtx.getSendingMessageUnits()));
        assertTrue(procCtx.getReceivedMessageUnits().stream()
        					   .noneMatch(mu -> mu.getCurrentProcessingState().getState() == ProcessingState.INTERRUPTED));
        assertTrue(procCtx.getSendingMessageUnits().stream()
        						.noneMatch(mu -> mu.getCurrentProcessingState().getState() == ProcessingState.FAILURE));
    }

    @Test
    public void testFaultOnOutReceipt() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);

        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        IUserMessageEntity umEntity = updateManager.storeReceivedMessageUnit(userMessage);
        updateManager.setProcessingState(umEntity, ProcessingState.DELIVERED);
        procCtx.setUserMessage(umEntity);

        Receipt receipt = new Receipt();
        receipt.setMessageId(MessageIdUtils.createMessageId());
        IReceiptEntity rcptEntity = updateManager.storeOutGoingMessageUnit(receipt);
        procCtx.addSendingReceipt(rcptEntity);

        mc.setFLOW(MessageContext.OUT_FLOW);
        Exception exception = new Exception("Some exception.");
    	procCtx.getParentContext().setFailureReason(exception);

    	try {
            new CatchAxisFault().flowComplete(procCtx.getParentContext());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue(Utils.isNullOrEmpty(procCtx.getReceivedMessageUnits()));
        assertFalse(procCtx.getSendingErrors().isEmpty());
        assertTrue(Utils.isNullOrEmpty(procCtx.getSendingReceipts()));
        assertNull(procCtx.getSendingPullRequest());
        assertNull(procCtx.getSendingUserMessage());
        assertEquals(ProcessingState.DELIVERED, umEntity.getCurrentProcessingState().getState());
        assertEquals(ProcessingState.INTERRUPTED, rcptEntity.getCurrentProcessingState().getState());
    }

    @Test
    public void testFaultOnInUserMsg() throws Exception {
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);
    	mc.setServerSide(true);

    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

    	StorageManager updateManager = HolodeckB2BCore.getStorageManager();
    	UserMessage userMessage = new UserMessage();
    	userMessage.setMessageId(MessageIdUtils.createMessageId());
    	IUserMessageEntity umEntity = updateManager.storeReceivedMessageUnit(userMessage);
    	updateManager.setProcessingState(umEntity, ProcessingState.OUT_FOR_DELIVERY);
    	procCtx.setUserMessage(umEntity);

    	Exception exception = new Exception("Some exception.");
    	procCtx.getParentContext().setFailureReason(exception);

    	try {
    		new CatchAxisFault().flowComplete(procCtx.getParentContext());
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}

    	assertTrue(Utils.isNullOrEmpty(procCtx.getReceivedMessageUnits()));
    	assertFalse(procCtx.getSendingErrors().isEmpty());
    	assertTrue(Utils.isNullOrEmpty(procCtx.getSendingReceipts()));
    	assertNull(procCtx.getSendingPullRequest());
    	assertNull(procCtx.getSendingUserMessage());
    	assertEquals(ProcessingState.INTERRUPTED, umEntity.getCurrentProcessingState().getState());
    }

    @Test
    public void testFaultOnOutUserMsg() throws Exception {
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	mc.setServerSide(false);

    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

    	StorageManager updateManager = HolodeckB2BCore.getStorageManager();
    	UserMessage userMessage = new UserMessage();
    	userMessage.setMessageId(MessageIdUtils.createMessageId());
    	IUserMessageEntity umEntity = updateManager.storeOutGoingMessageUnit(userMessage);
    	updateManager.setProcessingState(umEntity, ProcessingState.SENDING);
    	procCtx.setUserMessage(umEntity);

    	Exception exception = new Exception("Some exception.");
    	procCtx.getParentContext().setFailureReason(exception);

    	try {
    		new CatchAxisFault().flowComplete(procCtx.getParentContext());
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}

    	assertTrue(Utils.isNullOrEmpty(procCtx.getReceivedMessageUnits()));
    	assertTrue(Utils.isNullOrEmpty(procCtx.getSendingMessageUnits()));
    	assertEquals(ProcessingState.SUSPENDED, umEntity.getCurrentProcessingState().getState());
    }
}