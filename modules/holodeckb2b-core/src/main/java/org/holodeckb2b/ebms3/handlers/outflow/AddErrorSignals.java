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
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistency.entities.PullRequest;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.util.PModeFinder;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.IPMode;

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
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        
        log.debug("Check if this message already contains Error signals to send");
        Collection<EntityProxy<ErrorMessage>> errorSigs = null;
        try {
            errorSigs = (Collection<EntityProxy<ErrorMessage>>) mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
        } catch(ClassCastException cce) {
            log.fatal("Illegal state of processing! MessageContext contained a " + errorSigs.getClass().getName()
                        + " object as collection of error signals!");
            return InvocationResponse.ABORT;
        }
        
        if(!Utils.isNullOrEmpty(errorSigs)) {
            log.debug("Message already contains Error signals, can not add additional ones");
            return InvocationResponse.CONTINUE;
        }
        
        // Whether Errors can be bundled is determined by the primary message unit
        log.debug("Get primary message unit already in the message");
        EntityProxy<MessageUnit> primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc);
        
        if(primaryMU == null) {
            // This message does not have any other message unit 
            // => it is not an ebMS message (strange as Holodeck B2B module was engaged)
            log.warn("No ebMS message units in message!");
            return InvocationResponse.CONTINUE;
        }
        
        log.debug("Get errors that can be bundled with current message unit");
        Collection<EntityProxy<ErrorMessage>> errsToAdd = getBundableErrors(primaryMU);
        if (Utils.isNullOrEmpty(errsToAdd)) {
            log.debug("No error signal(s) found to add to the message");
            return InvocationResponse.CONTINUE;
        } else
            log.debug(errsToAdd.size() + " error signal(s) will be added to the message");
        
        // Change the processing state of the errors that are included
        for(EntityProxy<ErrorMessage> e : errsToAdd) {
            log.debug("Change processing state of error signal [" + e.entity.getMessageId() 
                                                                                    + "] to indicate it is included");
            // When processing state is changed add the error to the list of errors to send
            if (MessageUnitDAO.startProcessingMessageUnit(e)) {
                log.debug("Processing state changed for error signal with msgId=" + e.entity.getMessageId());
                MessageContextUtils.addErrorSignalToSend(mc, e);
            } 
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Retrieves the {@link ErrorMessage}s waiting to be sent and that can be bundled with the already included message
     * units.
     * <p>An <i>error signal</i> can be included in this message if the URL the error should be sent to equals the 
     * destination URL of the <i>primary</i> message unit.
     * <p>An error signal is waiting to be sent if its processing state is either {@link ProcessingStates#CREATED} or 
     * {@link ProcessingStates#TRANSPORT_FAILURE}.
     * 
     * @param primaryMU     The primary message unit contained in the message
     * @return              A collection of Error signal that can be bundled with the given primary message unit
     */
    private Collection<EntityProxy<ErrorMessage>> getBundableErrors(EntityProxy<MessageUnit> primaryMU) {
        ArrayList<EntityProxy<ErrorMessage>> errors = new ArrayList<EntityProxy<ErrorMessage>>();
        Collection<IPMode>       pmodes = null;
        
        log.debug("Get the destination URL of the message");
        String destURL = null;
        IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(primaryMU.entity.getPMode());

        if (primaryMU.entity instanceof UserMessage ||  primaryMU.entity instanceof PullRequest) {
            destURL = pmode.getLegs().iterator().next().getProtocol().getAddress();
        } else { // MessageUnit instanceof Receipt
            destURL = pmode.getLegs().iterator().next().getReceiptConfiguration().getTo();
        }

        log.debug("Get P-Modes of errors that can be bundled");
        pmodes = PModeFinder.getPModesWithErrorsTo(destURL);
            
        if(Utils.isNullOrEmpty(pmodes)) {
            log.debug("No P-Modes found that allow bundling of errors to this message");
            return null;
        }

        log.debug("Errors from " + pmodes.size() + " P-Modes can be bundled to message.");
        log.debug("Retrieve errors waiting to send");
        try {
            Collection<EntityProxy<ErrorMessage>> createdErrors = 
                                         MessageUnitDAO.getMessageUnitsForPModesInState(ErrorMessage.class, 
                                                                                        pmodes, 
                                                                                        ProcessingStates.READY_TO_PUSH);
            if (!Utils.isNullOrEmpty(createdErrors))
                errors.addAll(createdErrors);
            Collection<EntityProxy<ErrorMessage>> failedErrors = 
                                    MessageUnitDAO.getMessageUnitsForPModesInState(ErrorMessage.class, 
                                                                                   pmodes, 
                                                                                   ProcessingStates.TRANSPORT_FAILURE);
            if (!Utils.isNullOrEmpty(failedErrors))
                errors.addAll(failedErrors);
        } catch (DatabaseException dbe) {
            log.error("An error occurred while retrieving error signals from the database! Details: " + dbe.getMessage());
            return null;
        }
        
        // As we only allow one error signal in the message we select the oldest one if more are available
        if (errors.size() > 1) {
            log.debug("More than one error available, select oldest as bundling not allowed");
            EntityProxy<ErrorMessage> oldestError = errors.get(0);
            for(EntityProxy<ErrorMessage> e : errors)
                if (e.entity.getTimestamp().before(oldestError.entity.getTimestamp()))
                    oldestError = e;
            errors.clear();
            errors.add(oldestError);
        }
        
        return errors;
    }
}
