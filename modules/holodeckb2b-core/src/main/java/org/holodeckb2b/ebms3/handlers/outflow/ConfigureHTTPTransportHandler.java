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
package org.holodeckb2b.ebms3.handlers.outflow;

import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.holodeckb2b.common.handler.AbstractBaseHandler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.events.MessageTransfer;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.PModeUtils;

/**
 * Is the <i>OUT_FLOW</i> handler that configures the actual message transport over the HTTP protocol. When this 
 * message is sent as a HTTP request this handler will find the determine the destination URL based on the P-Mode of
 * the primary message unit. It will also enable <i>HTTP gzip compression</i> and <i>HTTP chunking</i> based on the 
 * P-Mode settings (in <b>PMode[1].Protocol</b>). The actual configuration is done by setting specific {@link Options} 
 * which define the:<ul>
 * <li>Transfer-encoding : When sending messages with large payloads included in the SOAP Body it is useful to compress
 *      the messages during transport. This is done by the standard compression feature of HTTP/1.1 by using the
 *      <i>gzip</i> Transfer-Encoding.<br>
 *      Whether compression should be enabled is configured in the P-Mode that controls the message transfer. Only if
 *      parameter <code>PMode.Protocol.HTTP.Compression</code> has value <code>true</code> compression is enabled.<br>
 *      When compression is enable two options are set to "true": {@see HTTPConstants.#CHUNKED} and
 *      {@see HTTPConstants#MC_GZIP_REQUEST} or {@see HTTPConstants#MC_GZIP_RESPONSE} depending on whether Holodeck B2B
 *      is the initiator or responder in the current message transfer.<br>
 *      That both the chunked and gzip encodings are enabled is a requirement from HTTP
 *      (see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6">http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6</a>).</li>
 * <li>EndpointReference : Defines where the message must be delivered. Only relevant when Holodeck B2B is initiator 
 * 		of the message transfer, if Holodeck B2B is responding to a request received from another MSH, the message is 
 * 		just added in the response.</li>
 * </ul>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ConfigureHTTPTransportHandler extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Log log) 
    																					throws PersistenceException {
        final IMessageUnitEntity primaryMU = procCtx.getPrimaryMessageUnit();
        // Only when message contains a message unit there is something to do
        if (primaryMU == null) 
        	return InvocationResponse.CONTINUE;
        
        final MessageContext axisMsgCtx = procCtx.getParentContext();
    	// Disable use of SOAP Action (=> will result in empty SOAPAction http header for SOAP 1.1)
    	axisMsgCtx.getOptions().setProperty(Constants.Configuration.DISABLE_SOAP_ACTION, "true");	
        log.trace("Get P-Mode configuration for primary MU");
        
        ILeg leg;
        try {
        	leg = PModeUtils.getLeg(primaryMU);
        } catch (IllegalStateException pmodeNotAvailable) {
        	log.error("P-Mode set for primary message unit [" + primaryMU.getMessageId() + "] is not available");
        	leg = null;
        }
        // For response error messages the P-Mode may be unknown, so no special HTTP configuration
        if (leg == null)
        	if (axisMsgCtx.isServerSide()) {
        		// When responding we don't need P-Mode for configuring HTTP and use default settings
        		log.debug("No P-Mode given for primary message unit, using default HTTP configuration");
        		return InvocationResponse.CONTINUE;
        	} else {
        		// For sending however we need the P-Mode to provide the URL, so the process cannot here
        		log.error("No P-Mode available for primary message unit [msgId=" + primaryMU.getMessageId() 
        					+ "] to get the destination!");
        		setMessagesToFailed(procCtx, log);
        		return InvocationResponse.ABORT;            		
        	}            		
        // Get current set of options
        final Options options = axisMsgCtx.getOptions();
        final IProtocol protocolCfg = leg.getProtocol();

        // If Holodeck B2B is initiator the destination URL must be set
        if (!axisMsgCtx.isServerSide()) {
            // Get the destination URL via the P-Mode of this message unit
            String destURL = null;
            try {
                // First we check if the Receipt or Error signal have a specific URL defined
                try {
                   if (primaryMU instanceof IReceipt)
                        destURL = leg.getReceiptConfiguration().getTo();
                    else if (primaryMU instanceof IErrorMessage)
                        destURL = leg.getUserMessageFlow().getErrorHandlingConfiguration().getReceiverErrorsTo();
                } catch (NullPointerException npe) {}
                // If not we use the URL defined on the leg level which is also the one to use for UserMessage and
                // PullRequest
                if (destURL == null)
                    destURL = leg.getProtocol().getAddress();
            } catch (final NullPointerException npe) {
                // The P-Mode does not contain the necessary information, unable to sent this message!
                log.error("P-Mode does not contain destination URL for " 
                			+ MessageUnitUtils.getMessageUnitName(primaryMU));
                setMessagesToFailed(procCtx, log);
            }
            log.debug("Destination URL=" + destURL);
            axisMsgCtx.setProperty(Constants.Configuration.TRANSPORT_URL, destURL);
        }

        // Check if HTTP compression and/or chunking should be used and set options accordingly
        final boolean compress = (protocolCfg != null ? protocolCfg.useHTTPCompression() : false);

        if (compress) 
            log.debug("Enable HTTP compression using gzip Content-Encoding");
        if (!axisMsgCtx.isServerSide())
            // Holodeck B2B is sending the message, so request has to be compressed
            options.setProperty(HTTPConstants.MC_GZIP_REQUEST, Boolean.valueOf(compress));
        else
            // Holodeck B2B is responding the message, so request has to be compressed
            options.setProperty(HTTPConstants.MC_GZIP_RESPONSE, Boolean.valueOf(compress));

        // Check if HTTP "chunking" should be used. In case of gzip CE, chunked TE is required. But as Axis2 does
        // not automaticly enable this we also enable chunking here when compression is used
        if (compress || (protocolCfg != null ? protocolCfg.useChunking() : false)) {
            log.debug("Enable chunked transfer-encoding");
            options.setProperty(HTTPConstants.CHUNKED, Boolean.TRUE);
        } else {
            log.debug("Disable chunked transfer-encoding");
            options.setProperty(HTTPConstants.CHUNKED, Boolean.FALSE);
        }

        // If the message does not contain any attachments we can disable SwA
        if (axisMsgCtx.getAttachmentMap().getContentIDSet().isEmpty()) {
            log.trace("Disable SwA as message does not contain attachments");
            options.setProperty(Constants.Configuration.ENABLE_SWA, Boolean.FALSE);
        } else
        	options.setProperty(Constants.Configuration.ENABLE_SWA, Boolean.TRUE);
        
        log.trace("HTTP configuration done");
        
        return InvocationResponse.CONTINUE;
    }

    /**
     * Sets the processing state of all message units in the message to FAILURE and raises a {@link MessageTransfer} 
     * event.
     * 
     * @param mc	The message processing context
     * @param log	The Log to be used
     * @throws PersistenceException  When the processing state cannot be saved in the database
     */
    private void setMessagesToFailed(final MessageProcessingContext procCtx, final Log log) throws PersistenceException {
    	final StorageManager updManager = HolodeckB2BCore.getStorageManager();
    	final IMessageProcessingEventProcessor eventProcessor = HolodeckB2BCore.getEventProcessor();
    	for(IMessageUnitEntity mu : procCtx.getSendingMessageUnits()) {
			log.debug("Updating processing state to FAILURE for message unit [msgId=" 
						+ mu.getMessageId() + "]");
			updManager.setProcessingState(mu, ProcessingState.FAILURE);
			eventProcessor.raiseEvent(new MessageTransfer(mu, new Exception("Unable to configure HTTP Connection")));
		}    	
    }

}
