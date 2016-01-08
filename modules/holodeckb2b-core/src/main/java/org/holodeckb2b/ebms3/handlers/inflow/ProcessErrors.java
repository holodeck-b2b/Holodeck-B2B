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
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
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

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        log.debug("Check for received errors in message.");
        ArrayList<ErrorMessage>  errorSignals = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.IN_ERRORS);
        
        if (errorSignals != null && !errorSignals.isEmpty()) {
            log.debug("Message contains " + errorSignals.size() + " Error signals, start processing");
            for (ErrorMessage e : errorSignals)
                // Ignore Errors that already failed
                if (!ProcessingStates.FAILURE.equals(e.getCurrentProcessingState().getName())) {
                    if (!processErrorSignal(e, mc)) {
                        log.warn("Error Signal [msgId=" + e.getMessageId() + "] could not be processed succesfully!");
                    }
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
     * @param errSignal     The {@link ErrorMessage} to process
     * @param mc            The current message context
     * @return              <code>true</code> if the error signal was processed successfully,<br>
     *                      <code>false</code> otherwise
     * @throws DatabaseException When a database error occurs while processing the Error Signal
     */
    protected boolean processErrorSignal(final ErrorMessage errSignal, MessageContext mc) throws DatabaseException {
        log.debug("Start processing Error [msgId=" + errSignal.getMessageId() + "]");        
        // Change processing state to indicate we start processing the error. Also checks that the error is not
        // already being processed
        ErrorMessage errorSignal = MessageUnitDAO.startProcessingMessageUnit(errSignal);
        if (errorSignal == null) {
            log.debug("Error [msgId=" + errorSignal.getMessageId() + "] is already being processed, skipping");
            return false;
        }

        log.debug("Get referenced message unit");
        MessageUnit refdMsgUnit = MessageUnitDAO.getSentMessageUnitWithId(errSignal.getRefToMessageId());

        // Change the processing state of the found messages
        log.debug("Setting processing state of referenced message [" + refdMsgUnit.getMessageId() 
                                                                                            + "] to failed");
        if (isWarning(errSignal)) {
            log.debug("The Error signal contains only warnings, so refd message unit is processed");
            MessageUnitDAO.setWarning(refdMsgUnit);
        } else {
            log.debug("The Error signal indicates failed processing");
            MessageUnitDAO.setFailed(refdMsgUnit);
        }

        log.debug("Done processing Error signal [" + errorSignal.getMessageId() + "]");
        // Errors may need to be delivered to bussiness app which can be done now
        MessageUnitDAO.setReadyForDelivery(errorSignal);   
        return true;
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
