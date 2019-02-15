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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.activation.DataHandler;

import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.pmode.Property;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 16:54 20.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DecompressionHandlerTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private DecompressionHandler handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = DecompressionHandlerTest.class.getClassLoader()
                .getResource("compression").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new DecompressionHandler();
    }


    @Test
    public void testDoProcessing() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage();

        // We need to add payload with containment type "attachment" to user message,
        // because PartInfoElement does not read the "containment" attribute
        // value of the PartInfo tag from file now (23-Feb-2017 T.S.)
        Payload payload = new Payload();
        payload.setContainment(IPayload.Containment.ATTACHMENT);
        ArrayList<IProperty> props = new ArrayList<>();
        Property p = new Property();
        p.setName(CompressionFeature.FEATURE_PROPERTY_NAME);
        p.setValue(CompressionFeature.COMPRESSED_CONTENT_TYPE);
        props.add(p);
        p = new Property();
        p.setName(CompressionFeature.MIME_TYPE_PROPERTY_NAME);
        p.setValue("image/jpeg");
        props.add(p);
        payload.setProperties(props);
        String payloadPath = "file://./uncompressed.jpg";
        payload.setPayloadURI(payloadPath);
        userMessage.addPayload(payload);

        Attachments attachments = new Attachments();
        attachments.addDataHandler(payloadPath,
                new DataHandler(new URL(payload.getPayloadURI())));
        mc.setAttachmentMap(attachments);

        // Setting input message property
        IUserMessageEntity userMessageEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        MessageProcessingContext procCtx = new MessageProcessingContext(mc);
        procCtx.setUserMessage(userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        // Check that attachment payload does not contain the compression enabling properties anymore
        final IPayload deCompPayload = userMessageEntity.getPayloads().iterator().next();
        assertEquals(IPayload.Containment.ATTACHMENT, deCompPayload.getContainment());
        Collection<IProperty> properties = deCompPayload.getProperties();
        assertFalse(Utils.isNullOrEmpty(properties)); 
    	assertFalse(properties.parallelStream().anyMatch(pp -> 
        										pp.getName().equals(CompressionFeature.FEATURE_PROPERTY_NAME)));
    											
        assertTrue(mc.getAttachment(deCompPayload.getPayloadURI()) instanceof CompressionDataHandler);
        assertEquals("image/jpeg", mc.getAttachment(deCompPayload.getPayloadURI()).getContentType());
    }
}