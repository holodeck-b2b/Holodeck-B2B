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
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.pmode.Leg;
import org.holodeckb2b.common.testhelpers.pmode.PMode;
import org.holodeckb2b.common.testhelpers.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:07 15.03.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ProcessReceiptsTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }
    
    @After
    public void tearDown() throws Exception {    	
        HolodeckB2BCore.getPModeSet().removeAll();
    }

    /**
     * Test the case when the message unit is present and is referenced in the receipt
     */
    @Test
    public void testDoProcessing() throws Exception {
    
    	PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setId("pmode-test-id");

        Leg leg = new Leg();
        ReceiptConfiguration receiptConfiguration = new ReceiptConfiguration();
        leg.setReceiptConfiguration(receiptConfiguration);
        pmode.addLeg(leg);

        HolodeckB2BCore.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input message property
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        userMessage.setPModeId(pmode.getId());
        
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(userMessage);
        storageManager.setProcessingState(userMessageEntity, ProcessingState.AWAITING_RECEIPT);
        
        // Setting input receipts property
        Receipt receipt = new Receipt();
        receipt.setRefToMessageId(userMessage.getMessageId());
        receipt.setMessageId(MessageIdUtils.createMessageId());
        
        IReceiptEntity receiptEntity = storageManager.storeIncomingMessageUnit(receipt);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedReceipt(receiptEntity);
        
        try {           
            assertEquals(Handler.InvocationResponse.CONTINUE, new ProcessReceipts().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertEquals(ProcessingState.DELIVERED, HolodeckB2BCoreInterface.getQueryManager()
        												.getMessageUnitsWithId(userMessage.getMessageId())
        												.iterator().next().getCurrentProcessingState().getState());        												
        assertEquals(ProcessingState.READY_FOR_DELIVERY, receiptEntity.getCurrentProcessingState().getState());        
    }

    /**
     * Test the case when there is no reference to message unit in the receipt
     */
    @Test
    public void testDoProcessingIfNoRefToMsgId() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input receipts property
        Receipt receipt = new Receipt();
        receipt.setMessageId(MessageIdUtils.createMessageId());
        receipt.setRefToMessageId(MessageIdUtils.createMessageId());
        IReceiptEntity receiptEntity = storageManager.storeIncomingMessageUnit(receipt);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedReceipt(receiptEntity);
        
        try {           
            assertEquals(Handler.InvocationResponse.CONTINUE, new ProcessReceipts().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.FAILURE, receiptEntity.getCurrentProcessingState().getState());
        assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors().get(receiptEntity.getMessageId())));
        assertEquals("EBMS:0003", 
        			 procCtx.getGeneratedErrors().get(receiptEntity.getMessageId()).iterator().next().getErrorCode());
    }
}