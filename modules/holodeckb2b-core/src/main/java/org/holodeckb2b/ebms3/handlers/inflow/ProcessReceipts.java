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

import java.util.Collection;
import java.util.List;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Is the <i>IN_FLOW</i> handler responsible for processing received receipt signals.
 * <p>For each {@link Receipt} in the message (indicated by message context property
 * {@link MessageContextProperties#IN_RECEIPTS}) it will check if the referenced {@link UserMessage} expects a Receipt
 * anyway. This is done by checking the <i>ReceiptConfiguration</i> for the leg ({@link ILeg#getReceiptConfiguration()}.
 * If no configuration exists Receipt are not expected and a <i>ProcessingModeMismatch</i> error is generated for the
 * Receipt and its processing state is set to {@link ProcessingStates#FAILURE}.<br>
 * If a receipt configuration exists and if the user message is waiting for a receipt it will be marked as delivered and
 * the Receipt's state will be set to {@link ProcessingStates#READY_FOR_DELIVERY} to indicate that it can be delivered
 * to the business application. The actual delivery is done by the {@link DeliverReceipts} handler.<br>
 * If a receipt configuration exists but the user message is not waiting for a receipt anymore (because it is already
 * acknowledged by another Receipt or it has failed due to an Error) the Receipt's processing state will be changed to
 * {@link ProcessingStates#DONE}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessReceipts extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        log.debug("Check for received receipts in message.");
        final Collection<IReceiptEntity>  receipts =
                                (Collection<IReceiptEntity>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);

        if (!Utils.isNullOrEmpty(receipts)) {
            log.debug("Message contains " + receipts.size() + " Receipts signals, start processing");
            for (final IReceiptEntity r : receipts)
                // Ignore Receipts that already failed
                if (r.getCurrentProcessingState().getState() != ProcessingState.FAILURE)
                    processReceipt(r, mc);
            log.debug("Receipts processed");
        } else
            log.debug("Message does not contain receipts, continue processing");

        return InvocationResponse.CONTINUE;
    }

    /**
     * Processes one Receipt signal.
     *
     * @param receipt   The receipt signal to process
     * @param mc        The message context of the message containing the receipt
     * @throws PersistenceException When a database error occurs while processing the Receipt Signal
     */
    protected void processReceipt(final IReceiptEntity receipt, final MessageContext mc) throws PersistenceException {
        StorageManager updateManager = HolodeckB2BCore.getStoreManager();
        // Change processing state to indicate we start processing the receipt. Also checks that the receipt is not
        // already being processed
        if (!updateManager.setProcessingState(receipt, ProcessingState.RECEIVED, ProcessingState.PROCESSING)) {
            log.debug("Receipt [msgId=" + receipt.getMessageId() + "] is already being processed, skipping");
            return;
        }

        final String refToMsgId = receipt.getRefToMessageId();
        log.debug("Start processing Receipt [msgId=" + receipt.getMessageId()
                    + "] for referenced message with msgId=" + refToMsgId);
        Collection<IMessageUnitEntity> refdMsgs = HolodeckB2BCore.getQueryManager()
                                                                 .getMessageUnitsWithId(refToMsgId);
        if (Utils.isNullOrEmpty(refdMsgs)) {
            // This error SHOULD NOT occur because the reference is already checked when finding the P-Mode
            log.error("Receipt [msgId=" + receipt.getMessageId() + "] contains unknown refToMessageId ["
                        + refToMsgId + "]!");
            updateManager.setProcessingState(receipt, ProcessingState.FAILURE);
            // Create error and add to context
            MessageContextUtils.addGeneratedError(mc, new ValueInconsistent("Receipt contains unknown reference ["
                                                                            + refToMsgId + "]",
                                                                            receipt.getMessageId()));
        } else {
            // The collection can only contain one User Message as messageId of sent message are guaranteed to be unique
            IMessageUnitEntity  ackedMessage = refdMsgs.iterator().next();
            if (ackedMessage instanceof IUserMessage) {
                // Check if the found message unit expects a receipt
                final IPMode pmode = HolodeckB2BCore.getPModeSet().get(ackedMessage.getPModeId());
                if (pmode == null || pmode.getLeg(ackedMessage.getLeg()).getReceiptConfiguration() == null) {
                    // The P-Mode is not configured for receipts, generate error
                    updateManager.setProcessingState(receipt, ProcessingState.FAILURE);
                    // Create error and add to context
                    MessageContextUtils.addGeneratedError(mc,
                                            new ValueInconsistent("P-Mode of referenced message [" + refToMsgId
                                                                  + "] is not configured for receipts",
                                                                  receipt.getMessageId()));
                }  else {
                    // Change to processing state of the reference message unit to delivered, but only if it is
                    // waiting for a receipt as we may otherwise overwrite an error state.
                    if (isWaitingForReceipt(ackedMessage)) {
                        log.debug("Found message unit waiting for Receipt, setting processing state to delivered");
                        updateManager.setProcessingState(ackedMessage, ProcessingState.DELIVERED);
                        // Maybe the Receipt must also be delivered to the business application, so change state
                        // to "ready for delivery"
                        log.debug("Mark Receipt as ready for delivery to business application");
                        updateManager.setProcessingState(receipt, ProcessingState.READY_FOR_DELIVERY);
                    } else {
                        log.debug("Found message unit not waiting for receipt anymore, processing finished.");
                        updateManager.setProcessingState(receipt, ProcessingState.DONE);
                    }
                }
            } else {
                // This Receipt is for a non User Message which is not allowed by the ebMS V3 Specification!
                // => create an error
                log.warn("Receipt [" + receipt.getMessageId() + "] must refer to User Message, but refers to "
                         + MessageUnitUtils.getMessageUnitName(ackedMessage) + "[" + refToMsgId + "]!");
                final ValueInconsistent   viError = new ValueInconsistent();
                viError.setErrorDetail("Receipt does not reference a User Message message unit");
                viError.setRefToMessageInError(receipt.getMessageId());
                MessageContextUtils.addGeneratedError(mc, viError);
                // The message processing of the error fails
                log.debug("Mark Receipt as failed");
                updateManager.setProcessingState(receipt, ProcessingState.FAILURE);
            }
        }
        log.debug("Done processing Receipt");
    }

    /**
     * Checks whether the message unit is waiting for receipt.
     * <p>The message unit is waiting for a receipt when:<ol>
     * <li>its current processing state is {@link ProcessingState#AWAITING_RECEIPT}</li>
     * <li>its current processing state is either {@link ProcessingState#READY_TO_PUSH} or {@link
     * ProcessingState#AWAITING_PULL} and the previous state was {@link ProcessingState#AWAITING_RECEIPT}</li></ol>
     * <p>The check must always include the current state as we do not want to change a processing state that is final.
     *
     * @param mu    The {@link IMessageUnit} to check
     * @return      <code>true</code> if the message unit is waiting for a receipt, <code>false</code> otherwise
     */
    protected boolean isWaitingForReceipt(final IMessageUnit mu) {
        // Check the previous state (there should be, but not guaranteed if receipt is in error)
        List<IMessageUnitProcessingState>   states = mu.getProcessingStates();
        ProcessingState prevState = null, curState = mu.getCurrentProcessingState().getState();
        if (mu.getProcessingStates().size() > 1)
            prevState = states.get(states.size() - 2).getState();

        return curState == ProcessingState.AWAITING_RECEIPT
               || (( curState == ProcessingState.AWAITING_PULL || curState == ProcessingState.READY_TO_PUSH )
                    && prevState == ProcessingState.AWAITING_RECEIPT );
    }
}
