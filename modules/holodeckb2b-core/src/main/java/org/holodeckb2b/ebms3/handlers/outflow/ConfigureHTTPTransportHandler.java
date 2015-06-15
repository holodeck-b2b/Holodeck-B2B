/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.handlers.outflow;

import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IPModeSet;
import org.holodeckb2b.common.pmode.IProtocol;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Configures the actual message transport over the HTTP protocol. Configuration
 * is done by setting specific {@see Options}. Not all options (see 
 * <a href="http://wso2.com/library/230/#HTTPConstants">http://wso2.com/library/230/#HTTPConstants</a> 
 * for an overview of all options), are relevant for Holodeck B2B. The options
 * set by this handler are:
 * <ul>
 * <li>Transfer-encoding : When sending messages with large payloads included in 
 * the SOAP Body it is useful to compress the messages during transport. This
 * is done by the standard compression feature of HTTP/1.1 by using the <i>gzip</i>
 * Transfer-Encoding.<br>
 * Whether compression should be enabled is configured in the P-Mode that controls
 * the message transfer. Only if parameter <code>PMode.Protocol.HTTP.Compression</code> 
 * has value <code>true</code> compression is enabled.<br>
 * When compression is enable two options are set to "true": {@see HTTPConstants.#CHUNKED}
 * and {@see HTTPConstants#MC_GZIP_REQUEST} or {@see HTTPConstants#MC_GZIP_RESPONSE} 
 * depending on whether Holodeck B2B is the initiator or responder in the current
 * message transfer.<br>
 * That both the chunked and gzip encodings are enabled is a requirement from HTTP 
 *(see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6">http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6</a>).</li>
 * <li>EndpointReference : The EndpointReference is used to determine the
 * MSH where the message must be delivered. This only relevant when Holodeck B2B
 * is initiator of the message transfer, if Holodeck B2B is responding to a request
 * received from another MSH, the message is just added in the response.<br>
 * In a point to point situation this only consists of an URL, but in a multi-hop
 * context there must be more information to determine the ultimate receiver of
 * the message. As Holodeck B2B currently only support point to point exchanges 
 * we directly set the URL.<br>
 * It is possible that the processed message contains multiple message units. In 
 * that case the P-Mode of the <i>primary</i> message unit (the one contained in
 * the MessageContext parameter {@see MessageContextParameters.#OUT_MESSAGE_UNIT}) 
 * will be used to set the URL.</li>
 * </ul>
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ConfigureHTTPTransportHandler extends BaseHandler {

    /**
     * Configuration of HTTP is only necessary in the OUT_FLOW because the OUT_FAULT_FLOW is always an HTTP response
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) {
        // First check if this is an ebMS message, i.e. contains ebMS messaging header
        if (Messaging.getElement(mc.getEnvelope()) == null)
            return InvocationResponse.CONTINUE;

        // Get current set of options
        Options options = mc.getOptions();
        
        // Get the primary message unit that is processed
        log.debug("Get the primary MessageUnit from MessageContext");
        MessageUnit primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc);
        
        // Only when message contains a message unit there is something to do
        if (primaryMU != null) {
            log.debug("Get P-Mode configuration for primary MU");
            IPModeSet pmSet = HolodeckB2BCore.getPModeSet();
            IPMode pmode = pmSet.get(primaryMU.getPMode());
            // For response error messages the P-Mode may be unknown, so no special HTTP configuration
            if (pmode == null) {
                log.debug("No P-Mode given for primary message unit, using default HTTP configuration");
                return InvocationResponse.CONTINUE;
            }
            
            // Currently only One-Way MEPs are supported, so always on first leg
            ILeg leg = pmode.getLegs().iterator().next();
            IProtocol protocolCfg = leg.getProtocol();
            
            // If Holodeck B2B is initiator the destination URL must be set
            if (isInFlow(INITIATOR)) {
                // Get the destination URL via the P-Mode of this message unit
                String destURL = null;
                try {
                    // First we check if the Receipt or Error signal have a specific URL defined
                    try {
                       if (primaryMU instanceof Receipt) 
                            destURL = leg.getReceiptConfiguration().getTo();
                        else if (primaryMU instanceof ErrorMessage)
                            destURL = leg.getUserMessageFlow().getErrorHandlingConfiguration().getReceiverErrorsTo(); 
                    } finally {}
                    
                    // If not we use the URL defined on the leg level which is also the one to use for UserMessage and
                    // PullRequest
                    if (destURL == null) 
                        destURL = leg.getProtocol().getAddress();
                } catch (NullPointerException npe) {
                    // The P-Mode does not contain the necessary information, unable to sent this message!
                    log.error("P-Mode does not contain destination URL for " + primaryMU.getClass().getSimpleName());
                }
                log.debug("Destination URL=" + destURL);
                mc.setProperty(Constants.Configuration.TRANSPORT_URL, destURL);
            }

            // Check if HTTP compression and/or chunking should be used and set options accordingly
            boolean compress = (protocolCfg != null ? protocolCfg.useHTTPCompression() : false);
            
            if (compress) {
                log.debug("Enable HTTP compression using gzip Content-Encoding");
                log.debug("Enable gzip content-encoding");
                if (isInFlow(INITIATOR))
                    // Holodeck B2B is sending the message, so request has to be compressed
                    options.setProperty(HTTPConstants.MC_GZIP_REQUEST, Boolean.TRUE);
                else
                    // Holodeck B2B is responding the message, so request has to be compressed
                    options.setProperty(HTTPConstants.MC_GZIP_RESPONSE, Boolean.TRUE);
            }
            
            // Check if HTTP "chunking" should be used. In case of gzip CE, chunked TE is required. But as Axis2 does 
            // not automaticly enable this we also enable chunking here when compression is used
            if (compress || (protocolCfg != null ? protocolCfg.useChunking() : true)) {
                log.debug("Enable chunked transfer-encoding");
                options.setProperty(HTTPConstants.CHUNKED, Boolean.TRUE);
            } else {
                log.debug("Disable chunked transfer-encoding");
                options.setProperty(HTTPConstants.CHUNKED, Boolean.FALSE);                
            }
            
            log.debug("HTTP configuration done");
        } else
            log.debug("Message does not contain ebMS message unit, nothing to do");

        return InvocationResponse.CONTINUE;
    }
    

}
