/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.util;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.handler.BaseHandler;


/**
 * Is a utility handler that logs the <code>SOAP:Envelope</code> element from the current message to a specific log
 * depending whether the message is in or outbound: <i>org.holodeckb2b.msgproc.soapenvlog.</i>(<i>IN|OUT</i>). The
 * log level used is INFO, so the logging can be enabled or disabled by setting the log level.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SOAPEnvelopeLogger extends BaseHandler {

    /**
     * We use a specific log for the SOAP headers so it can easily be enabled or disabled
     */
    private Log soapEnvLog = null;
    
            
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW | OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        // Create the log when it does not exist yet
        if (soapEnvLog != null) 
            soapEnvLog = LogFactory.getLog("org.holodeckb2b.msgproc.soapenvlog." 
                                            + (isInFlow((byte) (IN_FLOW | IN_FAULT_FLOW)) ? "IN" : "OUT"));
        
        // Only do something when logging is enabled
        if (soapEnvLog.isInfoEnabled()) {
            soapEnvLog.info(mc.getEnvelope().toStringWithConsume() + "\n");            
        }
        
        return InvocationResponse.CONTINUE;
    }
    
}
