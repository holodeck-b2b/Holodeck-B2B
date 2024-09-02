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
package org.holodeckb2b.as4.compression;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PayloadProfile;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Created at 13:57 21.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CompressionHandlerTest {

	private static HolodeckB2BTestCore testCore;

    @BeforeAll
    public static void setUpClass() throws Exception {
    	testCore = new HolodeckB2BTestCore();
        HolodeckB2BCoreInterface.setImplementation(testCore);
    }

    @Test
    public void testDoProcessing() throws Exception {

        PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
        Leg leg = pmode.getLeg(Label.REQUEST);
        UserMessageFlow umFlow = new UserMessageFlow();
        PayloadProfile plProfile = new PayloadProfile();
        plProfile.setCompressionType(CompressionFeature.COMPRESSED_CONTENT_TYPE);
        umFlow.setPayloadProfile(plProfile);
        leg.setUserMessageFlow(umFlow);
        HolodeckB2BCoreInterface.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());
        userMessage.setProcessingState(ProcessingState.PROCESSING);
        Payload payload = new Payload();
        payload.setContainment(IPayload.Containment.ATTACHMENT);
        payload.setPayloadURI(UUID.randomUUID().toString());
        userMessage.addPayload(payload);
        IUserMessageEntity userMessageEntity = testCore.getMetadataStorageProvider().storeMessageUnit(userMessage);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        Attachments attachments = new Attachments();
        DataHandler attDataHandler = new DataHandler(new FileDataSource(
        											TestUtils.getTestResource("compression/uncompressed.jpg").toFile()));
        attachments.addDataHandler(payload.getPayloadURI(), attDataHandler);
        mc.setAttachmentMap(attachments);

        MessageProcessingContext.getFromMessageContext(mc).setUserMessage(userMessageEntity);

        assertEquals(Handler.InvocationResponse.CONTINUE, assertDoesNotThrow(() -> new CompressionHandler().invoke(mc)));

        // Check that attachment payload contains compression enabling properties
        for (final IPayload p : userMessageEntity.getPayloads()) {
            // Only payloads contained in attachment can use compression
            if (p.getContainment() == IPayload.Containment.ATTACHMENT) {
                Collection<IProperty> properties = p.getProperties();
                assertNotNull(properties);
                Iterator<IProperty> it = properties.iterator();
                IProperty property = it.next();
                assertEquals(CompressionFeature.FEATURE_PROPERTY_NAME, property.getName());
                assertEquals(CompressionFeature.COMPRESSED_CONTENT_TYPE, property.getValue());
                property = it.next();
                assertEquals(CompressionFeature.MIME_TYPE_PROPERTY_NAME, property.getName());
                assertEquals(attDataHandler.getContentType(), property.getValue());
                assertTrue(mc.getAttachment(p.getPayloadURI()) instanceof CompressionDataHandler);
                assertEquals(CompressionFeature.COMPRESSED_CONTENT_TYPE,
                				mc.getAttachment(p.getPayloadURI()).getContentType());

            }
        }

    }
}