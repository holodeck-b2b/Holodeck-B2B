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
import java.util.Iterator;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Is the handler responsible for processing received error signals. For each error contained in one of the 
 * {@link ErrorMessage}s available in the message context property {@link MessageContextProperties#IN_ERRORS} it will 
 * check if there is a {@link MessageUnit} in the database and mark that message as failed.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ProcessErrors extends BaseHandler {

    /**
     * Errors will always be logged to a special error log. Using the logging configuration users can decide if this 
     * logging should be enabled and how errors should be logged.
     */
    private Log     errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.received");

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        log.debug("Check for received errors in message.");
        ArrayList<EntityProxy<ErrorMessage>>  errorSignals = 
                              (ArrayList<EntityProxy<ErrorMessage>>) mc.getProperty(MessageContextProperties.IN_ERRORS);
        
        if (!Utils.isNullOrEmpty(errorSignals)) {
            log.debug("Message contains " + errorSignals.size() + " Error signals, start processing");
            for (EntityProxy<ErrorMessage> e : errorSignals)
                // Ignore Errors that already failed
                if (!ProcessingStates.FAILURE.equals(e.entity.getCurrentProcessingState().getName())) {
                    processErrorSignal(e, mc);
                }
            log.debug("Error Signals processed");
        } else
            log.debug("Message does not contain error signals, continue processing");

        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Processes an Error signal. 
     * <p>First the referenced message id is checked for correctness meaning that it refers to an existing message unit.
     * <p>If it refers to non existing message units an <i>ValueInconsistent</i> error will be generated and added to 
     * the message context. The processing state of the error signal itself will be set to 
     * {@link ProcessingStates#FAILURE}.
     * <p>If the referenced id is valid the referenced message units processing state will be changed to {@link
     * ProcessingStates#FAILURE}. The processing state of the error signal itself is set to {@link 
     * ProcessingStates#READY_FOR_DELIVERY} to indicate that the error can be delivered to the business application if
     * needed.
     *  
     * @param errSignalProxy    The {@link EntityProxy} containing the {@link ErrorMessage} to process
     * @param mc                The current message context
     * @throws DatabaseException When a database error occurs while processing the Error Signal
     */
    protected void processErrorSignal(final EntityProxy<ErrorMessage> errSignalProxy, MessageContext mc) 
                                                                                        throws DatabaseException {
        log.debug("Start processing Error [msgId=" + errSignalProxy.entity.getMessageId() + "]");        
        // Change processing state to indicate we start processing the error. Also checks that the error is not
        // already being processed
        if (!MessageUnitDAO.startProcessingMessageUnit(errSignalProxy)) {
            log.debug("Error [msgId=" + errSignalProxy.entity.getMessageId() 
                                                                           + "] is already being processed, skipping");
            return;
        }

        // Always log the error signal, even if its processing may fail later
        errorLog.error(errSignalProxy.entity);                

        log.debug("Get referenced message unit(s)");
        ArrayList<EntityProxy> refdMessages = new ArrayList<>(1);
        
        // There may not be a refToMessageId in the error itself, in that case the message units from the request are
        // assumed to be referenced 
        String refToMessageId = errSignalProxy.entity.getRefToMessageId();
        if (!Utils.isNullOrEmpty(refToMessageId)) {
            log.debug("Error message [" + errSignalProxy.entity.getMessageId() + "] references messageId: "
                        + refToMessageId);
            EntityProxy mu = MessageUnitDAO.getSentMessageUnitWithId(refToMessageId);
            if (mu != null)
                refdMessages.add(mu);                        
        } else if (isInFlow(INITIATOR)) {
            log.warn("Error message [" + errSignalProxy.entity.getMessageId() + "] does not contain reference." 
                    + "Assuming it refers to sent messages");
            refdMessages.addAll(MessageContextUtils.getSentMessageUnits(mc));            
        }
        
        // An error should reference a message unit  
        if (refdMessages.isEmpty()) {
            log.warn("Error message [" + errSignalProxy.entity.getMessageId() 
                                                                       + "] does not reference a known message unit!");
            // Create error and add to context
            ValueInconsistent   viError = new ValueInconsistent();
            viError.setErrorDetail("Error does not reference a sent message unit");
            viError.setRefToMessageInError(errSignalProxy.entity.getMessageId());
            MessageContextUtils.addGeneratedError(mc, viError);  
            // The message processing of the error fails
            MessageUnitDAO.setFailed(errSignalProxy);
        } else {
            // Change the processing state of the found message unit(s)
            for (EntityProxy mu : refdMessages) {
                if (isWarning(errSignalProxy.entity)) {
                    log.debug("Error level is warning, set processing state of referenced message [" 
                                                                    + mu.entity.getMessageId() + "] to warning");
                    MessageUnitDAO.setWarning(mu);
                } else {
                    log.debug("Error level is warning, set processing state of referenced message [" 
                                                                    + mu.entity.getMessageId() + "] to failure");
                    MessageUnitDAO.setFailed(mu);
                }
            }
            log.debug("Done processing Error signal [" + errSignalProxy.entity.getMessageId() + "]");
            // Errors may need to be delivered to bussiness app which can be done now
            MessageUnitDAO.setReadyForDelivery(errSignalProxy);   
        }
    }
    
    
    
    /**
     * Determines if this Error signal only contains errors with severity <i>warning</i>.
     * 
     * @param errorSignal   The Error signal to check
     * @return              <code>true</code> if all errors in the signal have severity <i>warning</i>,<br>
     *                      <code>false</code> otherwise
     */
    private boolean isWarning(final IErrorMessage errorSignal) {
        boolean isWarning = true;
        
        Iterator<IEbmsError>  errs = errorSignal.getErrors().iterator();
        while (errs.hasNext() && isWarning) {
            isWarning = (errs.next().getSeverity() == IEbmsError.Severity.WARNING);
        }
        
        return isWarning;
    }
}
