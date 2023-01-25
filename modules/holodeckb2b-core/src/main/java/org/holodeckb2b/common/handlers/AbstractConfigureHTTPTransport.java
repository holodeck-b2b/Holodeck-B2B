/*
 * Copyright (C) 2018 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.events.impl.MessageTransferFailure;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is a base class for that implements the <i>out_flow</i> handler that configures the actual message transport over the
 * HTTP protocol. Based on the P-Mode [settings in <b>PMode[1].Protocol</b>] of the primary message unit it will enable
 * <i>HTTP gzip compression</i> and <i>HTTP chunking</i>.
 * <p>As the determination of where the message should be send to is protocol dependent implementation must implement
 * the {@link #getDestinationURL(IMessageUnitEntity, ILeg, IMessageProcessingContext)} method to provide the target URL.
 * <p>The actual configuration is done by setting specific {@link Options} which define the:<ul>
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
 * 		just added in the response. When the message unit to be send is a <i>Receipt</i> or <i>Error</i> the
 * 		destination URL can beside in the P-Mode also be provided by the Sender of the message in the MDN request.
 * 		If the Sender supplied a URL it will take precedence over the one set in the P-Mode.</li>
 * </ul>
 *
 * @author Sander Fieten (sander at chasquis-consulting.com)
 * @since 6.0.0
 */
public abstract class AbstractConfigureHTTPTransport extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, Logger log) throws PersistenceException {
        final IMessageUnitEntity primaryMU = procCtx.getPrimaryMessageUnit();
        // Only when message contains a message unit there is something to do
        if (primaryMU == null) {
        	log.debug("Message does not contain a message unit, nothing to do");
                return InvocationResponse.CONTINUE;
            }

        log.debug("Get P-Mode Leg for primary MU (msgID={})", primaryMU.getPModeId(), primaryMU.getMessageId());
        final ILeg leg = getLeg(primaryMU);

        /* For sending the request we need the P-Mode to provide the URL, so the process cannot if we didn't find a
         Leg for requests, but for response Signal Messages there may not be a specific out leg defined and we can just
         use default values and use the HTTP response
        */
        if (leg == null && !(primaryMU instanceof ISignalMessage) && procCtx.isHB2BInitiated()) {
    		log.error("P-Mode configuration not available for primary message unit [msgId={}]!",
    				  primaryMU.getMessageId());
            		setMessagesToFailed(procCtx, log);
            		return InvocationResponse.ABORT;
            	}

            // If Holodeck B2B is initiator the destination URL must be set
            if (procCtx.isHB2BInitiated()) {
                // Get the destination URL via the P-Mode of this message unit
                String destURL = null;
                try {
                	destURL = getDestinationURL(primaryMU, leg, procCtx);
                } catch (Throwable t) {
                	log.error("Error in determination of target URL for message (msgID={}) : {}",
                				primaryMU.getMessageId(), t.getMessage());
                }
                if (Utils.isNullOrEmpty(destURL)) {
                	// No destination URL available, unable to sent this message!
                    log.error("No destination URL availabel for " + MessageUnitUtils.getMessageUnitName(primaryMU)
                    			+ " with msgId: " + primaryMU.getMessageId());
                    setMessagesToFailed(procCtx, log);
                    return InvocationResponse.ABORT;
                }
                log.debug("Destination URL=" + destURL);
                procCtx.getParentContext().setProperty(Constants.Configuration.TRANSPORT_URL, destURL);
            }

            // Get current set of options
            final Options options = procCtx.getParentContext().getOptions();
            // Check if HTTP compression and/or chunking should be used and set options accordingly
            final IProtocol protocolCfg = leg.getProtocol();
            final boolean compress = (protocolCfg != null ? protocolCfg.useHTTPCompression() : false);
            if (compress) {
                log.debug("Enable HTTP compression using gzip Content-Encoding");
                log.debug("Enable gzip content-encoding");
                if (procCtx.isHB2BInitiated())
                    // Holodeck B2B is sending the message, so request has to be compressed
                    options.setProperty(HTTPConstants.MC_GZIP_REQUEST, Boolean.TRUE);
                else
                    // Holodeck B2B is responding the message, so request has to be compressed
                    options.setProperty(HTTPConstants.MC_GZIP_RESPONSE, Boolean.TRUE);
            }

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
            if (procCtx.getParentContext().getAttachmentMap().getContentIDSet().isEmpty()) {
                log.debug("Disable SwA as message does not contain attachments");
                options.setProperty(Constants.Configuration.ENABLE_SWA, Boolean.FALSE);
            }

            // Disable use of SOAP Action (=> will result in empty SOAPAction http header for SOAP 1.1)
        	options.setProperty(Constants.Configuration.DISABLE_SOAP_ACTION, "true");

            log.debug("HTTP configuration done");
        } else
            log.debug("Message does not contain ebMS message unit, nothing to do");

        return InvocationResponse.CONTINUE;
    }

    /**
     * Gets the Leg of the P-Mode that configures the sending of the given message. Normally the Leg found by
     * {@linkplain PModeUtils#getLeg()} will work, but for some messages and protocols the leg may need to be determined
     * differently. This can be done by overriding this method.
     *
     * @param msgToSend		the message for which to get the Leg
     * @return	the Leg that configures the sending of the message if found, <code>null</code> otherwise
     */
    protected ILeg getLeg(IMessageUnitEntity msgToSend) {
        try {
        	return PModeUtils.getLeg(msgToSend);
        } catch (IllegalStateException pmodeNotAvailable) {
        	return null;
        }
    }

	/**
	 * Gets the destination URL for the given message unit.
	 *
	 * @param msgToSend		The message unit being send
	 * @param leg			The P-Mode configuration parameters for this leg
	 * @param mc			The message processing context
	 * @return				The destination URL, <code>null</code> if URL cannot be determined
	 */
	protected abstract String getDestinationURL(IMessageUnitEntity msgToSend, ILeg leg,
												IMessageProcessingContext procCtx);

	 /**
     * Sets the processing state of all message units in the message to FAILURE and raises a {@link MessageTransferFailure}
     * event.
     *
     * @param mc	The message processing context
     * @param log	The Log to be used
     * @throws PersistenceException  When the processing state cannot be saved in the database
     */
    private void setMessagesToFailed(final IMessageProcessingContext procCtx, final Logger log) throws PersistenceException {
    	final StorageManager updManager = HolodeckB2BCore.getStorageManager();
    	final IMessageProcessingEventProcessor eventProcessor = HolodeckB2BCore.getEventProcessor();
    	for(IMessageUnitEntity mu : procCtx.getSendingMessageUnits()) {
			log.debug("Updating processing state to FAILURE for message unit [msgId="
						+ mu.getMessageId() + "]");
			updManager.setProcessingState(mu, ProcessingState.FAILURE);
			eventProcessor.raiseEvent(new MessageTransferFailure(mu, new Exception("Unable to configure HTTP Connection")));
		}
    }

}
