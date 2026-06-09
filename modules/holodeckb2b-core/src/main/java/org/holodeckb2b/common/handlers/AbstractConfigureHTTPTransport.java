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
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.events.impl.MessageTransferFailure;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.core.transport.http.HTTPTransportSender;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.StorageException;

/**
 * Is a base class for that implements the <i>out_flow</i> handler that is used to set the URL of the other MSH in case
 * of outgoing messages and perform additional configuration of the actual message transport over the HTTP protocol
 * required by the messaging protocol.
 * <p>
 * As the determination of where the message should be send to is protocol dependent implementation must implement
 * the {@link #getDestinationURL(IMessageUnitEntity, ILeg, IMessageProcessingContext)} method to provide the target URL.
 * <p>
 * Additionally implementations can override the {@link #prepareHttp(IMessageUnitEntity, IMessageProcessingContext)}
 * method to perform additional configuration of the HTTP transport specific to the messaging protocol. Note that most
 * HTTP configuration is already handled by the Holodeck B2B Core and does not need to be done in message handlers.
 *
 * @author Sander Fieten (sander at chasquis-consulting.com)
 * @since 6.0.0
 * @see HTTPTransportSender
 */
public abstract class AbstractConfigureHTTPTransport extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, Logger log) throws StorageException {
        final IMessageUnitEntity primaryMU = procCtx.getPrimaryMessageUnit();
        // Only when message contains a message unit there is something to do
        if (primaryMU == null) {
        	log.debug("Message does not contain a message unit, nothing to do");
        	return InvocationResponse.CONTINUE;
        }

        // If Holodeck B2B is initiator the destination URL must be set
        if (procCtx.isHB2BInitiated()) {
            // Get the protocol configuration via the P-Mode of this message unit
            IProtocol protocol = null;
            String destURL;
            try {
            	protocol = getProtocolConfig(primaryMU, procCtx);
            } catch (Throwable t) {
            	log.error("Error in determination of target URL for message (msgID={}) : {}",
            				primaryMU.getMessageId(), t.getMessage());
            }
            if (protocol == null || Utils.isNullOrEmpty(destURL = protocol.getAddress())) {
            	// No destination URL available, unable to sent this message!
                log.error("No destination URL available for " + MessageUnitUtils.getMessageUnitName(primaryMU)
                			+ " with msgId: " + primaryMU.getMessageId());
                final StorageManager updManager = HolodeckB2BCore.getStorageManager();
            	final IMessageProcessingEventProcessor eventProcessor = HolodeckB2BCore.getEventProcessor();
            	for(IMessageUnitEntity mu : procCtx.getSendingMessageUnits()) {
            		// As the configuration can be added to the P-Mode put message unit in SUSPENDED state so they can
            		// be resumed when the P-Mode is updated
        			log.debug("Updating processing state to SUSPENDED for message unit [msgId="
        						+ mu.getMessageId() + "]");
        			updManager.setProcessingState(mu, ProcessingState.SUSPENDED);
        			eventProcessor.raiseEvent(new MessageTransferFailure(mu,
        														new Exception("Unable to configure HTTP Connection")));
        		}
                return InvocationResponse.ABORT;
            }
            log.debug("Destination URL = {}", destURL);
            procCtx.getParentContext().setProperty(Constants.Configuration.TRANSPORT_URL, destURL);
            procCtx.getParentContext().setProperty(HTTPTransportSender.MC_HTTP_CONFIG, protocol);
        }
        prepareHttp(primaryMU, procCtx);
        log.debug("HTTP configuration done");

        return InvocationResponse.CONTINUE;
    }

	/**
	 * Gets the protocol configuration for the given message unit.
	 *
	 * @param msgToSend		The primary message unit being send
	 * @param procCtx		The message processing context
	 * @return				A {@link IProtocol} instance with the protocol configuration,<br/>
	 * 						or <code>null</code> if the configuration cannot be determined
	 * @since 8.2.0
	 */
	protected abstract IProtocol getProtocolConfig(IMessageUnitEntity msgToSend, IMessageProcessingContext procCtx);

	/**
	 * Sets the messaging protocol specific HTTP configuration.
	 * <p>
	 * NOTE: Most HTTP configuration is already handled by the Holodeck B2B Core and does not need to be done in message
	 * handlers.
	 *
	 * @param msgToSend		The primary message unit being send
	 * @param procCtx		The message processing context
	 * @since 8.0.0
	 * @see HTTPTransportSender
	 */
	protected void prepareHttp(IMessageUnitEntity msgToSend, IMessageProcessingContext procCtx) {
	}
}
