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
import static org.junit.Assert.fail;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handlers.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 23:39 29.01.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class AddPayloadsTest {

	private static String baseDir;

	@BeforeClass
	public static void setUpClass() throws Exception {
		baseDir = AddPayloadsTest.class.getClassLoader()
				.getResource(AddPayloadsTest.class.getSimpleName()).getPath();
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));
	}

	@Test
	public void testDoProcessing() throws Exception {

		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.OUT_FLOW);
		// Envelope is needed to add body payload
		try {
			mc.setEnvelope(SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12));
		} catch (AxisFault axisFault) {
			fail(axisFault.getMessage());
		}

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId("payload-adder-01");
		// Programmatically added attachment payload
		Payload payload = new Payload();
		payload.setContainment(IPayload.Containment.ATTACHMENT);
		String payloadURI = "some_URI_01";
		payload.setPayloadURI(payloadURI);
		payload.setContentLocation(baseDir + "/flower.jpg");
		userMessage.addPayload(payload);

		// Programmatically added body payload
		payload = new Payload();
		payload.setContainment(IPayload.Containment.BODY);
		payload.setPayloadURI("some_URI_03");
		payload.setContentLocation(baseDir + "/document.xml");
		userMessage.addPayload(payload);

		// Setting input message property
		IUserMessageEntity userMessageEntity = HolodeckB2BCore.getStorageManager()
																.storeIncomingMessageUnit(userMessage);
		
		MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(userMessageEntity);

		try {
			new AddPayloads().invoke(mc);			
		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertNotNull(mc.getAttachmentMap());
		assertEquals(1, mc.getAttachmentMap().getAllContentIDs().length);
		
		assertNotNull(mc.getEnvelope().getBody().getFirstElement());
		assertEquals("document", mc.getEnvelope().getBody().getFirstElement().getLocalName());
	}
}
