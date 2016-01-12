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

import java.util.ArrayList;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;

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
        ArrayList<EntityProxy<Receipt>>  receipts = 
                                (ArrayList<EntityProxy<Receipt>>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        
        if (!Utils.isNullOrEmpty(receipts)) {
            log.debug("Message contains " + receipts.size() + " Receipts signals, start processing");
            for (EntityProxy<Receipt> r : receipts)
                // Ignore Receipts that already failed
                if (!ProcessingStates.FAILURE.equals(r.entity.getCurrentProcessingState().getName()))
                    processReceipt(r, mc);                
            log.debug("Receipts processed");
        } else
            log.debug("Message does not contain receipts, continue processing");

        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Processes one Receipt signal. 
     * 
     * @param r     The {@link Receipt} to process
     * @param mc    The message context of the message containing the receipt
     * @throws      DatabaseException When a database error occurs while processing the Receipt Signal
     */
    protected void processReceipt(final EntityProxy<Receipt> rcptProxy, final MessageContext mc) 
                                                                                            throws DatabaseException {
        // Change processing state to indicate we start processing the receipt. Also checks that the receipt is not
        // already being processed
        if (!MessageUnitDAO.startProcessingMessageUnit(rcptProxy)) {
            log.debug("Receipt [msgId=" + rcptProxy.entity.getMessageId() + "] is already being processed, skipping");
            return;
        }
        
        String refToMsgId = rcptProxy.entity.getRefToMessageId();
        log.debug("Start processing Receipt [msgId=" + rcptProxy.entity.getMessageId() 
                    + "] for referenced message with msgId=" + refToMsgId);
        EntityProxy<MessageUnit> refdMsg = MessageUnitDAO.getSentMessageUnitWithId(refToMsgId);

        if (refdMsg == null) {
            // This error SHOULD NOT occur because the reference is already checked when finding the P-Mode 
            log.error("Receipt [msgId=" + rcptProxy.entity.getMessageId() + "] contains unknown refToMessageId ["
                        + refToMsgId + "]!");
            MessageUnitDAO.setFailed(rcptProxy);
            // Create error and add to context
            MessageContextUtils.addGeneratedError(mc, new ValueInconsistent("Receipt contains unknown reference [" 
                                                                            + refToMsgId + "]", 
                                                                            rcptProxy.entity.getMessageId()));  
        } else {
            // Check if the found message unit expects a receipt 
            String pmodeId = refdMsg.entity.getPMode();
            if (pmodeId != null) {
                IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(pmodeId);
                if (pmode == null || pmode.getLegs().iterator().next().getReceiptConfiguration() == null) {
                    // The P-Mode is not configured for receipts, generate error
                    MessageUnitDAO.setFailed(rcptProxy);
                    // Create error and add to context
                    MessageContextUtils.addGeneratedError(mc, 
                                            new ValueInconsistent("P-Mode of referenced message [" + refToMsgId 
                                                                  + "] is not configured for receipts",
                                                                  rcptProxy.entity.getMessageId()));  
                }  else {
                     // Change to processing state of the reference message unit to delivered, but only if it is 
                     // waiting for a receipt as we may otherwise overwrite an error state.
                    if (isWaitingForReceipt(refdMsg.entity)) {
                        log.debug("Found message unit waiting for Receipt, setting processing state to delivered");
                        MessageUnitDAO.setDelivered(refdMsg);                        
                        // Maybe the Receipt must also be delivered to the business application, so change state
                        // to "ready for delivery"
                        log.debug("Mark Receipt as ready for delivery to business application");
                        MessageUnitDAO.setReadyForDelivery(rcptProxy);
                    } else {
                        log.debug("Found message unit not waiting for receipt anymore, processing finished.");
                        MessageUnitDAO.setDone(rcptProxy);
                    }
                }  
            }
        }
        log.debug("Done processing Receipt");                       
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
    protected boolean isWaitingForReceipt(final MessageUnit mu) {
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
