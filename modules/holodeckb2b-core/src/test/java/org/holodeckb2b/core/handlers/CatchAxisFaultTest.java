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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.inmemory.InMemoryProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 18:06 19.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CatchAxisFaultTest {

    private static HolodeckB2BTestCore core;

    private CatchAxisFault handler;

    private MessageProcessingContext procCtx;
    private UserMessage				 userMessage;
    private Receipt					 receipt;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        core = new HolodeckB2BTestCore(CatchAxisFaultTest.class.getClassLoader().getResource(".").getPath());
        core.setPersistencyProvider(new InMemoryProvider());        
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new CatchAxisFault();

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);

        procCtx = MessageProcessingContext.getFromMessageContext(mc);
        
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        procCtx.setUserMessage(updateManager.storeIncomingMessageUnit(userMessage));

        receipt = new Receipt();
        receipt.setMessageId(MessageIdUtils.createMessageId());
        procCtx.addReceivedReceipt(updateManager.storeIncomingMessageUnit(receipt));
        
    }
    
    @Test
    public void testAllGood() {
        try {
            handler.flowComplete(procCtx.getParentContext());            
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertFalse(Utils.isNullOrEmpty(procCtx.getReceivedMessageUnits()));
        assertEquals(2, procCtx.getReceivedMessageUnits().size());
        
        final IMessageUnitEntity usrMsgInCtx = procCtx.getReceivedMessageUnit(userMessage.getMessageId());
        assertNotNull(usrMsgInCtx);
        assertTrue(usrMsgInCtx instanceof IUserMessageEntity);
        assertNotEquals(ProcessingState.FAILURE, usrMsgInCtx.getCurrentProcessingState().getState());
        assertNotNull(procCtx.getReceivedReceipts());
        assertNotEquals(ProcessingState.FAILURE, procCtx.getReceivedReceipts().iterator().next()
        																	  .getCurrentProcessingState().getState());
    }
    
    @Test
    public void testAxisFaultThrown() {

        Exception exception = new Exception("Some exception.");        
        procCtx.getParentContext().setFailureReason(exception);
        try {
            handler.flowComplete(procCtx.getParentContext());            
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue(Utils.isNullOrEmpty(procCtx.getReceivedMessageUnits()));
        
        try {
			Collection<IMessageUnitEntity> storedMsg = HolodeckB2BCore.getQueryManager()
																	.getMessageUnitsWithId(userMessage.getMessageId());
			assertFalse(Utils.isNullOrEmpty(storedMsg));
			assertEquals(ProcessingState.FAILURE, storedMsg.iterator().next().getCurrentProcessingState().getState());
			
			storedMsg = HolodeckB2BCore.getQueryManager().getMessageUnitsWithId(receipt.getMessageId());
			assertFalse(Utils.isNullOrEmpty(storedMsg));
			assertEquals(ProcessingState.FAILURE, storedMsg.iterator().next().getCurrentProcessingState().getState());			
		} catch (PersistenceException e) {
			fail("Message not found");
		}        
        
    }
}