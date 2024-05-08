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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.test.storage.InMemoryPSProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Created at 12:09 15.03.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SaveUserMsgAttachmentsTest {

	private static HolodeckB2BTestCore testCore;

    @BeforeAll
    public static void setUpClass() throws Exception {
        testCore = new HolodeckB2BTestCore();
        HolodeckB2BCoreInterface.setImplementation(testCore);
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        testCore.cleanStorage();
        testCore.cleanTemp();
    }

    @Test
    public void testDefault() throws Exception {

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage();
        Payload payload = new Payload();
        payload.setContainment(Containment.ATTACHMENT);
        payload.setPayloadURI("some-att-cid");
        userMessage.addPayload(payload);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        IUserMessageEntity userMsgEntity = HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(userMessage);
        procCtx.setUserMessage(userMsgEntity);

        // Adding data handler for the payload described in mmd
        Attachments attachments = new Attachments();
        DataHandler dh = new DataHandler(new FileDataSource(TestUtils.getTestResource("dandelion.jpg").toFile()));
        attachments.addDataHandler(payload.getPayloadURI(), dh);

        mc.setAttachmentMap(attachments);

        assertEquals(Handler.InvocationResponse.CONTINUE,
        				assertDoesNotThrow(() -> new SaveUserMsgAttachments().invoke(mc)));

        IPayloadEntity storedPayload = userMsgEntity.getPayloads().iterator().next();
        assertNotNull(storedPayload);
        assertEquals("image/jpeg", storedPayload.getMimeType());

        try (FileInputStream org = new FileInputStream(TestUtils.getTestResource("dandelion.jpg").toFile());
        	 InputStream stored = storedPayload.getContent()) {
	        HB2BTestUtils.assertEqual(org, stored);
		} catch (Exception e) {
			fail(e.getMessage());
		}

    }

    @Test
    public void testAlreadySaved() throws Exception {
    	final File testFile = TestUtils.getTestResource("dandelion.jpg").toFile();

    	MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage();
        Payload payload = new Payload();
        payload.setContainment(Containment.ATTACHMENT);
        payload.setPayloadURI("some-att-cid");
        userMessage.addPayload(payload);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        IUserMessageEntity userMsgEntity = HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(userMessage);
        procCtx.setUserMessage(userMsgEntity);

        // Pre save payload
        IPayloadContent content = testCore.getPayloadStorageProvider().createNewPayloadStorage(
        								userMsgEntity.getPayloads().iterator().next().getPayloadId(), null, null);
        try (FileInputStream fis = new FileInputStream(testFile); OutputStream cos = content.openStorage()) {
			Utils.copyStream(fis, cos);
		}

        // Adding data handler for the payload described in mmd
        Attachments attachments = new Attachments();
        DataHandler dh = new DataHandler(new FileDataSource(testFile));
        attachments.addDataHandler(payload.getPayloadURI(), dh);

        mc.setAttachmentMap(attachments);

        assertEquals(Handler.InvocationResponse.CONTINUE,
        				assertDoesNotThrow(() -> new SaveUserMsgAttachments().invoke(mc)));

        assertNotNull(userMsgEntity.getPayloads().iterator().next().getContent());
		assertEquals(1, ((InMemoryPSProvider) testCore.getPayloadStorageProvider()).getPayloadCount());
    }
}