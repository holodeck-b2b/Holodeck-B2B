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
package org.holodeckb2b.ebms3.util;

import java.util.Collections;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.persistency.entities.EbmsError;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;

/**
 * Is a special handler to handle unexpected and previously unhandled errors. When such errors are detected the 
 * processing state of message units currently being processed will be changed to either indicate failed processing. 
 * This means that the processing state of all message units in the current flow are set to <i>FAILURE</i>. 
 * <p>When the error occurs while processing a request or creating a response to a PullRequest an ebMS <i>Other</i> 
 * error is generated and reported to the sender of the request. No other message unit is included in the response to 
 * the sender.<br>
 * Because error reporting to the <i>Producer</i> of a message is current not supported the error is only logged when
 * it occurs when processing an outgoing request.
 * <p>Note that this is a kind of "last resort" error handler and therefore is not supposed to handle normal errors that
 * can occur in the processing of ebMS messages. These errors should all result in an ebMS error and handled 
 * accordingly.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CatchAxisFault extends BaseHandler {
    
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW | OUT_FLOW | RESPONDER;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws AxisFault {
        return InvocationResponse.CONTINUE;
    }
    
    @Override
    public void doFlowComplete(MessageContext mc) {        
        // This handler only needs to act when there was an unexpected failure
        if (mc.getFailureReason() != null) {            
            if (isInFlow(OUT_FLOW)) {
                log.error("A Fault was raised while processing messages!" 
                            + " Reported cause= " + mc.getFailureReason().getMessage());
                /*
                    Change the processing state of the message units to FAILED unless the message unit is already 
                    processed completely, i.e. its processing state is DELIVERED or WAITING_FOR_RECEIPT.
                */
                for (EntityProxy mu : MessageContextUtils.getSentMessageUnits(mc)) {
                    // Changing the processing state may fail if the problems are caused by the database. 
                    try {
                        String curState = mu.entity.getCurrentProcessingState().getName();                        
                        if (!ProcessingStates.DELIVERED.equals(curState) 
                           && !ProcessingStates.AWAITING_RECEIPT.equals(curState)) {
                            log.error(mu.entity.getClass().getSimpleName() + " with msg-id [" + mu.entity.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                            MessageUnitDAO.setFailed(mu);
                        }
                    } catch (DatabaseException ex) {
                        // Unable to change the processing state, log the error. 
                        log.error(mu.entity.getClass().getSimpleName() + " with msg-id [" + mu.entity.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                    }
                }                
                log.debug("Remove existing outgoing message units from context");
                mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, null);
                mc.setProperty(MessageContextProperties.OUT_RECEIPTS, null);
                mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, null);
                
                // If we are responding to a PullRequest we will sent an ebMS "Other" error to indicate the problem
                if (MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_PULL_REQUEST) != null) {
                    EntityProxy<ErrorMessage> newErrorMU = null;

                    OtherContentError   otherError = new OtherContentError();
                    otherError.setErrorDetail("An internal error occurred while processing the message.");
                    otherError.setSeverity(IEbmsError.Severity.WARNING);
                    try {
                        log.debug("Create the Error signal message");
                        newErrorMU = MessageUnitDAO.createOutgoingErrorMessageUnit(
                                                                    Collections.singletonList((EbmsError) otherError), 
                                                                    null, null, true, true);
                    } catch (DatabaseException dbe) {
                        // (Still) a problem with the database, create the Error signal message without storing it 
                        log.fatal("Could not store error signal message in database! Details: " + dbe.getMessage());
                        log.debug("Create non persisted ErrorMessage");
                        newErrorMU = MessageUnitDAO.createTransientOtherError(otherError);                        
                    }
                    log.debug("Created a new Error signal message");                
                    mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, Collections.singletonList(newErrorMU));
                    log.debug("Set the Error signal as the only ebMS message to return");                    
                    
                    // Remove the error condition from the context as we handled the error here
                    mc.setFailureReason(null);
                }
            } else if (isInFlow(IN_FLOW)) {
                log.error("A Fault was raised in the IN_FLOW while processing a request!" 
                            + " Reported cause= " + mc.getFailureReason().getMessage());                
                /*
                    Change the processing state of the message units in this request to FAILED unless the message unit
                    is already processed completely, i.e. its processing state is DONE or DELIVERED.
                
                    There may already be message units created for the response. Although they maybe could be sent in 
                    the OUT_FAULT_FLOW the processing state of these message units are also changed to FAILED.                 
                */
                for (EntityProxy mu : MessageContextUtils.getRcvdMessageUnits(mc)) {
                    // Changing the processing state may fail if the problems are caused by the database. 
                    try {
                        String curState = mu.entity.getCurrentProcessingState().getName();                        
                        if (!ProcessingStates.DELIVERED.equals(curState) 
                           && !ProcessingStates.AWAITING_RECEIPT.equals(curState)) {
                            log.error(mu.entity.getClass().getSimpleName() + " with msg-id [" + mu.entity.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                            MessageUnitDAO.setFailed(mu);
                        }
                    } catch (DatabaseException ex) {
                        // Unable to change the processing state, log the error. 
                        log.error(mu.entity.getClass().getSimpleName() + " with msg-id [" + mu.entity.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                    }                    
                }
                log.debug("Remove prepared outgoing message units from context");
                mc.setProperty(MessageContextProperties.OUT_RECEIPTS, null);
                mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, null);

                // If we are responding to a equest we will sent an ebMS "Other" error to indicate the problem
                if (isInFlow(RESPONDER)) {
                    EntityProxy<ErrorMessage> newErrorMU = null;
                    OtherContentError   otherError = new OtherContentError();
                    otherError.setErrorDetail("An internal error occurred while processing the message.");
                    otherError.setSeverity(IEbmsError.Severity.WARNING);
                    try {
                        log.debug("Create the Error signal message");
                        newErrorMU = MessageUnitDAO.createOutgoingErrorMessageUnit(
                                                                    Collections.singletonList((EbmsError) otherError), 
                                                                    null, null, true, true);
                    } catch (DatabaseException dbe) {
                        // (Still) a problem with the database, create the Error signal message without storing it 
                        log.fatal("Could not store error signal message in database! Details: " + dbe.getMessage());
                        log.debug("Create non persisted ErrorMessage");
                        newErrorMU = MessageUnitDAO.createTransientOtherError(otherError);                        
                    }
                    log.debug("Created a new Error signal message");                
                    mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, Collections.singletonList(newErrorMU));
                    log.debug("Set the Error signal as the only ebMS message to return");                    
                    
                    // Remove the error condition from the context as we handled the error here
                    mc.setFailureReason(null);
                }
            } 
        } 
    }
}
