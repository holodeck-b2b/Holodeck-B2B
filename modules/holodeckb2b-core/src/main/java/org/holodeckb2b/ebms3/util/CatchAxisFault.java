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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.MessageIdGenerator;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.persistent.processing.ProcessingState;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;

/**
 * Is a special handler to handle unexpected and previously unhandled errors. When such errors are detected the 
 * processing state of message units currently being processed will be set to {@link ProcessingStates#FAILURE}. 
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
                log.error("A Fault was raised in the OUT_FLOW while processing outgoing message!" 
                            + " Reported cause= " + mc.getFailureReason().getMessage());
                
                /* 
                    Change the processing state of the message units in this message. For UserMessage message units
                    this will be WAIT_FOR_PULL or READY_TO_PUSH to enable the message a next time. For signals 
                    (PullRequest, Errors and Receipts) this will be FAILURE because they can not be resent.
                
                    Also the message units must be removed from the MessageContext to prevent them from being sent in
                    the OUT_FAULT_FLOW
                */
                Collection<MessageUnit> outMsgUnits = MessageContextUtils.getSentMessageUnits(mc);
                for (MessageUnit mu : outMsgUnits) {
                    // Changing the processing state may fail if the problems are caused by the database. 
                    try {
                        // Check message unit type
                        if (mu instanceof UserMessage) {     
                            if (isInFlow(INITIATOR))
                                mu = MessageUnitDAO.setReadyToPush(mu);
                            else
                                mu = MessageUnitDAO.setWaitForPull(mu);
                            log.warn("UserMessage with msg-id [" + mu.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                        } else { // must be a Signal 
                            log.error(mu.getClass().getSimpleName() + " with msg-id [" + mu.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                            mu = MessageUnitDAO.setFailed(mu);
                        }
                    } catch (DatabaseException ex) {
                        // Unable to change the processing state, log the error. 
                        log.error(mu.getClass().getSimpleName() + " with msg-id [" + mu.getMessageId() 
                                                                    + "] could not be sent due to an internal error.");    
                    }
                }
                
                log.debug("Remove existing outgoing message units from context");
                mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, null);
                mc.setProperty(MessageContextProperties.OUT_RECEIPTS, null);
                mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, null);
                
                // If we are responding to a PullRequest we will sent an ebMS "Other" error to indicate the problem
                if (MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_PULL_REQUEST) != null) {
                    ErrorMessage newErrorMU = null;

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
                        log.fatal("Could not store error signal message in database!");
                        newErrorMU = new ErrorMessage();
                        newErrorMU.addError(otherError);
                        // Generate a message id and timestamp for the new error message unit
                        newErrorMU.setMessageId(MessageIdGenerator.createMessageId());
                        newErrorMU.setTimestamp(new Date());
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
                Collection<MessageUnit> outMsgUnits = MessageContextUtils.getRcvdMessageUnits(mc);
                for (MessageUnit mu : outMsgUnits) {
                    // Changing the processing state may fail if the problems are caused by the database. 
                    try {
                        // Check the current processing state and change to failed if not completely processed
                        ProcessingState procState = mu.getCurrentProcessingState();
                        String procStateName = procState != null ? procState.getName() : null;
                        
                        if (!ProcessingStates.DONE.equals(procStateName) && 
                            !ProcessingStates.DELIVERED.equals(procStateName)) {                                
                            mu = MessageUnitDAO.setFailed(mu);
                            log.error(mu.getClass().getSimpleName() + " with msg-id [" + mu.getMessageId() 
                                                    + "] could not be processed completely due to an internal error.");    
                        }
                    } catch (DatabaseException ex) {
                        // Unable to change the processing state, log the error. 
                        log.error(mu.getClass().getSimpleName() + " with msg-id [" + mu.getMessageId() 
                                                    + "] could not be processed completely due to an internal error.");    
                    }
                }
                
                log.debug("Remove prepared outgoing message units from context");
                mc.setProperty(MessageContextProperties.OUT_RECEIPTS, null);
                mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, null);
            } 
        } 
    }
}
