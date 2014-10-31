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
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.ebms3.util.PModeFinder;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for adding <i>Error signals</i> that are waiting to be sent to the 
 * outgoing message.
 * <p>Currently error signals will only be added to messages that initiate a message exchange, i.e. that are sent
 * as a request. This allows for an easy bundling rule as the destination URL can be used as selector. Adding errors
 * to a response would require additional P-Mode configuration as possible bundling options must be indicated.
 * <p>NOTE: There exists some ambiguity in both the ebMS Core Specification and AS4 Profile about bundling of message 
 * units (see issue https://tools.oasis-open.org/issues/browse/EBXMLMSG-50?jql=project%20%3D%20EBXMLMSG). This is also
 * a reason to bundle only if the URL is the same.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class AddErrorSignals extends BaseHandler {

    /**
     * Errors are only added to request messages.
     * 
     * @return Indication that the handler only processes responses (=<code>OUT_FLOW | INITIATOR</code>)
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW | INITIATOR;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) {
        
        log.debug("Check if this message already contains Error signals to send");
        Collection<ErrorMessage> errorSigs = null;
        try {
            errorSigs = (Collection<ErrorMessage>) mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
        } catch(ClassCastException cce) {
            log.fatal("Illegal state of processing! MessageContext contained a " + errorSigs.getClass().getName()
                        + " object as collection of error signals!");
            return InvocationResponse.ABORT;
        }
        
        if(errorSigs != null && !errorSigs.isEmpty()) {
            log.debug("Message already contains Error signals, can not add additional ones");
            return InvocationResponse.CONTINUE;
        }
        
        // Check which other message units are already in the message that errors would be bundled with
        Collection<MessageUnit> bundledMUs = null;
        try {
            log.debug("Get bundled message units");
            bundledMUs = getMessageUnits(mc);
        } catch (IllegalStateException e) {
            // Objects in message context are not of expected type!
            log.fatal(e.getMessage());
            return InvocationResponse.ABORT;
        }
        
        if(bundledMUs.isEmpty()) {
            // This message does not any other message unit => it is not an ebMS message (strange as Holodeck B2B module was engaged)
            log.warn("No ebMS message units in message!");
            return InvocationResponse.CONTINUE;
        }
        
        log.debug("Get errors that can be bundled with current message unit");
        Collection<ErrorMessage> errsToAdd = getBundableErrors(bundledMUs);
        if (errsToAdd == null || errsToAdd.isEmpty()) {
            log.debug("No error signal(s) found to add to the message");
            return InvocationResponse.CONTINUE;
        } else
            log.debug(errsToAdd.size() + " error signal(s) will be added to the message");
        
        // Change the processing state of the errors that are included
        for(ErrorMessage e : errsToAdd) {
            log.debug("Change processing state of error signal [" + e.getMessageId() + "] to indicate it is included");
            try {
                // When processing state is changed add the error to the list of errors to send
                ErrorMessage error = MessageUnitDAO.startProcessingMessageUnit(e);
                if (error != null) {
                    log.debug("Processing state changed for error signal with msgId=" + error.getMessageId());
                    MessageContextUtils.addErrorSignalToSend(mc, error);
                } else
                    log.debug("Could not change processing state for error signal with msgId=" + e.getMessageId() 
                                + ", skipping");                
            } catch (DatabaseException dbe) {
                log.error("An error occurred while changing the processing state of error signal [" 
                            + e.getMessageId() + "]. Details: " + dbe.getMessage());
            }
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
            Collection<Receipt> receipts = (ArrayList<Receipt>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
            if (receipts != null && !receipts.isEmpty()) {
                log.debug("Message contains one or more Receipts signals");
                otherMUs.addAll(receipts);
            }
        } catch (ClassCastException e) {
            throw new IllegalStateException("Illegal state of processing! MessageContext contained an object that was not collection of receipts!");
        }
        
        return otherMUs;
    }
    
    /**
     * Retrieves the {@link ErrorMessage}s waiting to be sent and that can be bundled with the already included message
     * units.
     * <p>An <i>error signal</i> can be included in this message if the URL the error should be sent to equals the 
     * destination URL of the other message units.
     * <p>NOTE: It is assumed that the message units already included in the message have to be sent to the same 
     * URL, so only one message unit is checked.
     * <p>An error signal is waiting to be sent if its processing state is either {@link ProcessingStates#CREATED} or 
     * {@link ProcessingStates#TRANSPORT_FAILURE}.
     * 
     * @param toBeBundledWithMUs
     * @param isResponse 
     * @return 
     */
    private Collection<ErrorMessage> getBundableErrors(Collection<MessageUnit> toBeBundledWithMUs) {
        ArrayList<ErrorMessage> errors = new ArrayList<ErrorMessage>();
        Collection<IPMode>       pmodes = null;
        
        log.debug("Get the destination URL of the message");
        String destURL = null;
        MessageUnit mu = toBeBundledWithMUs.iterator().next();
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(mu.getPMode());

        if (mu instanceof UserMessage || mu instanceof PullRequest) {
            destURL = pmode.getLegs().iterator().next().getProtocol().getAddress();
        } else { // MessageUnit instanceof Receipt
            destURL = pmode.getLegs().iterator().next().getReceiptConfiguration().getTo();
        }

        log.debug("Get P-Modes of errors that can be bundled");
        pmodes = PModeFinder.getPModesWithErrorsTo(destURL);
            
        if(pmodes == null || pmodes.isEmpty()) {
            log.debug("No P-Modes found that allow bundling of errors to this message");
            return null;
        }

        log.debug("Errors from " + pmodes.size() + " P-Modes can be bundled to message.");
        log.debug("Retrieve errors waiting to send");
        try {
            Collection<ErrorMessage> createdErrors = 
                       MessageUnitDAO.getMessageUnitsForPModesInState(ErrorMessage.class, pmodes, ProcessingStates.READY_TO_PUSH);
            if (createdErrors != null && !createdErrors.isEmpty())
                errors.addAll(createdErrors);
            Collection<ErrorMessage> failedErrors = 
                       MessageUnitDAO.getMessageUnitsForPModesInState(ErrorMessage.class, pmodes, ProcessingStates.TRANSPORT_FAILURE);
            if (failedErrors != null && !failedErrors.isEmpty())
                errors.addAll(failedErrors);
        } catch (DatabaseException dbe) {
            log.error("An error occurred while retrieving error signals from the database! Details: " + dbe.getMessage());
            return null;
        }
        
        // As we only allow one error signal in the message we select the oldest one if more are available
        if (errors.size() > 1) {
            log.debug("More than one error available, select oldest as bundling not allowed");
            ErrorMessage oldestError = errors.get(0);
            for(ErrorMessage e : errors)
                if (e.getTimestamp().before(oldestError.getTimestamp()))
                    oldestError = e;
            errors.clear();
            errors.add(oldestError);
        }
        
        return errors;
    }
}
