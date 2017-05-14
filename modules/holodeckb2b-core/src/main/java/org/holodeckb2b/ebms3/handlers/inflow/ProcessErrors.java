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
import java.util.Collection;
import java.util.Iterator;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Is the <i>IN_FLOW</i> handler responsible for processing received error signals. For each error contained in one of
 * the {@link IErrorMessageEntity}s available in the message context property {@link MessageContextProperties#IN_ERRORS} it
 * will check if there is a {@link IMessageUnitEntity} in the database and mark that message as failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessErrors extends BaseHandler {

    /**
     * Errors will always be logged to a special error log. Using the logging configuration users can decide if this
     * logging should be enabled and how errors should be logged.
     */
    private final Log     errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.received");

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        log.debug("Check for received errors in message.");
        final ArrayList<IErrorMessageEntity>  errorSignals =
                              (ArrayList<IErrorMessageEntity>) mc.getProperty(MessageContextProperties.IN_ERRORS);

        if (!Utils.isNullOrEmpty(errorSignals)) {
            log.debug("Message contains " + errorSignals.size() + " Error signals, start processing");
            for (final IErrorMessageEntity e : errorSignals)
                // Ignore Errors that already failed
                if (e.getCurrentProcessingState().getState() != ProcessingState.FAILURE)
                    processErrorSignal(e, mc);
            log.debug("All Error Signals processed");
        } else
            log.debug("Message does not contain error signals, continue processing");

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
     * @param mc                The current message context
     * @throws PersistenceException When a database error occurs while processing the Error Signal
     */
    protected void processErrorSignal(final IErrorMessageEntity errSignal, final MessageContext mc)
                                                                                        throws PersistenceException {
        log.debug("Start processing Error Signal [msgId=" + errSignal.getMessageId() + "]");
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        // Change processing state to indicate we start processing the error. Also checks that the error is not
        // already being processed
        if (!storageManager.setProcessingState(errSignal, ProcessingState.RECEIVED, ProcessingState.PROCESSING)) {
            log.debug("Error Signal [msgId=" + errSignal.getMessageId() + "] is already being processed, skipping");
            return;
        }

        // Always log the error signal, even if its processing may fail later
        if (isWarning(errSignal))
            errorLog.warn(MessageUnitUtils.errorSignalToString(errSignal));
        else
            errorLog.error(MessageUnitUtils.errorSignalToString(errSignal));

        log.debug("Get referenced message unit(s)");
        Collection<IMessageUnitEntity> refdMessages = null;
        // There may not be a refToMessageId in the error itself, in that case the message units from the request are
        // assumed to be referenced
        final String refToMessageId = errSignal.getRefToMessageId();
        if (!Utils.isNullOrEmpty(refToMessageId)) {
            log.debug("Error Signal [" + errSignal.getMessageId() + "] references messageId: "
                        + refToMessageId);
            refdMessages = HolodeckB2BCore.getQueryManager().getMessageUnitsWithId(refToMessageId);
        } else if (isInFlow(INITIATOR)) {
            log.warn("Error Signal [" + errSignal.getMessageId() + "] does not contain reference."
                    + "Assuming it refers to sent messages");
            refdMessages = MessageContextUtils.getSentMessageUnits(mc);
        }

        // An error should reference a message unit
        if (Utils.isNullOrEmpty(refdMessages)) {
            log.warn("Error Signal [" + errSignal.getMessageId() + "] does not reference a known message unit!");
            // Create error and add to context
            final ValueInconsistent   viError = new ValueInconsistent();
            viError.setErrorDetail("Error does not reference a sent message unit");
            viError.setRefToMessageInError(errSignal.getMessageId());
            MessageContextUtils.addGeneratedError(mc, viError);
            // The message processing of the error fails
            storageManager.setProcessingState(errSignal, ProcessingState.FAILURE);
        } else {
            // Change the processing state of the found message unit(s)
            for (final IMessageUnitEntity mu : refdMessages) {
                if (isWarning(errSignal)) {
                    log.debug("Error level is warning, set processing state of referenced message ["
                              + mu.getMessageId() + "] to warning");
                    storageManager.setProcessingState(mu, ProcessingState.WARNING);
                } else {
                    log.debug("Error level is warning, set processing state of referenced message ["
                              + mu.getMessageId() + "] to failure");
                    storageManager.setProcessingState(mu, ProcessingState.FAILURE);
                }
            }
            log.debug("Processed Error Signal [" + errSignal.getMessageId() + "]");
            // Errors may need to be delivered to bussiness app which can be done now
            storageManager.setProcessingState(errSignal, ProcessingState.READY_FOR_DELIVERY);
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

        final Iterator<IEbmsError>  errs = errorSignal.getErrors().iterator();
        while (errs.hasNext() && isWarning) {
            isWarning = (errs.next().getSeverity() == IEbmsError.Severity.WARNING);
        }

        return isWarning;
    }
}
