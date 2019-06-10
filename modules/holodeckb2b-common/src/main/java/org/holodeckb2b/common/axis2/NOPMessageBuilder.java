/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.axis2;

import java.io.InputStream;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.TransportUtils;

/**
 * Is a {@link Builder} implementation that does not process the received message content but just adds a reference to
 * the request input stream to the Axis2 <i>Message Context</i>. This means that either handlers or the final message
 * receiver is responsible for processing of the message content.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class NOPMessageBuilder implements Builder {

	/**
	 * Name of the Axis2 Message Context property in which the request input stream is stored 
	 */
	public static final String REQUEST_INPUTSTREAM = "hb2b-rest:is";
			
	@Override
	public OMElement processDocument(InputStream inputStream, String contentType, MessageContext messageContext)
			throws AxisFault {		
		
		messageContext.setDoingREST(true);
		messageContext.setProperty(REQUEST_INPUTSTREAM, inputStream);
				
		return TransportUtils.createSOAPEnvelope(null);

	}
}
