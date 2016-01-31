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
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.ebms.axis2.MessageContextUtils;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistency.entities.SignalMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ILeg;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for changing the processing state of message units that are and have been
 * sent out in the current SOAP message.
 * <p>When the handler is executed in the flow the processing state of all message units contained in the message is
 * set to {@link ProcessingStates#SENDING}. When {@link #flowComplete(org.apache.axis2.context.MessageContext)} is 
 * executed the handler checks if the sent operation was successful and changes the processing state accordingly to
 * either {@link ProcessingStates#TRANSPORT_FAILURE} or {@link ProcessingStates#DELIVERED} / 
 * {@link ProcessingStates#AWAITING_RECEIPT} (for User Message that should be acknowledged through a Receipt).
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
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
     * This method changes the processing state of the message units to {@link ProcessingStates#SENDING} to indicate
     * they are being sent to the other MSH. 
     * 
     * @param mc            The current message 
     * @return              {@link InvocationResponse#CONTINUE} as this handler only needs to run when the message has
     *                      been sent.
     * @throws DatabaseException    When the processing state can not be changed
     */
    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        // Get all message units in this message
        Collection<EntityProxy> msgUnits = MessageContextUtils.getSentMessageUnits(mc);
        // And change their processing state
        for (EntityProxy mu : msgUnits) {
            MessageUnitDAO.setSending(mu);
            log.info(mu.entity.getClass().getSimpleName() + " with msg-id [" 
                                                                    + mu.entity.getMessageId() + "] is being sent");
        }        
        
        return InvocationResponse.CONTINUE;
    }
 
    /**
     * This method is called after the message was sent out. Depending on the result (success or fault) the processing
     * state of the message units contained in the message is changed. When a fault occurred during sending the state
     * is set to {@link ProcessingStates#TRANSPORT_FAILURE}. When the SOAP message is sent out successfully the change
     * depends on the type of message unit:<ul>
     * <li><i>Signal message units</i> : the processing state will be changed to {@link ProcessingStates#DELIVERED}</li>
     * <li><i>User message unit</i> : the new processing state depends on whether a receipt is expected. If a receipt
     *  is expected, the new state will be {@link ProcessingStates#AWAITING_RECEIPT}, otherwise it will be 
     *  {@link ProcessingStates#DELIVERED}.</li></ul>
     * 
     * @param mc    The current message that was sent out
     */
    @Override
    public void doFlowComplete(MessageContext mc) {
        // First get the ebMS header block, that is the eb:Messaging element
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        
        if (messaging != null) {
            log.debug("Check result of sent operation");
            boolean   success = (mc.getFailureReason() == null);
            log.debug("The sent operation was " + (success ? "" : "not ") + "successfull");
            
            //Change processing state of all message units in the message accordingly
            Collection<EntityProxy> msgUnits = MessageContextUtils.getSentMessageUnits(mc);
            
            for (EntityProxy mu : msgUnits) {
                try {
                    if (!success) {
                        MessageUnitDAO.setTransportFailure(mu);
                    } else {
                        // State to set depends on type of message unit
                        if (mu.entity instanceof SignalMessage) {
                            // Signals are always set to delivered
                            MessageUnitDAO.setDelivered(mu);
                        } else {
                            log.debug("User Message is sent, check P-Mode if Receipt is expected");
                            // Because we only support One-Way the first leg determines
                            ILeg leg = HolodeckB2BCoreInterface.getPModeSet().get(mu.entity.getPMode()).getLegs()
                                            .iterator().next();
                            if (leg.getReceiptConfiguration() != null)
                                MessageUnitDAO.setWaitForReceipt(mu);
                            else
                                MessageUnitDAO.setDelivered(mu);                        
                        }
                    }
                    log.debug("Processing state for message unit [" + mu.entity.getMessageId() + "] changed to" 
                                + mu.entity.getCurrentProcessingState().getName());
                } catch (DatabaseException databaseException) {
                    // Ai, something went wrong updating the processing state of the message unit. As the message unit 
                    // is already processed there is nothing we can other than logging the error
                    log.error("A database error occurred while update the processing state of message unit ["
                                + mu.entity.getMessageId() + "]. Details: " + databaseException.getMessage());                    
                }
            }
        }
    }
}