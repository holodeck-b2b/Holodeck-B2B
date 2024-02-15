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
package org.holodeckb2b.core.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 22:19 09.03.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class StartProcessingUsrMessageTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeReceivedMessageUnit(new UserMessage());
        
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);
        
        try {
            assertEquals(InvocationResponse.CONTINUE, new StartProcessingUsrMessage().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.PROCESSING, userMessageEntity.getCurrentProcessingState().getState());        
    }
    
    @Test
    public void testSkipAlreadyProcessed() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeReceivedMessageUnit(new UserMessage());
        
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);
        
        updateManager.setProcessingState(HolodeckB2BCore.getQueryManager()
        		.getMessageUnitWithCoreId(userMessageEntity.getCoreId()), 
        		ProcessingState.OUT_FOR_DELIVERY);
        try {
            assertEquals(InvocationResponse.CONTINUE, new StartProcessingUsrMessage().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNull(procCtx.getReceivedUserMessage());
        assertEquals(ProcessingState.OUT_FOR_DELIVERY, userMessageEntity.getCurrentProcessingState().getState());        
    }    
}