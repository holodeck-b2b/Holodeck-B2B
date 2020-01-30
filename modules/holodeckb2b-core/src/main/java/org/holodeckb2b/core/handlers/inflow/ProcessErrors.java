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
package org.holodeckb2b.core.handlers.inflow;

import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.ValueInconsistent;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is the <i>IN_FLOW</i> handler responsible for processing received error signals. For each error contained in one of
 * the {@link IErrorMessageEntity}s available in the message processing context it will check if there is a {@link 
 * IMessageUnitEntity} in the database and mark that message as failed.
 * <p>Errors will always be logged to a special error log. Using the logging configuration users can decide if this
 * logging should be enabled and how errors should be logged.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessErrors extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) 
    																					throws PersistenceException {
        log.debug("Check for received errors in message.");
        final Collection<IErrorMessageEntity> errorSignals = procCtx.getReceivedErrors();
        
        if (!Utils.isNullOrEmpty(errorSignals)) {
            log.debug("Message contains " + errorSignals.size() + " Error signals, start processing");
            for (final IErrorMessageEntity e : errorSignals)
                // Ignore Errors that already failed
                if (e.getCurrentProcessingState().getState() != ProcessingState.FAILURE)
                    processErrorSignal(e, procCtx, log);
            log.debug("All Error Signals processed");
        } 
        return InvocationResponse.CONTINUE;
    }

    /**
     * Processes an Error signal.
     * <p>First the referenced message id is checked for correctness meaning that it refers to an existing message unit.
     * <p>If it refers to non existing message units an <i>ValueInconsistent</i> error will be generated and added to
     * the message context. The processing state of the error signal itself will be set to
     * {@link ProcessingState#FAILURE}.
     * <p>If the referenced id is valid the referenced message units processing state will be changed to {@link
     * ProcessingState#FAILURE}. The processing state of the error signal itself is set to {@link
     * ProcessingState#READY_FOR_DELIVERY} to indicate that the error can be delivered to the business application if
     * needed.
     *
     * @param errSignal    The {@link IErrorMessageEntity} object representing the Error Signal to process
     * @param procCtx      The current message processing context
     * @throws PersistenceException When a database error occurs while processing the Error Signal
     */
    protected void processErrorSignal(final IErrorMessageEntity errSignal, final IMessageProcessingContext procCtx, 
    								  final Logger log) throws PersistenceException {
        log.debug("Start processing Error Signal [msgId=" + errSignal.getMessageId() + "]");
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        // Change processing state to indicate we start processing the error. Also checks that the error is not
        // already being processed
        if (!storageManager.setProcessingState(errSignal, ProcessingState.RECEIVED, ProcessingState.PROCESSING)) {
            log.warn("Error Signal [msgId=" + errSignal.getMessageId() + "] is already (being) processed, skipping");
            return;
        }

        // Always log the error signal, even if its processing may fail later
        final Logger errorLog = LogManager.getLogger("org.holodeckb2b.msgproc.errors.received." + 
        										  procCtx.getParentContext().getAxisService().getName().toUpperCase());
        if (MessageUnitUtils.isWarning(errSignal))
            errorLog.warn(MessageUnitUtils.errorSignalToString(errSignal));
        else
            errorLog.error(MessageUnitUtils.errorSignalToString(errSignal));

        log.trace("Get referenced message unit(s)");
        Collection<IMessageUnitEntity> refdMessages = null;
        // There may not be a refToMessageId in the error itself, in that case the message units from the request are
        // assumed to be referenced
        final String refToMessageId = errSignal.getRefToMessageId();
        if (!Utils.isNullOrEmpty(refToMessageId)) {
            log.debug("Error Signal [" + errSignal.getMessageId() + "] references messageId: "
                        + refToMessageId);
            final IMessageUnitEntity sentMessage = procCtx.getSendingMessageUnit(refToMessageId);
            if (sentMessage != null)
            	refdMessages = Collections.singletonList(sentMessage);
            else
            	refdMessages = HolodeckB2BCore.getQueryManager().getMessageUnitsWithId(refToMessageId, Direction.OUT);        
        } else {
            log.debug("Error Signal [" + errSignal.getMessageId() + "] does not contain reference."
            			+ "Assuming it refers to all sent messages");
            refdMessages = procCtx.getSendingMessageUnits();
        }

        // An error should reference a message unit
        if (Utils.isNullOrEmpty(refdMessages)) {
            log.warn("Error Signal [" + errSignal.getMessageId() + "] does not reference a known message unit!");
            // Create error and add to context
            procCtx.addGeneratedError(new ValueInconsistent("Error does not reference a sent message unit",
            												errSignal.getMessageId()));
            // The message processing of the error fails
            storageManager.setProcessingState(errSignal, ProcessingState.FAILURE);
        } else {
            // Change the processing state of the found message unit(s)
            for (final IMessageUnitEntity mu : refdMessages) {
                if (MessageUnitUtils.isWarning(errSignal)) {
                    if (mu instanceof IUserMessage)
                    	log.info("Received an Error Signal for User Message [msgId=" + mu.getMessageId() 
                    			 + "] with severity warning, message may not have been processed by receiver!");
                    log.debug("Error level is warning, set processing state of referenced message ["
                              + mu.getMessageId() + "] to warning");
                    storageManager.setProcessingState(mu, ProcessingState.WARNING);
                } else {
                    if (mu instanceof IUserMessage)
                    	log.info("Received an Error Signal for User Message [msgId=" + mu.getMessageId() 
                    			 + "] with severity failure, message has not been processed by receiver!");                	
                	log.debug("Error level is failure, set processing state of referenced message ["
                              + mu.getMessageId() + "] to failure");
                    storageManager.setProcessingState(mu, ProcessingState.FAILURE);
                }
            }
            log.info("Processed Error Signal [" + errSignal.getMessageId() + "]");
            storageManager.setProcessingState(errSignal, ProcessingState.READY_FOR_DELIVERY);
            procCtx.addRefdMsgUnitByError(errSignal, refdMessages);
        }
    }
}
