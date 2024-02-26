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
package org.holodeckb2b.core.handlers.inflow;

import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.ValueInconsistent;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the <i>IN_FLOW</i> handler responsible for processing received receipt signals.
 * <p>For each {@link IReceiptEntity} in the message processing context it will check if the referenced <i>User Message
 * </i> expects a Receipt and if so mark it as delivered. This is done by checking the <i>ReceiptConfiguration</i> for
 * the leg ({@link ILeg#getReceiptConfiguration()} and the referenced User Message's processing state. If no
 * configuration exists Receipts are not expected and a <i>ProcessingModeMismatch</i> error is generated for the
 * Receipt and its processing state is set to {@link ProcessingState#FAILURE}.<br>
 * If a receipt configuration exists and if there is a User Message waiting for a Receipt it will be marked as delivered
 * and the Receipt's state will be set to {@link ProcessingState#READY_FOR_DELIVERY} to indicate that it can be
 * delivered to the business application. The actual delivery is done by the {@link DeliverReceipts} handler.<br>
 * If a receipt configuration exists but the user message is not waiting for a receipt anymore (because it is already
 * acknowledged by another Receipt or it has failed due to an Error) the Receipt's processing state will be changed to
 * {@link ProcessingState#DONE}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessReceipts extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log)
    																					throws StorageException {
        log.debug("Check for received receipts in message.");
        final Collection<IReceiptEntity>  receipts = procCtx.getReceivedReceipts();

        if (!Utils.isNullOrEmpty(receipts)) {
            log.debug("Message contains " + receipts.size() + " Receipts signals, start processing");
            for (final IReceiptEntity r : receipts)
                // Ignore Receipts that already failed
                if (r.getCurrentProcessingState().getState() != ProcessingState.FAILURE)
                    processReceipt(r, procCtx, log);
            log.debug("All Receipts in message processed");
        }
        return InvocationResponse.CONTINUE;
    }

    /**
     * Processes one Receipt signal.
     *
     * @param receipt   The receipt signal to process
     * @param procCtx   The message processing context of the message containing the receipt
     * @param log		Log to be used
     * @throws StorageException When a database error occurs while processing the Receipt Signal
     */
    protected void processReceipt(final IReceiptEntity receipt, final IMessageProcessingContext procCtx,
    							  final Logger log) throws StorageException {
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        // Change processing state to indicate we start processing the receipt. Also checks that the receipt is not
        // already being processed
        if (!updateManager.setProcessingState(receipt, ProcessingState.PROCESSING)) {
            log.warn("Receipt [msgId=" + receipt.getMessageId() + "] is already being processed, skipping");
            return;
        }

        final String refToMsgId = receipt.getRefToMessageId();
        log.trace("Start processing Receipt [msgId=" + receipt.getMessageId()
                    + "] for referenced message with msgId=" + refToMsgId);

        Collection<IMessageUnitEntity> refdMsgs = null;
        final IMessageUnitEntity sentMessage = procCtx.getSendingMessageUnit(refToMsgId);
        if (sentMessage != null)
        	refdMsgs = Collections.singletonList(sentMessage);
        else
        	refdMsgs = HolodeckB2BCore.getQueryManager().getMessageUnitsWithId(refToMsgId, Direction.OUT);
        if (Utils.isNullOrEmpty(refdMsgs)) {
            // This error SHOULD NOT occur because the reference is already checked when finding the P-Mode
            log.warn("Receipt [msgId=" + receipt.getMessageId() + "] contains unknown refToMessageId ["
                        + refToMsgId + "]!");
            updateManager.setProcessingState(receipt, ProcessingState.FAILURE);
            // Create error and add to context
            procCtx.addGeneratedError(new ValueInconsistent("Receipt contains unknown reference [" + refToMsgId + "]",
                                                            receipt.getMessageId()));
        } else {
            // The collection can only contain one User Message as messageId of sent message are guaranteed to be unique
            IMessageUnitEntity  ackedMessage = refdMsgs.iterator().next();
            if (ackedMessage instanceof IUserMessage) {
                // Check if the found message unit expects a receipt
                final ILeg leg = PModeUtils.getLeg(ackedMessage);
                if (leg.getReceiptConfiguration() == null) {
                    // The P-Mode is not configured for receipts, generate error
                    updateManager.setProcessingState(receipt, ProcessingState.FAILURE);
                    // Create error and add to context
                    procCtx.addGeneratedError(new ValueInconsistent("P-Mode of referenced message [" + refToMsgId
                    												+ "] is not configured for receipts",
                    												receipt.getMessageId()));
                }  else {
                    // Change processing state of the reference message unit to delivered, but only if it is
                    // waiting for a receipt as we may otherwise overwrite an error state.
                    if (isWaitingForReceipt(ackedMessage)) {
                        log.info("Receipt received for User Message [msgId= " + refToMsgId
                        			+ "] => successfully delivered");
                        updateManager.setProcessingState(ackedMessage, ProcessingState.DELIVERED);
                        // Maybe the Receipt must also be delivered to the business application, so change state
                        // to "ready for delivery"
                        log.trace("Mark Receipt as ready for delivery to business application");
                        updateManager.setProcessingState(receipt, ProcessingState.READY_FOR_DELIVERY);
                    } else {
                        log.trace("Found message unit not waiting for receipt anymore, processing finished.");
                        updateManager.setProcessingState(receipt, ProcessingState.DONE);
                    }
                }
            } else {
                // This Receipt is for a non User Message which is not allowed by the ebMS V3 Specification!
                // => create an error
                log.warn("Receipt [" + receipt.getMessageId() + "] must refer to User Message, but refers to "
                         + MessageUnitUtils.getMessageUnitName(ackedMessage) + "[" + refToMsgId + "]!");
                procCtx.addGeneratedError(new ValueInconsistent("Receipt does not reference a User Message message unit"
                												, receipt.getMessageId()));
                // The message processing of the error fails
                log.trace("Mark Receipt as failed");
                updateManager.setProcessingState(receipt, ProcessingState.FAILURE);
            }
        }
        log.trace("Done processing Receipt");
    }

    /**
     * Checks whether the message unit is waiting for receipt.
     * <p>The message unit is waiting for a receipt when its current processing state is:<ol>
     * <li>either {@link ProcessingState#AWAITING_RECEIPT} or {@link ProcessingState#TRANSPORT_FAILURE}</li>
     * <li>either {@link ProcessingState#READY_TO_PUSH}, {@link ProcessingState#AWAITING_PULL},
     * 		{@link ProcessingState#SUSPENDED} or {@link ProcessingState#WARNING}
     *    <b>and</b> the previous state was {@link ProcessingState#AWAITING_RECEIPT}
     *    			 or {@link ProcessingState#TRANSPORT_FAILURE}</li>
     * </ol>
     * <p>The check must always include the current state as we do not want to change a processing state that is final.
     *
     * @param mu    The {@link IMessageUnitEntity} to check
     * @return      <code>true</code> if the message unit is waiting for a receipt, <code>false</code> otherwise
     * @throws org.holodeckb2b.interfaces.persistency.StorageException When the given message unit entity can not
     *                                                                     be loaded from storage
     */
    protected boolean isWaitingForReceipt(final IMessageUnitEntity mu) throws StorageException {
        ProcessingState currentState = mu.getCurrentProcessingState().getState();
        if (currentState == ProcessingState.AWAITING_RECEIPT || currentState == ProcessingState.TRANSPORT_FAILURE)
            return true;
        else if (currentState == ProcessingState.AWAITING_PULL || currentState == ProcessingState.READY_TO_PUSH
        		|| currentState == ProcessingState.WARNING || currentState == ProcessingState.SUSPENDED) {
            ProcessingState prevState = mu.getProcessingStates().get(mu.getProcessingStates().size() - 2).getState();
            return prevState == ProcessingState.AWAITING_RECEIPT || prevState == ProcessingState.TRANSPORT_FAILURE;
        } else
            return false;
    }
}
