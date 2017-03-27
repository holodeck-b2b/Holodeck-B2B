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
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.packaging.Messaging;

/**
 * Is the handler responsible for checking if a EBMS SOAPHeaderBlock is present
 * and setting it as processed.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class ReportHeaderProcessed extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext msgCtx) throws AxisFault {

        final SOAPEnvelope env = msgCtx.getEnvelope();
        if (env != null) {
            final SOAPHeaderBlock messagingHdr = Messaging.getElement(env);
            if (messagingHdr != null) {
                messagingHdr.setProcessed();
            }
        }

        return InvocationResponse.CONTINUE;
    }

}
