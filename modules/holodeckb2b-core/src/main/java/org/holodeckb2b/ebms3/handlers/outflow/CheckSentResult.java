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

import java.util.ArrayList;
import java.util.Collection;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.SignalMessage;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for changing the processing state of message units that were sent out in
 * the current SOAP message. 
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
        Collection<MessageUnit> msgUnits = MessageContextUtils.getSentMessageUnits(mc);
        // And change their processing state
        for (MessageUnit mu : msgUnits) {
            MessageUnitDAO.setSending(mu);
            log.info(mu.getClass().getSimpleName()+ " with msg-id [" + mu.getMessageId() + "] is being sent");
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
            
            // Collect all signals as they are processed uniformly
            Collection<SignalMessage>   signals = collectSignals(mc);
            changeSignalMessageState(signals, success);
            
            // Process the User message units contained in the message
            Collection<UserMessage> usermsgs = new ArrayList<UserMessage>();
            UserMessage um = (UserMessage) mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE);
            if (um != null)
                usermsgs.add(um);
            changeUserMessageState(usermsgs, success);
            
            // The transport error is handled, so clear the failure to prevent further processing of the error
            mc.setFailureReason(null);
        } // else 
            // Not an ebMS message, nothing to do
    }

    /**
     * Gets all the <i>Signal</i> message units contained in the current message.
     * 
     * @param mc        The {@link MessageContext} of the current message
     * @return          Collection of {@link SignalMessage} objects representing all Signal message units contained
     *                  in the message
     */
    protected Collection<SignalMessage> collectSignals(MessageContext mc) {
        Collection<SignalMessage> result = new ArrayList<SignalMessage>();
        Collection<SignalMessage> signalInMC = null;
        
        // There can only be a single PullRequest
        PullRequest pullRequest = (PullRequest) mc.getProperty(MessageContextProperties.OUT_PULL_REQUEST);
        if (pullRequest != null)
            result.add(pullRequest);
        signalInMC = (Collection<SignalMessage>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
        if (signalInMC != null && !signalInMC.isEmpty())
            result.addAll(signalInMC);
        signalInMC = (Collection<SignalMessage>) mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
        if (signalInMC != null && !signalInMC.isEmpty())
            result.addAll(signalInMC);
        
        return result;
    }
    
    /**
     * Changes the processing state of the Signal message units accordingly to outcome of the sent operation:<br>
     * {@link ProcessingStates#DELIVERED} when the operation was successful, {@link ProcessingStates#TRANSPORT_FAILURE}.
     * 
     * @param signals   The collection of signal messages contained in the sent message
     * @param success   Indication whether the sent operation was successful
     */
    protected void changeSignalMessageState(final Collection<SignalMessage> signals, final boolean success) {
        log.debug("The processing state to set is " 
                        + (success ? ProcessingStates.DELIVERED : ProcessingStates.TRANSPORT_FAILURE));
        
        // Set processing state of each signal
        for(SignalMessage s : signals) {
           log.debug("Setting processing state for signal message with msgId=" + s.getMessageId());
            try {
                if (success) {
                    MessageUnitDAO.setDelivered(s);
                } else {
                    //@todo: Should we do a retry? 
                    MessageUnitDAO.setTransportFailure(s);
                }
                log.debug("Processing state of signal message [" + s.getMessageId() + "] changed");
            } catch (DatabaseException databaseException) {
                // Ai, something went wrong updating the processing state of the signal. As the signal is already
                // processed there is nothing we can other than logging the error
                log.error("A database error occurred while update the processing state of signal message ["
                            + s.getMessageId() + "]. Details: " + databaseException.getMessage());
            }
        }
    }

    /**
     * Changes the processing state of the User message units accordingly to outcome of the sent operation and the
     * P-Mode configuration for receipts:<br>
     * {@link ProcessingStates#DELIVERED} when the operation was successful, {@link ProcessingStates#TRANSPORT_FAILURE}.
     * 
     * @param usermsgs  The collection of user messages contained in the sent message
     * @param success   Indication whether the sent operation was successful
     */
    protected void changeUserMessageState(final Collection<UserMessage> usermsgs, final boolean success) {
        
        // Set processing state of each signal
        for(UserMessage um : usermsgs) {
           log.debug("Setting processing state for signal message with msgId=" + um.getMessageId());
            try {
                if (!success) {
                    //@todo: Should we do a specific retry for this? Now done using Reception Awareness retry
                    MessageUnitDAO.setTransportFailure(um);
                } else {
                    log.debug("User message is sent, check P-Mode if Receipt is expected");
                    // Because we only support One-Way the first leg determines
                    ILeg leg = HolodeckB2BCoreInterface.getPModeSet().get(um.getPMode()).getLegs().iterator().next();
                    if (leg.getReceiptConfiguration() != null)
                        MessageUnitDAO.setWaitForReceipt(um);
                    else
                        MessageUnitDAO.setDelivered(um);
                }
                log.debug("Processing state of user message [" + um.getMessageId() + "] changed to " 
                            + um.getCurrentProcessingState().getName());
            } catch (DatabaseException databaseException) {
                // Ai, something went wrong updating the processing state of the user message. As the message unit is already
                // processed there is nothing we can other than logging the error
                log.error("A database error occurred while update the processing state of user message ["
                            + um.getMessageId() + "]. Details: " + databaseException.getMessage());
            }
        }
    }
    
}