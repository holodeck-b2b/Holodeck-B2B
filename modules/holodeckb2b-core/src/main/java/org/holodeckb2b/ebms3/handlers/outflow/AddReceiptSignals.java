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
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.util.PModeFinder;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for adding <i>Receipt signals</i> that are waiting to be sent to the 
 * outgoing message.
 * <p>Currently receipt signals will only be added to messages that initiate a message exchange, i.e. that are sent
 * as a request. This allows for an easy bundling rule as the destination URL can be used as selector. Adding receipts
 * to a response would require additional P-Mode configuration as possible bundling options must be indicated.
 * <p>NOTE: There exists some ambiguity in both the ebMS Core Specification and AS4 Profile about bundling of message 
 * units (see issue https://tools.oasis-open.org/issues/browse/EBXMLMSG-50?jql=project%20%3D%20EBXMLMSG). This is also
 * a reason to bundle only if the URL is the same.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class AddReceiptSignals extends BaseHandler {

    /**
     * Response message are ignored as receipts are not added to responses
     * 
     * @return Indication that the handler only runs in the response out flow, i.e. <code>OUT_FLOW | INITIATOR </code>
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW | INITIATOR;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        
        log.debug("Check if this message already contains Receipt signals to send");
        Collection<Receipt> rcptSigs = null;
        try {
            rcptSigs = (Collection<Receipt>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
        } catch(ClassCastException cce) {
            log.fatal("Illegal state of processing! MessageContext contained a " + rcptSigs.getClass().getName() + " object as collection of error signals!");
            return InvocationResponse.ABORT;
        }
        
        if(rcptSigs != null && !rcptSigs.isEmpty()) {
            log.debug("Message already contains Receipt signals, can not add additional ones");
            return InvocationResponse.CONTINUE;
        }
        
        // Check which other message units are already in the message that receipts would be bundled with
        Collection<MessageUnit> bundledMUs = null;
        try {
            log.debug("Get bundled message units");
            bundledMUs = getMessageUnits(mc);
        } catch (IllegalStateException e) {
            // Objects in message context are not of expected type!
            log.fatal(e.getMessage());
            return InvocationResponse.ABORT;
        }
        
        if (bundledMUs.isEmpty()) {
            // This message does not any other message unit => it is not an ebMS message (strange as Holodeck B2B module was engaged)
            log.warn("No ebMS message units in message!");
            return InvocationResponse.CONTINUE;
        }
        
        log.debug("Get receipts that can be bundled with current message unit");
        Collection<Receipt> rcptsToAdd = getBundableReceipts(bundledMUs);        
        if (rcptsToAdd == null || rcptsToAdd.isEmpty()) {
            log.debug("No receipt signal(s) found to add to the message");
            return InvocationResponse.CONTINUE;
        } else
            log.debug(rcptsToAdd.size() + " receipt signal(s) will be added to the message");
        
        // Change the processing state of the rcpts that are included
        for(Receipt r : rcptsToAdd) {
            log.debug("Change processing state of receipt signal [" + r.getMessageId() + "] to indicate it is included");
            // When processing state is changed add the receipt to the list of receipts to send
            Receipt rcpt = MessageUnitDAO.startProcessingMessageUnit(r);
            if (rcpt != null) {
                log.debug("Processing state changed for receipt signal with msgId=" + r.getMessageId());
                MessageContextUtils.addReceiptToSend(mc, rcpt);
            } else
                log.debug("Could not change processing state for receipt signal with msgId=" + r.getMessageId() 
                            + ", skipping");                
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Retrieves all other types of message units already in this message. 
     * 
     * @param mc    The message context
     * @return      {@link Collection} of {@link MessageUnit} objects for the message units already in the message. 
     */
    private Collection<MessageUnit> getMessageUnits(MessageContext mc) {
        Collection<MessageUnit>   otherMUs = new ArrayList<MessageUnit>();
        
        log.debug("Check if message contains an User Message");
        try {
            UserMessage userMsg = (UserMessage) mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE);
            if (userMsg != null) {
                log.debug("Message contains an User Message");
                otherMUs.add(userMsg);
            }
        } catch (ClassCastException cce) {
            throw new IllegalStateException("Illegal state of processing! MessageContext contained another object as UserMessage!");
        }
        
        log.debug("Check if message already contains another signal message");
        try {
            PullRequest pullReq = (PullRequest) mc.getProperty(MessageContextProperties.OUT_PULL_REQUEST);
            if (pullReq != null) {
                log.debug("Message contains a PullRequest");
                otherMUs.add(pullReq);
            }
        } catch (ClassCastException cce) {
            throw new IllegalStateException("Illegal state of processing! MessageContext contained another object as PullRequest!");
        }
        try {
            Collection<ErrorMessage> errs = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
            if (errs != null && !errs.isEmpty()) {
                log.debug("Message contains one or more Error signals");
                otherMUs.addAll(errs);
            }
        } catch (ClassCastException e) {
            throw new IllegalStateException("Illegal state of processing! MessageContext contained an object that was not collection of error!");
        }
        
        return otherMUs;
    }
    
    /**
     * Retrieves the {@link Receipt}s waiting to be sent and that can be bundled with the already included message
     * units.
     * <p>An <i>receipt signal</i> can be included in this message if the URL the receipt should be sent to equals the 
     * destination URL of the other message units.
     * <p>NOTE: It is assumed that the message units already included in the message have to be sent to the same 
     * URL, so only one message unit is checked.
     * <p>An receipt signal is waiting to be sent if its processing state is either {@link ProcessingStates#CREATED} or 
     * {@link ProcessingStates#TRANSPORT_FAILURE}.
     * 
     * @param toBeBundledWithMUs
     * @return 
     */
    private Collection<Receipt> getBundableReceipts(Collection<MessageUnit> toBeBundledWithMUs) {
        ArrayList<Receipt> rcpts = new ArrayList<Receipt>();
        Collection<IPMode> pmodes = null;
        
        log.debug("Get the destination URL of the message");
        String destURL = null;
        MessageUnit mu = toBeBundledWithMUs.iterator().next();
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(mu.getPMode());

        if (mu instanceof UserMessage || mu instanceof PullRequest) {
            destURL = pmode.getLegs().iterator().next().getProtocol().getAddress();
        } else { // MessageUnit instanceof ErrorMessage
            //@todo: Should use IPMode operations! PMode.Leg[1].ReceiverErrorsTo but depends on message that was in error!
        }

        log.debug("Get P-Modes of receipts that can be bundled");
        pmodes = PModeFinder.getPModesWithReceiptsTo(destURL);
            
        if(pmodes == null || pmodes.isEmpty()) { 
            log.debug("No P-Modes found that allow bundling of receipts to this message");
            return null;
        }

        log.debug("Receipts from " + pmodes.size() + " P-Modes can be bundled to message.");
        log.debug("Retrieve receipts waiting to send");
        try {
            Collection<Receipt> createdReceipts = 
                       MessageUnitDAO.getMessageUnitsForPModesInState(Receipt.class, pmodes, ProcessingStates.CREATED);
            if (createdReceipts != null && !createdReceipts.isEmpty())
                rcpts.addAll(createdReceipts);
            Collection<Receipt> failedReceipts = 
                       MessageUnitDAO.getMessageUnitsForPModesInState(Receipt.class, pmodes, ProcessingStates.TRANSPORT_FAILURE);
            if (failedReceipts != null && !failedReceipts.isEmpty())
                rcpts.addAll(failedReceipts);
        } catch (DatabaseException dbe) {
            log.error("An error occurred while retrieving receipts signals from the database! Details: " + dbe.getMessage());
            return null;
        }
        
        // As we only allow one error signal in the message we select the oldest one if more are available
        if (rcpts.size() > 1) {
            log.debug("More than one receipt available, select oldest as bundling not allowed");
            Receipt oldestRcpt = rcpts.get(0);
            for(Receipt e : rcpts)
                if (e.getTimestamp().before(oldestRcpt.getTimestamp()))
                    oldestRcpt = e;
            rcpts.clear();
            rcpts.add(oldestRcpt);
        }
        
        return rcpts;
    }
}
