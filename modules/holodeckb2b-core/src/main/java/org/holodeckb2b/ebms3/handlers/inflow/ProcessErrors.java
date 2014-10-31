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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.util.MessageContextUtils;

/**
 * Is the handler responsible for processing received error signals. For each error contained in one of the 
 * {@link ErrorMessage}s available in the message context property {@link MessageContextProperties#IN_ERRORS} it will 
 * check if there is a {@link MessageUnit} in the database and mark that message as failed.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ProcessErrors extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) {
        log.debug("Check for received errors in message.");
        ArrayList<ErrorMessage>  errorSignals = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.IN_ERRORS);
        
        if (errorSignals != null && !errorSignals.isEmpty()) {
            log.debug("Message contains " + errorSignals.size() + " Error signals, start processing");
            for (ErrorMessage e : errorSignals)
                if (!processErrorSignal(e, mc)) {
                    log.warn("Error Signal [msgId=" + e.getMessageId() + "] could not be processed succesfully!");
                }
            log.debug("Error Signals processed");
        } else
            log.debug("Message does not contain error signals, continue processing");

        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Processes an Error signal. 
     * <p>First the referenced message ids are checked for correctness meaning that references must be consistent and
     * refer to existing message units. The consistency is defined in the ebMS Core Specification (section 6.3) as:
     * <i>"If eb:RefToMessageId is present as a child of <code>eb:SignalMessage/eb:MessageInfo</code>, then every 
     * <code>eb:Error</code> element MUST be related to the ebMS message (message-in-error) identified by 
     * <code>eb:RefToMessageId</code>.<br>
     * If the element <code>eb:SignalMessage/eb:MessageInfo</code> does not contain <code>eb:RefToMessageId</code>, then
     * the <code>eb:Error</code> element(s) MUST NOT be related to a particular ebMS message."</i>
     * <p>Holodeck B2B allows a bit more flexibility because the referenced ids in the individual <code>Error</code>
     * elements do not need to be the same when there is no <code>eb:RefToMessageId</code> element in the header.
     * <p>If either the references are inconsistent or refer to non existing message units an <i>ValueInconsistent</i> 
     * error will be generated and added to the message context. The processing state of the error signal itself will 
     * be set to {@link ProcessingStates#FAILURE}.
     * <p>If the referenced ids are valid the message units referenced to will have their processing state changed to 
     * {@link ProcessingStates#FAILURE}. When all referenced message units are checked the processing state of the error 
     * signal itself is changed to {@link ProcessingStates#DONE}.
     *  
     * @param errSignal     The {@link ErrorMessage} to process
     * @param mc            The current message context
     * @return              <code>true</code> if the error signal was processed successfully,<br>
     *                      <code>false</code> otherwise
     */
    protected boolean processErrorSignal(final ErrorMessage errSignal, MessageContext mc) {
        log.debug("Start processing Error [msgId=" + errSignal.getMessageId() + "]");        
        // Change processing state to indicate we start processing the error. Also checks that the error is not
        // already being processed
        try {
            ErrorMessage errorSignal = MessageUnitDAO.startProcessingMessageUnit(errSignal);
            if (errorSignal == null) {
                log.debug("Error [msgId=" + errorSignal.getMessageId() + "] is already being processed, skipping");
                return false;
            }
            
            log.debug("Get referenced message units");
            Set<String>     refMsgIds = getRefdMsgIds(errorSignal, mc);

            log.debug("Check if there exist message units for the referenced ids");
            ArrayList<MessageUnit> refdMsgs = new ArrayList<MessageUnit>();
            boolean refIdsExist = true; 
            if (refMsgIds != null) {
                String refId = null;
                for(Iterator<String> refIds = refMsgIds.iterator() ; refIds.hasNext() && refIdsExist ; ) {
                    refId = refIds.next();
                    Collection<MessageUnit> r = MessageUnitDAO.getSentMessageUnitsWithId(refId);
                    if (r == null || r.isEmpty()) {
                        refIdsExist = false;
                    } else {
                        log.debug("Found " + r.size() + " message units for refId=" + refId);
                        refdMsgs.addAll(r);
                    }
                }
            }

            // If the error signal contains multiple errors it is possible that just one of the errors refers to an 
            // unknown message. In that case the collection of referenced messages may not be empty but the error signal
            // is still invalid
            if (!refIdsExist || refdMsgs.isEmpty()) {
                log.warn("Error signal or error in signal [msgId=" + errorSignal.getMessageId() 
                                + "] contains invalid references!");
                // Create error and add to context
                ValueInconsistent   viError = new ValueInconsistent();
                viError.setErrorDetail("Error contains invalid references (unknown or inconsistent)");
                viError.setRefToMessageInError(errorSignal.getMessageId());
                MessageContextUtils.addGeneratedError(mc, viError);  

                MessageUnitDAO.setFailed(errorSignal);
                return false;
            } else {
                // if the signal references just one message unit, use that message units P-Mode as P-Mode of the signal
                if (refdMsgs.size() == 1)
                    MessageUnitDAO.setPModeId(errSignal, refdMsgs.get(0).getPMode());
                    
                // Change the processing state of the found messages
                for (MessageUnit mu : refdMsgs) {
                    log.debug("Setting processing state of referenced message [" + mu.getMessageId()
                                + "] to failed");
                    MessageUnitDAO.setFailed(mu);
                }
                    
                log.debug("Done processing Error signal [" + errorSignal.getMessageId() + "]");
                // Errors may need to be delivered to bussiness app which can be done now
                MessageUnitDAO.setReadyForDelivery(errorSignal);   
                return true;
            }
        } catch (DatabaseException ex) {
            log.error("An error occurred while changing the processing state of the error signal. Details: " + ex.getMessage());
            return false;
        }
    }
    
    /**
     * Is a helper method to collect all the message ids referenced by the error signal and check their consistency. 
     * When the signal contains a <code>eb:RefToMessageId</code> in the message header, the ids contained in the 
     * <code>refToMessageInError</code> attribute of the separate <code>eb:Error</code> elements must be the same and 
     * their is just one referenced message. Otherwise the set consists of all ids in the <code>refToMessageInError
     * </code> attribute of the separate <code>eb:Error</code> elements. When that set is also empty we check if the 
     * signal is a transport level response to a single message unit and use that id. 
     * 
     * @param errorSignal   The Error signal to collect the referenced message ids from
     * @param mc            The current message context
     * @return              The set of referenced message ids in the Error signal if it is valid, or<br>
     *                      <code>null</code> when the error does not consistently reference one or more message units
     */
    private Set<String> getRefdMsgIds(final ErrorMessage errorSignal, final MessageContext mc) {
        Set<String>     refMsgIds = new HashSet<String>();

        // First get RefToMessageId from header
        String sigRefToMsgId = errorSignal.getRefToMessageId();
        if (sigRefToMsgId != null && !sigRefToMsgId.isEmpty())
            refMsgIds.add(sigRefToMsgId);
        // Then check individual errors    
        for(IEbmsError e : errorSignal.getErrors()) {
            String errRefToMsgId = e.getRefToMessageInError();
            if (errRefToMsgId != null && !errRefToMsgId.isEmpty())
                refMsgIds.add(errRefToMsgId);
        }
        
        // If the RefToMessageId element from header contained a id, all other ids should be the same and the set of ids
        // should just contain one id. If not the references are inconsistent.
        if (sigRefToMsgId != null && !sigRefToMsgId.isEmpty() && refMsgIds.size() > 1)
            return null;
        
        log.debug("The error signal references " + refMsgIds.size() + " message units.");
        
        // If there is no referenced message unit and the error is received as a response to a single message unit
        //  we sent out we still have a reference
        if (refMsgIds.isEmpty() && isInFlow(INITIATOR)) {
            Collection<MessageUnit>  reqMUs = MessageContextUtils.getSentMessageUnits(mc);
            if (reqMUs.size() != 1) {
                log.warn("Error signal [msgId=" + errorSignal.getMessageId() +
                                                "] can not be related to specific message unit!");
                return null;
            } else {
                log.debug("Request contained one message unit, assuming error applies to it");
                refMsgIds.add(reqMUs.iterator().next().getMessageId());
            }
        }   
        
        return refMsgIds;
    }
}
