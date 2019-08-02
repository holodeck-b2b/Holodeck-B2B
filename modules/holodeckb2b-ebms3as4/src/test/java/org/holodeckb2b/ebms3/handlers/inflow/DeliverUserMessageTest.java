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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.DeliveryConfiguration;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod.NullDeliverer;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.core.handlers.inflow.DeliverUserMessage;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageDelivery;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.After;
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

	@BeforeClass
	public static void setUpClass() throws Exception {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
	}

	@After
	public void tearDown() throws Exception {
		TestUtils.cleanOldMessageUnitEntities();
		HolodeckB2BCoreInterface.getPModeSet().removeAll();
	}

	@Test
	public void testDoProcessing() throws Exception {
		PMode pmode = TestUtils.create1WayReceivePushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
        
		DeliveryConfiguration deliverySpecification = new DeliveryConfiguration();
		deliverySpecification.setFactory(NullDeliveryMethod.class.getName());
		deliverySpecification.setId("delivery_spec_id");
		leg.setDefaultDelivery(deliverySpecification);
			
		HolodeckB2BCore.getPModeSet().add(pmode);

		MessageContext mc = new MessageContext();
		mc.setServerSide(true);
		mc.setFLOW(MessageContext.IN_FLOW);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeIncomingMessageUnit(userMessage);
		storageManager.setProcessingState(umEntity, ProcessingState.READY_FOR_DELIVERY);
		
		MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(umEntity);

		try {
			assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverUserMessage().invoke(mc));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertTrue(((NullDeliverer) HolodeckB2BCore.getMessageDeliverer(deliverySpecification))
																			.wasDelivered(userMessage.getMessageId()));
		assertEquals(ProcessingState.DELIVERED, umEntity.getCurrentProcessingState().getState());
		TestEventProcessor eventProc = (TestEventProcessor) HolodeckB2BCoreInterface.getEventProcessor();
		assertTrue(eventProc.events.size() == 1);
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivery);
		assertTrue(((IMessageDelivery) eventProc.events.get(0)).isDeliverySuccessful());
	}
}