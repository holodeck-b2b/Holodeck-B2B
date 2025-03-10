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
package org.holodeckb2b.security.callbackhandlers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.activation.DataHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.context.MessageContext;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.ebms3.security.callbackhandlers.AttachmentCallbackHandler;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 17:37 04.05.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class AttachmentCallbackHandlerTest {

    private MessageContext mc;
    private AttachmentCallbackHandler handler;

    @Before
    public void setUp() throws Exception {
        mc = new MessageContext();
        setAttachments();
        handler = new AttachmentCallbackHandler(mc);
    }

    @After
    public void tearDown() throws Exception {
        mc = null;
    }

    @Test
    public void testHandleSingleAttachmentRequestCallback() throws Exception {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new AttachmentRequestCallback();
        ((AttachmentRequestCallback)callbacks[0]).setAttachmentId("some_URI_01");

        assertNull(((AttachmentRequestCallback) callbacks[0]).getAttachments());

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        List<Attachment> attachmentList =
                ((AttachmentRequestCallback)callbacks[0]).getAttachments();

        assertNotNull(attachmentList);
        assertTrue(attachmentList.size() == 1);
    }

    @Test
    public void testHandleAllAttachmentRequestCallback() throws Exception {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new AttachmentRequestCallback();
        ((AttachmentRequestCallback)callbacks[0]).setAttachmentId("Attachments");

        assertNull(((AttachmentRequestCallback) callbacks[0]).getAttachments());

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        List<Attachment> attachmentList =
                ((AttachmentRequestCallback)callbacks[0]).getAttachments();

        assertNotNull(attachmentList);
        assertTrue(attachmentList.size() == 2);
    }

    @Test
    public void testHandleAttachmentResultCallback() throws Exception {
        Callback[] callbacks = new Callback[1];

        AttachmentResultCallback callback = new AttachmentResultCallback();
        callback.setAttachmentId("some_URI_01");
        callback.setAttachment(mock(Attachment.class));

        callbacks[0] = callback;

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getAttachmentMap().getDataHandler("some_URI_01"));
    }

    @Test
    public void testHandleUnsupportedRequestCallback() throws Exception {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new Callback() {};

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedCallbackException);
        }
    }

    /**
     *
     * @throws Exception
     */
    private void setAttachments() throws Exception {
        // Add attachments to message context
        Attachments attachments = new Attachments();

        // Programmatically added attachment payload
        Payload payload = new Payload();
        payload.setContainment(IPayload.Containment.ATTACHMENT);
        String payloadURI = "some_URI_01";
        payload.setPayloadURI(payloadURI);

        // Adding data handler for the programmatically added attachment payload
        DataHandler dh = new DataHandler(TestUtils.getTestResource("flower.jpg").toUri().toURL());
        attachments.addDataHandler(payloadURI, dh);

        // Programmatically added body payload
        payload = new Payload();
        payload.setContainment(IPayload.Containment.ATTACHMENT);
        payloadURI = "some_URI_02";
        payload.setPayloadURI(payloadURI);        

        // Adding data handler for the programmatically added body payload
        dh = new DataHandler(TestUtils.getTestResource("dandelion.jpg").toUri().toURL()); 
        attachments.addDataHandler(payloadURI, dh);

        mc.setAttachmentMap(attachments);
    }
}