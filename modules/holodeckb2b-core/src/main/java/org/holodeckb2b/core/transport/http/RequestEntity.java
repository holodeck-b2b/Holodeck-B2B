/*
 * Copyright (C) 2026 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.axiom.blob.Blobs;
import org.apache.axiom.blob.MemoryBlob;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.MessageFormatter;
import org.apache.http.entity.AbstractHttpEntity;

/**
 * Represents the entity body of an HTTP request and handles the actual creation of the content when the request is
 * sent. When the chunked transfer encoding is not used the content is first created in a memory blob to be able to
 * calculate the content-length. Although the {@link HTTPRequestSender} will always use the chunked transfer encoding
 * when content compression is used this class also supports non chunked compressed entities and will create the
 * compressed content first in memory.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 9.0.0
 */
class RequestEntity extends AbstractHttpEntity {

	private OMOutputFormat format;
    private MessageFormatter messageFormatter;

    private final boolean gzip;

    private MessageContext messageContext;

    private final MemoryBlob content;
    private final boolean preserve;

    /**
     * Creates a new request entity body instance which using the given configuration.
     *
     * @param msgContext
     * @param messageFormatter	the Axis2 message formatter to create the actual content
     * @param format			Axiom format option how to create the content
     * @param chunked			indicates whether chunked encoding is used
     * @param gzip				indicates whether gzip encoding is used
     * @param preserve			if the formatter should preserve the content so it can be repeated
     * @throws AxisFault when the content cannot be created
     */
	RequestEntity(MessageContext msgContext, MessageFormatter messageFormatter, OMOutputFormat format,
						 boolean chunked, boolean gzip, boolean preserve) throws AxisFault {
		this.messageContext = msgContext;
		this.format = format;
		this.messageFormatter = messageFormatter;
		this.chunked = chunked;
		this.gzip = gzip;
		this.preserve = preserve;
		if (!chunked) {
			content = Blobs.createMemoryBlob();
		    try (OutputStream out = content.getOutputStream()) {
		        writeContent(out);
		    } catch (IOException ex) {
		        throw AxisFault.makeFault(ex);
		    }
		} else
			content = null;
		setContentType(messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
	}

	@Override
	public boolean isRepeatable() {
		return preserve || !chunked;
	}

	@Override
	public long getContentLength() {
		if (chunked)
			return -1;
		else
			return content.getSize();
	}

	@Override
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeTo(OutputStream outStream) throws IOException {
		if (content != null)
			content.writeTo(outStream);
		else
			writeContent(outStream);
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	/**
	 * @return <code>true</code> if the entity body is compressed, <code>false</code> otherwise
	 */
	public boolean isCompressed() {
		return gzip;
	}

	/**
	 * Writes the actual content of the entity body to the specified output stream.
	 *
	 * @param out	The output stream to which the content is written
	 * @throws IOException	If an I/O error occurs
	 */
	private void writeContent(OutputStream out) throws IOException {
		if (gzip)
            out = new GZIPOutputStream(out);

        try {
            messageFormatter.writeTo(messageContext, format, out, preserve);
            if (gzip)
                ((GZIPOutputStream) out).finish();
            out.flush();
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }
	}
}
