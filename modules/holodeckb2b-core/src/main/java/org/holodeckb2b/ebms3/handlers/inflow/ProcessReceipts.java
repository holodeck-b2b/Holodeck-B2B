/*
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

import java.util.ArrayList;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.ProcessingModeMismatch;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the handler responsible for processing received receipt signals. For each {@link Receipt} available in the message 
 * context property {@link MessageContextProperties#IN_RECEIPTS} it will check if there is a {@link UserMessage} in the 
 * database waiting for a receipt and mark that message as delivered.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ProcessReceipts extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        log.debug("Check for received receipts in message.");
        ArrayList<Receipt>  receipts = (ArrayList<Receipt>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        
        if (receipts != null && !receipts.isEmpty()) {
            log.debug("Message contains " + receipts.size() + " Receipts signals, start processing");
            for (Receipt r : receipts)
                // Ignore Receipts that already failed
                if (!ProcessingStates.FAILURE.equals(r.getCurrentProcessingState().getName())) {
                    if (!processReceipt(r, mc)) {
                        log.warn("Receipt [msgId=" + r.getMessageId() + "] could not be processed succesfully!");
                    }
                }
            log.debug("Receipts processed");
        } else
            log.debug("Message does not contain receipts, continue processing");

        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Processes a Receipt signal. Checks if there are message units with the referenced message id waiting for a
     * receipt and if so changes their processing state to {@link ProcessingStates#DELIVERED}.<br>
     * When all referenced message units are checked the processing state of the receipt itself is changed to 
     * {@link ProcessingStates#DONE}.
     * 
     * @param r     The {@link Receipt} to process
     * @param mc    The message context of the message containing the receipt
     * @return      <code>true</code> if the receipt signal was processed successfully, <code>false</code> otherwise
     * @throws DatabaseException When a database error occurs while processing the Receipt Signal
     */
    protected boolean processReceipt(final Receipt r, final MessageContext mc) throws DatabaseException {
        String refToMsgId = r.getRefToMessageId();
        
        log.debug("Start processing Receipt [msgId=" + r.getMessageId() 
                    + "] for reference message with msgId=" + refToMsgId);
        
        // Change processing state to indicate we start processing the receipt. Also checks that the receipt is not
        // already being processed
        Receipt rcpt = MessageUnitDAO.startProcessingMessageUnit(r);
        if (rcpt == null) {
            log.debug("Receipt [msgId=" + r.getMessageId() + "] is already being processed, skipping");
            return false;
        } else {
        log.debug("Get referenced message units");
            MessageUnit refdMsg = MessageUnitDAO.getSentMessageUnitWithId(refToMsgId);
            if (refdMsg == null) {
                log.warn("Receipt [msgId=" + r.getMessageId() + "] contains unknown refToMessageId ["
                        + refToMsgId + "]!");
                MessageUnitDAO.setFailed(rcpt);
                // Create error and add to context
                ValueInconsistent   viError = new ValueInconsistent();
                viError.setErrorDetail("Receipt contains unknown message reference [" + refToMsgId + "]");
                viError.setRefToMessageInError(rcpt.getMessageId());
                MessageContextUtils.addGeneratedError(mc, viError);  
                return false;
            } else {
                // Check if the found message unit is waiting for a receipt and change processing state if so
                if (isWaitingForReceipt(refdMsg)) {
                    log.debug("Found message unit waiting for Receipt, setting processing state to delivered");
                    MessageUnitDAO.setDelivered(refdMsg);
                    // Maybe the Receipt must also be delivered to the business application, so change state
                    // to "ready for delivery"
                    log.debug("Mark Receipt as ready for delivery to business application");
                    MessageUnitDAO.setReadyForDelivery(rcpt);
                }  else {
                    // This message is not waiting for receipt, maybe it is already acknowledged? Check P-Mode if
                    // it uses receipts
                    String pmodeId = refdMsg.getPMode();
                    if (pmodeId != null) {
                       if (HolodeckB2BCore.getPModeSet().get(pmodeId).getLegs().iterator().next()
                                .getReceiptConfiguration() == null) {
                           // The P-Mode is not configured for receipts, generate error
                            MessageUnitDAO.setFailed(rcpt);
                            // Create error and add to context
                            ProcessingModeMismatch   pmodeError = new ProcessingModeMismatch();
                            pmodeError.setErrorDetail("Referenced message [" + refToMsgId 
                                                        + "] is not configured for receipts");
                            pmodeError.setRefToMessageInError(rcpt.getMessageId());
                            MessageContextUtils.addGeneratedError(mc, pmodeError);  
                            return false;
                       }  else {
                            // The P-Mode is configured for receipts, so message probably already acked and
                            // receipt delivered (if needed) => nothing to do, processing done
                           log.info("Received Receipt [" + rcpt.getMessageId() + "] for message [" +
                                            refdMsg.getMessageId() + "] not waiting for receipt anymore");
                           MessageUnitDAO.setDone(rcpt);
                       }
                    } // else 
                        // The referenced message unit has no associated P-Mode, unable to determine whether is
                        // was waiting for a receipt, ignore
                }
            }
            log.debug("Done processing Receipt");                       
            return true;
        }
    }
    
    /**
     * Checks whether the message unit is waiting for receipt.
     * <p>The message unit is waiting for a receipt when:<ol>
     * <li>its current processing state is {@link ProcessingStates#AWAITING_RECEIPT}</li>
     * <li>its current processing state is either {@link ProcessingStates#READY_TO_PUSH} or {@link ProcessingStates#AWAITING_PULL}
     * and the previous state was {@link ProcessingStates#AWAITING_RECEIPT}</li></ol>
     * <p>The check must always include the current state as we do not want to change a processing state that is final.
     * 
     * @param mu    The {@link MessageUnit} to check
     * @return      <code>true</code> if the message unit is waiting for a receipt, <code>false</code> otherwise 
     */
    protected boolean isWaitingForReceipt(MessageUnit mu) {
        // Check the previous state (there should be, but not guaranteed if receipt is in error)
        String prevState = null;
        if (mu.getProcessingStates().size() > 1)
            prevState = mu.getProcessingStates().get(1).getName();
        
        return ProcessingStates.AWAITING_RECEIPT.equals(mu.getCurrentProcessingState().getName()) 
            || (
                (  ProcessingStates.AWAITING_PULL.equals(mu.getCurrentProcessingState().getName()) 
                || ProcessingStates.READY_TO_PUSH.equals(mu.getCurrentProcessingState().getName())
                ) && ProcessingStates.AWAITING_RECEIPT.equals(prevState)
               );
    }
}
