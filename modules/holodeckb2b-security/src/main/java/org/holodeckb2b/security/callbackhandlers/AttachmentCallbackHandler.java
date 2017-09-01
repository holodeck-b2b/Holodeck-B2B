/**
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.context.MessageContext;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;

/**
 * Is the callback handler that provides the WSS4J library access to the attachments of the message. It converts between
 * the WSS4J and Axis2 format.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class AttachmentCallbackHandler implements CallbackHandler {

    private final MessageContext msgContext;

    public AttachmentCallbackHandler(final MessageContext msgContext) {
        this.msgContext = msgContext;
    }

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (final Callback callback : callbacks)
        {
            if (callback instanceof AttachmentRequestCallback) {
                final AttachmentRequestCallback reqCallback = (AttachmentRequestCallback) callback;
                // Is one specific attachment requested or all?
                String attachmentId = reqCallback.getAttachmentId();
                if ("Attachments".equals(attachmentId))
                    attachmentId = null; // all attachments

                reqCallback.setAttachments(getAttachments(attachmentId));
            } else if (callback instanceof AttachmentResultCallback) {
                final AttachmentResultCallback resultCallback = (AttachmentResultCallback) callback;
                final Attachment resultAttachment = resultCallback.getAttachment();

                // Wrap the stream from the WSS4J Attachment in a DataHandler that can be used by Axis2
                final DataHandler dh = new DataHandler(new AttachmentDataSource(resultAttachment.getMimeType(),
                                                                          resultAttachment.getSourceStream()));

                msgContext.getAttachmentMap().addDataHandler(resultCallback.getAttachmentId(), dh);
            } else {
                throw new UnsupportedCallbackException(callback, "Unsupported callback");
            }
        }
    }

    /**
     * Gets all requested attachments as a list of {@link Attachment} objects so they can be processed by WSS4j.
     *
     * @param attachmentId  The Content-Id of the requested attachment, or<br>
     *                      <code>null</code> if all attachments are requested
     * @return The list of attachments
     * @throws IOException  When one of the requested attachment can not be accessed
     */
    private List<Attachment> getAttachments(final String attachmentId) throws IOException {
        final List<org.apache.wss4j.common.ext.Attachment> attachmentList
                                                            = new ArrayList<>();
        final Attachments attachments = msgContext.getAttachmentMap();
        if (attachments != null) {
            for (final Object o : attachments.getContentIDSet()) {
                final String cid = (String) o;
                // Check if this is the correct attachment, if none content-id is specified it is always okay
                if (attachmentId != null && !attachmentId.equals(cid))
                    continue;

                // Create the WSS4J Attachhment object
                final Attachment att = new Attachment();
                final DataHandler attachment = attachments.getDataHandler(cid);
                att.setMimeType(attachment.getContentType());
                att.setId(cid);
                att.setSourceStream(attachment.getInputStream());

                attachmentList.add(att);
            }
        }

        return attachmentList;
    }

    /**
     * Simple {@link DataSource} implementation to wrap the source stream from an WSS4J into DataHandler that can be
     * used by Axis2.
     */
    class AttachmentDataSource implements DataSource {

        private final String mimeType;
        private final InputStream ins;

        AttachmentDataSource(final String ctParam, final InputStream sourceStream) throws IOException {
            this.mimeType = ctParam;
            ins = sourceStream;
        }

        @Override
        public String getContentType() {
            return mimeType;
        }

        @Override
        public InputStream getInputStream() {
            return ins;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
