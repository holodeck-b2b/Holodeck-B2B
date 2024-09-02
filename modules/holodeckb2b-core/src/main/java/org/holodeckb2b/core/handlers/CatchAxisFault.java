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
package org.holodeckb2b.core.handlers;

import java.util.Collection;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.events.impl.GenericReceiveMessageFailure;
import org.holodeckb2b.common.events.impl.GenericSendMessageFailure;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.storage.NonPersistedErrorMessage;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;

/**
 * Is a special handler to handle unexpected and previously unhandled errors. When such errors are detected the
 * processing state of message units currently being processed will be changed to indicate failed processing. This means
 * that the processing state of all message units in the current flow are set to <i>FAILURE</i> unless their current
 * state already indicate a failure or when the processing is in a "stable" state, e.g. waiting for receipt or completed
 * <p>When the error occurs while processing a request an ebMS <i>Other</i> error is generated and reported to the
 * sender of the request. No other message unit is included in the response to the sender.<br>
 * <p>Note that this is a kind of "last resort" error handler and therefore is not supposed to handle normal errors that
 * can occur in the processing of ebMS messages. These errors should all result in an ebMS error and handled
 * accordingly.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CatchAxisFault extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext mc, final Logger log) throws AxisFault {
        return InvocationResponse.CONTINUE;
    }

    @Override
    public void doFlowComplete(final IMessageProcessingContext procCtx, final Logger log) {
    	final MessageContext msgContext = procCtx.getParentContext();
        // This handler only needs to act when there was a failure
        if (msgContext.getFailureReason() != null) {
        	final Exception cause = msgContext.getFailureReason();
            log.error("An error occurred while processing messages! Error stack=\n {}",
                        Utils.getExceptionTrace(cause, true));
            // As we don't know the exact cause of the error the processing state of all message units that are being
            // currently processed should be set to failed if there processing is not completed yet
            Collection<IMessageUnitEntity>  msgUnitsInProcess = null;
            final boolean receiving = msgContext.getFLOW() == MessageContext.IN_FLOW
            							|| msgContext.getFLOW() == MessageContext.IN_FAULT_FLOW;
            if (receiving)
                msgUnitsInProcess = procCtx.getReceivedMessageUnits();
            else
                msgUnitsInProcess = procCtx.getSendingMessageUnits();

            for (final IMessageUnitEntity mu : msgUnitsInProcess) {
                // Changing the processing state may fail if the problems are caused by the database.
                try {
                    final ProcessingState curState = mu.getCurrentProcessingState().getState();
                    if (ProcessingState.DELIVERED != curState && ProcessingState.AWAITING_RECEIPT != curState
                    	&& ProcessingState.DONE != curState && ProcessingState.TRANSPORT_FAILURE != curState) {
                        log.error(MessageUnitUtils.getMessageUnitName(mu) + " with msg-id [" + mu.getMessageId()
                                    + "] could not be processed due to an unexpected error.");
                        HolodeckB2BCore.getStorageManager().setProcessingState(mu,
                        			mu instanceof IUserMessage && mu.getDirection() == Direction.OUT ?
                        										ProcessingState.SUSPENDED : ProcessingState.FAILURE);
                        // Raise event to signal the processing failure
                        HolodeckB2BCore.getEventProcessor().raiseEvent(receiving ?
		                        								new GenericReceiveMessageFailure(mu, cause)
		                        							  : new GenericSendMessageFailure(mu, cause));
                    }
                } catch (final StorageException ex) {
                    // Unable to change the processing state, log the error.
                    log.error(MessageUnitUtils.getMessageUnitName(mu) + " with msg-id [" + mu.getMessageId()
                                + "] could not be processed due to an internal error.");
                }
            }

            log.debug("Clear the message processing context");
            procCtx.removeAllMessages();

            // If we are responding to a received messages and were in the normal flow, try to send an Error back
            if (msgContext.isServerSide() && msgContext.getFLOW() != MessageContext.OUT_FAULT_FLOW) {
            	procCtx.addSendingError(createOtherError(log));
            }
        	// We have handled the error, so nothing to do for Axis
        	msgContext.setFailureReason(null);
        }
    }

    /**
     * Creates a new Error Signal message with one <i>Other</i> error that indicates that an internal error occurred
     * while processing the received message unit(s). As we don't know exactly what was the cause of the error the ebMS
     * error does not reference any other message unit.
     *
     * @param mc    The current message context to which the error is added
     */
    private IErrorMessageEntity createOtherError(final Logger log) {
        final OtherContentError   otherError = new OtherContentError();
        otherError.setErrorDetail("An internal error occurred while processing the message.");
        otherError.setSeverity(IEbmsError.Severity.warning);
        ErrorMessage errorMessage = new ErrorMessage(otherError);
        try {
            return (IErrorMessageEntity) HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(errorMessage);
        } catch (final StorageException dbe) {
            // (Still) a problem with the database, create the Error signal message without storing it
            log.fatal("Could not store error signal message in database! Details: " + dbe.getMessage());
            log.trace("Create a non-persisted ErrorMessageEntity so we can still send the message");
            return new NonPersistedErrorMessage(errorMessage);
        } catch (DuplicateMessageIdException e) {
        	// Can never occur because a unique id is generated for the Error Message
        	return null;
		}
    }
}
