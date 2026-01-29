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
package org.holodeckb2b.core.axis2;

import java.io.InputStream;
import java.text.ParseException;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.mime.ContentType;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.MIMEAwareBuilder;
import org.apache.axis2.builder.MIMEBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.util.MessageProcessorSelector;

/**
 * Is a replacement for the default {@link MIMEBuilder} which will throw an {@link AxisFault} when no Builder can be
 * found for the request as this indicates that the received request is invalid and therefore should be answered to with
 * a HTTP 400 (Bad Request).
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.1.1
 */
public class MimeMultipartBuilder implements Builder {

	@Override
	public OMElement processDocument(InputStream inputStream, String contentType, MessageContext msgContext)
			throws AxisFault {
		Attachments attachments = BuilderUtil.createAttachmentsMap(msgContext, inputStream, contentType);

		ContentType ct;
		try {
			ct = new ContentType(contentType);
		} catch (ParseException e) {
			throw new AxisFault("Invalid Content Type Field in the Mime Message",
								SOAP12Constants.QNAME_SENDER_FAULTCODE);
		}

		String type = ct.getParameter("type");
		Builder builder = MessageProcessorSelector.getMessageBuilder(type, msgContext);

		if (builder != null && builder instanceof MIMEAwareBuilder)
			return ((MIMEAwareBuilder) builder).processMIMEMessage(attachments, type, msgContext);
		else
			throw new AxisFault("Unknown/invalid MIME type of root body part [" + type + "]",
								SOAP12Constants.QNAME_SENDER_FAULTCODE);
	}
}
