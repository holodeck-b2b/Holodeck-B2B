/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.handlers;

import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;

/**
 * Is the handler responsible for checking if SOAPHeaderBlock(s) are present and setting them as processed. This
 * will ensure that Axis2 will not reject the message because of unprocessed headers.  
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class ReportHeaderProcessed extends AbstractHandler {

    @Override
    public InvocationResponse invoke(final MessageContext msgCtx) throws AxisFault {

        final SOAPEnvelope env = msgCtx.getEnvelope();
        if (env != null) {
            @SuppressWarnings("unchecked")
			final Iterator<OMElement> headers = env.getHeader().getChildElements();
            while (headers.hasNext())
                ((SOAPHeaderBlock) headers.next()).setProcessed();            
        }

        return InvocationResponse.CONTINUE;
    }

}
