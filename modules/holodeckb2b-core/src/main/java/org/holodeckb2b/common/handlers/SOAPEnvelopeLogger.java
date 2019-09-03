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

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.handlers.MessageProcessingContext;


/**
 * Is a utility handler that logs the <code>SOAP:Envelope</code> element from the current message to a specific log
 * depending whether the message is in or outbound: <i>org.holodeckb2b.msgproc.soapenvlog.</i>(<i>IN|OUT</i>). The
 * log level used is INFO, so the logging can be enabled or disabled by setting the log level.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SOAPEnvelopeLogger extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Logger log) throws Exception {
        // We use a specific log for the SOAP headers so it can easily be enabled or disabled
    	final int currentFlow = procCtx.getParentContext().getFLOW();
        final boolean incoming = currentFlow == MessageContext.IN_FLOW || currentFlow == MessageContext.IN_FAULT_FLOW;
        final Log soapEnvLog = LogFactory.getLog("org.holodeckb2b.msgproc.soapenvlog." + (incoming ? "IN" : "OUT"));

        // Only do something when logging is enabled
        if (soapEnvLog.isInfoEnabled()) {
            try {
            	soapEnvLog.info(procCtx.getParentContext().getEnvelope().cloneOMElement().toStringWithConsume() + "\n");
            } catch (Exception invalidSOAP) {
            	if (incoming) {
            		log.warn("Received a message with invalid SOAP envelope! Details: " 
            				+ Utils.getExceptionTrace(invalidSOAP));
            		soapEnvLog.error("Received message cannot be logged because its SOAP envelope is not valid!");
            	} else {
            		log.error("Created message does not contain a valid SOAP envelope! Details: " 
            				+ Utils.getExceptionTrace(invalidSOAP));
            	}
            }
        }

        return InvocationResponse.CONTINUE;
    }

}
