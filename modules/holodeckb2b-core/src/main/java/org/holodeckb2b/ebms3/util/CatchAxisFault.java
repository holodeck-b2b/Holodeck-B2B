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
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is a special handler to handle unexpected and previously unhandled errors. When such errors are detected the
 * processing state of message units currently being processed will be changed to either indicate failed processing.
 * This means that the processing state of all message units in the current flow are set to <i>FAILURE</i>.
 * <p>When the error occurs while processing a request or creating a response to a PullRequest an ebMS <i>Other</i>
 * error is generated and reported to the sender of the request. No other message unit is included in the response to
 * the sender.<br>
 * Because error reporting to the <i>Producer</i> of a message is currently not supported the error is only logged when
 * it occurs when processing an outgoing request.
 * <p>Note that this is a kind of "last resort" error handler and therefore is not supposed to handle normal errors that
 * can occur in the processing of ebMS messages. These errors should all result in an ebMS error and handled
 * accordingly.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CatchAxisFault extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW | OUT_FLOW | RESPONDER;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws AxisFault {
        return InvocationResponse.CONTINUE;
    }

    @Override
    public void doFlowComplete(final MessageContext mc) {
        // This handler only needs to act when there was an unexpected failure
        if (mc.getFailureReason() != null) {
            log.error("A Fault was raised while processing messages!  Reported cause= "
                        + mc.getFailureReason().getMessage());
            // As we don't know the exact cause of the error the processing state of all message units that are being
            // currently processed should be set to failed if there processing is not completed yet
            Collection<IMessageUnitEntity>  msgUnitsInProcessing = null;

            if (isInFlow(OUT_FLOW))
                msgUnitsInProcessing = MessageContextUtils.getSentMessageUnits(mc);
            else
                msgUnitsInProcessing = MessageContextUtils.getReceivedMessageUnits(mc);

            for (final IMessageUnitEntity mu : msgUnitsInProcessing) {
                // Changing the processing state may fail if the problems are caused by the database.
                try {
                    final ProcessingState curState = mu.getCurrentProcessingState().getState();
                    if (ProcessingState.DELIVERED != curState && ProcessingState.AWAITING_RECEIPT != curState) {
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

            log.debug("Remove existing outgoing message units from context");
            mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, null);
            mc.setProperty(MessageContextProperties.OUT_RECEIPTS, null);
            mc.setProperty(MessageContextProperties.OUT_ERRORS, null);

            // If we are in the in flow and responding to received messages or when we were sending response to a
            // PullRequest we will sent an ebMS "Other" error to indicate the problem
            if (isInFlow((byte) (IN_FLOW | RESPONDER)) ||
                MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_PULL_REQUEST) != null)
                createOtherError(mc);
        }
    }

    /**
     * Creates a new Error Signal message with one <i>Other</i> error that indicates that an internal error occurred
     * while processing the received message unit(s). As we don't know exactly what was the cause of the error the ebMS
     * error does not reference any other message unit.
     *
     * @param mc    The current message context to which the error is added
     */
    private void createOtherError(final MessageContext mc) {
        final OtherContentError   otherError = new OtherContentError();
        otherError.setErrorDetail("An internal error occurred while processing the message.");
        otherError.setSeverity(IEbmsError.Severity.WARNING);
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.addError(otherError);
        try {
            log.debug("Create the Error signal message");
            IErrorMessageEntity storedError = (IErrorMessageEntity) HolodeckB2BCore.getStorageManager()
                                                                                .storeOutGoingMessageUnit(errorMessage);
            log.debug("Created a new Error signal message");
            mc.setProperty(MessageContextProperties.OUT_ERRORS, Collections.singletonList(storedError));
            log.debug("Set the Error signal as the only ebMS message to return");
        } catch (final PersistenceException dbe) {
            // (Still) a problem with the database, create the Error signal message without storing it
            log.fatal("Could not store error signal message in database! Details: " + dbe.getMessage());
            log.debug("Set the non-persisted ErrorMessage in message context");
            errorMessage.setMessageId(MessageIdUtils.createMessageId());
            mc.setProperty(MessageContextProperties.OUT_ERRORS, Collections.singletonList(errorMessage));
        }
        // Remove the error condition from the context as we handled the error here
        mc.setFailureReason(null);
    }
}
