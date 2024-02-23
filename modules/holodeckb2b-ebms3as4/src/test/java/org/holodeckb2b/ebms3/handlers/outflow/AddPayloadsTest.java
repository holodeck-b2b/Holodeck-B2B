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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileInputStream;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Created at 23:39 29.01.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class AddPayloadsTest {

	@BeforeAll
	public static void setUpClass() throws Exception {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
	}

	@Test
	public void testAttachmentPayload() throws Exception {
		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.OUT_FLOW);
		// Envelope is needed to add body payload
		assertDoesNotThrow(() -> mc.setEnvelope(SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12)));

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId("payload-adder-01");
		// Programmatically added attachment payload
		Payload payload = new Payload();
		payload.setContainment(IPayload.Containment.ATTACHMENT);
		payload.setPayloadURI("some_URI_01");
		payload.setContentStream(new FileInputStream(TestUtils.getTestResource("flower.jpg").toFile()));
		userMessage.addPayload(payload);

		// Setting input message property
		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));

		assertDoesNotThrow(() -> new AddPayloads().invoke(mc));

		assertNotNull(mc.getAttachmentMap());
		assertEquals(1, mc.getAttachmentMap().getAllContentIDs().length);
		assertEquals(payload.getPayloadURI(), mc.getAttachmentMap().getAllContentIDs()[0]);
	}

	@Test
	public void testBodyPayload() throws Exception {
		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.OUT_FLOW);
		// Envelope is needed to add body payload
		assertDoesNotThrow(() -> mc.setEnvelope(SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12)));

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId("payload-adder-01");
		// Programmatically added body payload
		Payload payload = new Payload();
		payload.setContainment(IPayload.Containment.BODY);
		payload.setPayloadURI("some_URI_03");
		payload.setContentStream(new FileInputStream(TestUtils.getTestResource("document.xml").toFile()));
		userMessage.addPayload(payload);

		// Setting input message property
		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));

		assertDoesNotThrow(() -> new AddPayloads().invoke(mc));

		assertNotNull(mc.getEnvelope().getBody().getFirstElement());
		assertEquals("document", mc.getEnvelope().getBody().getFirstElement().getLocalName());
	}
}
