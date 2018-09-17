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

import java.util.Collection;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.events.MessageTransferEvent;
import org.holodeckb2b.interfaces.events.IMessageTransferEvent;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for changing the processing state of message units that are and have been
 * sent out in the current SOAP message.
 * <p>When the handler is executed in the flow the processing state of all message units contained in the message is
 * set to {@link ProcessingState#SENDING}. When {@link #flowComplete(org.apache.axis2.context.MessageContext)} is
 * executed the handler checks if the sent operation was successful and changes the processing state accordingly to
 * either {@link ProcessingState#TRANSPORT_FAILURE} or {@link ProcessingState#DELIVERED} /
 * {@link ProcessingState#AWAITING_RECEIPT} (for User Message that should be acknowledged through a Receipt).
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CheckSentResult extends BaseHandler {

    /**
     * @return Indication that this is an OUT_FLOW handler
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    /**
     * This method changes the processing state of the message units to {@link ProcessingState#SENDING} to indicate
     * they are being sent to the other MSH.
     *
     * @param mc            The current message
     * @return              {@link InvocationResponse#CONTINUE} as this handler only needs to run when the message has
     *                      been sent.
     * @throws PersistenceException    When the processing state can not be changed
     */
    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        // Get all message units in this message
        final Collection<IMessageUnitEntity> msgUnits = MessageContextUtils.getSentMessageUnits(mc);
        // And change their processing state
        final StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        for (final IMessageUnitEntity mu : msgUnits) {
            updateManager.setProcessingState(mu, ProcessingState.SENDING);
            log.info(MessageUnitUtils.getMessageUnitName(mu) + " with msg-id ["
                                                                    + mu.getMessageId() + "] is being sent");
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * This method is called after the message was sent out. Depending on the result (success or fault) the processing
     * state of the message units contained in the message is changed. When a fault occurred during sending the state
     * is set to {@link ProcessingState#TRANSPORT_FAILURE}. When the SOAP message is sent out successfully the change
     * depends on the type of message unit:<ul>
     * <li><i>Signal message units</i> : the processing state will be changed to {@link ProcessingState#DELIVERED}</li>
     * <li><i>User message unit</i> : the new processing state depends on whether a receipt is expected. If a receipt
     *  is expected, the new state will be {@link ProcessingState#AWAITING_RECEIPT}, otherwise it will be
     *  {@link ProcessingState#DELIVERED}.</li></ul>
     *
     * @param mc    The current message that was sent out
     */
    @Override
    public void doFlowComplete(final MessageContext mc) {
        // First check if there were messaging units sent
        final Collection<IMessageUnitEntity> msgUnits = MessageContextUtils.getSentMessageUnits(mc);

        if (!Utils.isNullOrEmpty(msgUnits)) {
            log.debug("Check result of sent operation");
            final boolean   success = isSuccessful(mc);
            log.debug("The sent operation was " + (success ? "" : "not ") + "successful");

            //Change processing state of all message units in the message accordingly
            final StorageManager updateManager = HolodeckB2BCore.getStorageManager();
            for (final IMessageUnitEntity mu : msgUnits) {
                try {
                    IMessageTransferEvent transferEvent;
                    if (!success) {
                        updateManager.setProcessingState(mu, ProcessingState.TRANSPORT_FAILURE);
                        transferEvent = new MessageTransferEvent(mu, mc.getFailureReason());
                    } else {
                        // State to set depends on type of message unit
                        if (mu instanceof ISignalMessage) {
                            // Signals are always set to delivered
                            updateManager.setProcessingState(mu, ProcessingState.DELIVERED);
                        } else {
                            log.trace("User Message is sent, check P-Mode if Receipt is expected");
                            // Because we only support One-Way the first leg determines
                            final ILeg leg = HolodeckB2BCore.getPModeSet().get(mu.getPModeId()).getLeg(mu.getLeg());
                            if (leg.getReceiptConfiguration() != null)
                                updateManager.setProcessingState(mu, ProcessingState.AWAITING_RECEIPT);
                            else
                                updateManager.setProcessingState(mu, ProcessingState.DELIVERED);
                        }
                        transferEvent = new MessageTransferEvent(mu);
                    }
                    log.trace("Processing state for message unit [" + mu.getMessageId() + "] changed to "
                                + mu.getCurrentProcessingState().getState());
                    // Raise a message processing event about the transfer
                    HolodeckB2BCore.getEventProcessor().raiseEvent(transferEvent, mc);
                } catch (final PersistenceException databaseException) {
                    // Ai, something went wrong updating the processing state of the message unit. As the message unit
                    // is already processed there is nothing we can other than logging the error
                    log.error("A database error occurred while update the processing state of message unit ["
                                + mu.getMessageId() + "]. Details: " + databaseException.getMessage());
                }
            }
        }
    }
    
    /**
     * Checks whether the message exchange was successful, which in case of AS4 is when there were no exceptions 
     * raised.
     *  
     * @param msgContext	The message context of the sent message
     * @return	<code>true</code> if no exceptions were raised during the message exchange,<br>
     * 			<code>false</code> if there was an exception during the message processing.
     */
    protected boolean isSuccessful(final MessageContext msgContext) {
    	return msgContext.getFailureReason() == null;
    }
}