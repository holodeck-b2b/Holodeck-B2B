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

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestDeliveryManager;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:05 15.03.17
 *
 * Checked for cases coverage (24.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DeliverUserMessageTest {

	static TestDeliveryManager delman;
	
	@BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
        delman = (TestDeliveryManager) HolodeckB2BCore.getDeliveryManager();
    }

	@Before
	public void resetRejection() {
		delman.rejection = null;
	}

	@Test
	public void testDoProcessing() throws Exception {
		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.IN_FLOW);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeIncomingMessageUnit(userMessage);

		storageManager.setProcessingState(umEntity, ProcessingState.READY_FOR_DELIVERY);
		
		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(umEntity);

		try {
			assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverUserMessage().invoke(mc));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertTrue(delman.isDelivered(userMessage.getMessageId()));		
	}
	
    @Test
    public void testIgnoreNonReady() throws Exception {
		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.IN_FLOW);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeIncomingMessageUnit(userMessage);

		storageManager.setProcessingState(umEntity, ProcessingState.PROCESSING);
		
		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(umEntity);

		try {
			assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverUserMessage().invoke(mc));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertFalse(delman.isDelivered(userMessage.getMessageId()));		
    }

    @Test
    public void testPermanentFailure() throws Exception {
		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.IN_FLOW);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeIncomingMessageUnit(userMessage);

		storageManager.setProcessingState(umEntity, ProcessingState.READY_FOR_DELIVERY);
		
		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(umEntity);
		
		try {
			delman.rejection = new MessageDeliveryException("", true);
			assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverUserMessage().invoke(mc));
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertFalse(delman.isDelivered(userMessage.getMessageId()));		
		
		Collection<IEbmsError> errors = procCtx.getGeneratedErrors().get(userMessage.getMessageId());
		assertNotNull(errors);
		assertEquals(1, errors.size());
		IEbmsError error = errors.iterator().next();
		assertEquals(OtherContentError.ERROR_CODE, error.getErrorCode());
    }
    
}