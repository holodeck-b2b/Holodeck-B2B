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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Is the first handler of the out flow and is responsible for preparing a response by checking if the handlers in the
 * in flow prepared message units that should be sent as response. If they did the entity objects for the message units
 * are copied to the out flow message context to make them available to the other handlers in the out flow.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PrepareResponseMessage extends BaseHandler {

    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW | RESPONDER;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws AxisFault {
        // Check which response message units were set during in flow, starting with user message
        log.trace("Check for response user message unit");
        final IUserMessageEntity um = (IUserMessageEntity) MessageContextUtils.getPropertyFromInMsgCtx(mc,
                                                                            MessageContextProperties.OUT_USER_MESSAGE);
        if (um != null) {
            log.debug("Response contains an user message unit");
            // Copy to current context so it gets processed correctly
            mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, um);
        } 
        // Check if there is a receipt signal messages to be included
        log.trace("Check for receipt signal to be included");
        final IReceiptEntity receipt = (IReceiptEntity) MessageContextUtils.getPropertyFromInMsgCtx(mc,
                                                                          MessageContextProperties.RESPONSE_RECEIPT);
        if (receipt != null) {
            log.debug("Response contains a receipt signal");
            // Copy to current context so it gets processed correctly
            MessageContextUtils.addReceiptToSend(mc, receipt);
        } 
        // Check if there are error signal messages to be included
        log.trace("Check for error signals generated during in flow to be included");
        final Collection<IErrorMessageEntity> errors =
                            (Collection<IErrorMessageEntity>) MessageContextUtils.getPropertyFromInMsgCtx(mc,
                                                                                  MessageContextProperties.OUT_ERRORS);
        if (Utils.isNullOrEmpty(errors))
            log.trace("Response does not contain error signal(s)");
        else if (errors.size() > 1) {
            // The were multiple error signals generated in the in flow, check if bundling is allowed
            log.trace("Response contains multiple error signals");
            if (HolodeckB2BCore.getConfiguration().allowSignalBundling()) {
                // Bundling is enabled, so include all errors
                log.debug("Bundling allowed, add all errors to response");
                // Copy to current context so it gets processed correctly
                mc.setProperty(MessageContextProperties.OUT_ERRORS, errors);
            } else {
                // Bundling not allowed, select one error signal to include
                log.trace("Bundling is not allowed, select one error to report");
                final IErrorMessageEntity include = selectError(errors, mc);
                errors.remove(include);
                // The other errors can not be further processed, so change their state to failed
                setFailed(errors);
                log.debug("Include the selected error [msgID/refTo" + include.getMessageId() + "/"
                                + include.getRefToMessageId() + "] for processing");
                MessageContextUtils.addErrorSignalToSend(mc, include);
            }
        } else {
            log.debug("Response does contain one error signal");
            MessageContextUtils.addErrorSignalToSend(mc, errors.iterator().next());
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Is a helper method to select the error signal that has the highest priority of sending. The priority of error
     * signals is determined by the message unit they reference and as follows:<ol>
     * <li>no referenced message unit: The error is to the complete message and can not be related to a message unit.
     * This indicates a general problem with the message and should be reported;</li>
     * <li>UserMessage</li>
     * <li>PullRequest</li>
     * <li>Error or Receipt</li>
     * </ol>
     *
     * @param errors    The collection of errors that were generated for the received message
     * @param mc        The current [out flow] messsage context
     * @return          The error signal message to include in the response
     */
    private IErrorMessageEntity selectError(final Collection<IErrorMessageEntity> errors, final MessageContext mc) {
        IErrorMessageEntity    r = null;
        int             cp = -1; // The prio of the currently selected err, 0=Error or Receipt, 1=PullReq, 2=UsrMsg

        log.trace("Get the messageIds and types of received message units");
        final Map<String, Class<? extends IMessageUnitEntity>>  rcvdMsgs = getReceivedMessageUnits(mc);
        // Now select the error with highest prio
        for(final IErrorMessageEntity e : errors) {
            final String refTo = e.getRefToMessageId();
            if (Utils.isNullOrEmpty(refTo)) {
                log.debug("Select general error (without refTo)");
                r = e; // This is the error signal that does not reference a message unit
                break;
            } else {
                log.debug("Check which type of MU is referenced by error");
                final Class<? extends IMessageUnitEntity> type = rcvdMsgs.get(refTo);
                if (IUserMessage.class.isAssignableFrom(type)) {
                    log.debug("Select error referencing the UserMessage");
                    r = e; cp = 2;
                } else if (IPullRequest.class.isAssignableFrom(type) && cp < 1) {
                    log.debug("Select error referencing the PullRequest");
                    r = e; cp = 1;
                } else if (cp == -1) {
                    // Error references either Error or Receipt which have same prio, just select first
                    log.debug("Select error referencing the Error or Receipt");
                    r = e; cp = 0;
                }
            }
        }
        return r;
    }

    /**
     * Gets all the message units received in the request message and map them on their <i>message id</i>.
     *
     * @param mc    The current [out flow] message context
     * @return      {@link Map} containing all messageIds and their message unit type
     */
    private Map<String, Class<? extends IMessageUnitEntity>> getReceivedMessageUnits(final MessageContext mc) {
        final HashMap<String, Class<? extends IMessageUnitEntity>>   reqMUs = new HashMap<>();

        final Collection<IMessageUnitEntity> msgUnits = MessageContextUtils.getReceivedMessageUnits(mc);
        for (final IMessageUnitEntity mu : msgUnits)
            reqMUs.put(mu.getMessageId(), mu.getClass());

        return reqMUs;
    }

    /**
     * Helper method to change the processing state of the error signals that can not be included in the response
     * to {@link ProcessingState#FAILURE}.
     *
     * @param errors    The collection of {@link IErrorMessageEntity}s that can not be included in the response.
     */
    private void setFailed(final Collection<IErrorMessageEntity> errors) {
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        for(final IErrorMessageEntity e : errors)
            try {
                updateManager.setProcessingState(e, ProcessingState.FAILURE);
                log.trace("Changed state of error [" + e.getMessageId()
                            + "] to failed as it can not be included in response!");
            } catch (final PersistenceException dbe) {
                // Log and ignore error
                log.error("An error occured while changing the state of error [" + e.getMessageId()
                            + "]! Details: " + dbe.getMessage());
            }
    }
}
