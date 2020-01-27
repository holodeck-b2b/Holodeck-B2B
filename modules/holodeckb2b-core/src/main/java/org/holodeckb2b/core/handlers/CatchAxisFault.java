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
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is a special handler to handle unexpected and previously unhandled errors. When such errors are detected the
 * processing state of message units currently being processed will be changed to either indicate failed processing.
 * This means that the processing state of all message units in the current flow are set to <i>FAILURE</i>.
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
        // This handler only needs to act when there was an unexpected failure
        if (msgContext.getFailureReason() != null) {
            log.error("A Fault was raised while processing messages!  Reported cause= "
                        + msgContext.getFailureReason().getMessage());
            // As we don't know the exact cause of the error the processing state of all message units that are being
            // currently processed should be set to failed if there processing is not completed yet
            Collection<IMessageUnitEntity>  msgUnitsInProcess = null;

            if (msgContext.getFLOW() == MessageContext.IN_FLOW)
                msgUnitsInProcess = procCtx.getReceivedMessageUnits();
            else
                msgUnitsInProcess = procCtx.getSendingMessageUnits();

            for (final IMessageUnitEntity mu : msgUnitsInProcess) {
                // Changing the processing state may fail if the problems are caused by the database.
                try {
                    final ProcessingState curState = mu.getCurrentProcessingState().getState();
                    if (ProcessingState.DELIVERED != curState && ProcessingState.AWAITING_RECEIPT != curState
                    	&& ProcessingState.DONE != curState) {
                        log.error(MessageUnitUtils.getMessageUnitName(mu) + " with msg-id [" + mu.getMessageId()
                                    + "] could not be processed due to an internal error.");
                        HolodeckB2BCore.getStorageManager().setProcessingState(mu, ProcessingState.FAILURE);
                    }
                } catch (final PersistenceException ex) {
                    // Unable to change the processing state, log the error.
                    log.error(MessageUnitUtils.getMessageUnitName(mu) + " with msg-id [" + mu.getMessageId()
                                + "] could not be processed due to an internal error.");
                }
            }

            log.debug("Clear the message processing context");
            procCtx.removeAllMessages();
            
            // If we are in the in flow and responding to received messages 
            if (msgContext.isServerSide() && (msgContext.getFLOW() == MessageContext.IN_FLOW 
            							     || msgContext.getFLOW() == MessageContext.IN_FAULT_FLOW)) {                 
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
        } catch (final PersistenceException dbe) {
            // (Still) a problem with the database, create the Error signal message without storing it
            log.fatal("Could not store error signal message in database! Details: " + dbe.getMessage());
            log.trace("Create a non-persisted ErrorMessageEntity so we can still send the message");
            errorMessage.setMessageId(MessageIdUtils.createMessageId());
            return new NonPersistedErrorMessage(errorMessage);
        }
    }
    
    /**
     * Helper class to allow sending of an error the sender of the message even if the persistency layer is down. 
     */
    class NonPersistedErrorMessage extends ErrorMessage implements IErrorMessageEntity {
    	
    	public NonPersistedErrorMessage(final ErrorMessage source) {
			super(source);
		}

		@Override
		public boolean isLoadedCompletely() {
			return true;
		}

		@Override
		public boolean usesMultiHop() {
			return false;
		}

		@Override
		public boolean shouldHaveSOAPFault() {
			return true;
		}
    }
}
