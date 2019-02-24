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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import javax.activation.DataHandler;

import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 12:09 15.03.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class SaveUserMsgAttachmentsTest {

    private static String baseDir;
	private static HolodeckB2BTestCore holodeckB2BTestCore;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = SaveUserMsgAttachmentsTest.class.getClassLoader()
        						.getResource(SaveUserMsgAttachmentsTest.class.getSimpleName()).getPath();        
        holodeckB2BTestCore = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(holodeckB2BTestCore);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();    
        holodeckB2BTestCore.cleanTemp();
    }

    @Test
    public void testDoProcessing() throws Exception {

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage();        
        Payload payload = new Payload();
        payload.setContainment(Containment.ATTACHMENT);
        payload.setMimeType("image/jpeg");
        payload.setPayloadURI("some-att-cid");
        userMessage.addPayload(payload);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        IUserMessageEntity userMsgEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        procCtx.setUserMessage(userMsgEntity);
        
        // Adding data handler for the payload described in mmd
        Attachments attachments = new Attachments();
        DataHandler dh = new DataHandler(new URL("file://" + baseDir + "/dandelion.jpg"));
        attachments.addDataHandler(payload.getPayloadURI(), dh);

        mc.setAttachmentMap(attachments);

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new SaveUserMsgAttachments().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        String storedPayload = userMsgEntity.getPayloads().iterator().next().getContentLocation();
        assertFalse(Utils.isNullOrEmpty(storedPayload));        
        assertTrue(new File(storedPayload).exists());        
    }
}