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
package org.holodeckb2b.common.handler;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Extends {@link AbstractBaseHandler} to ensure the handler only runs when there is a User Message to process. It 
 * checks whether a {@link IUserMessageEntity} object is available in the current message processing context for the 
 * flow the handler is running in and that its current processing state is not <code>FAILURE</code>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0  This handler is a refactored version of the old 
 * 				 <code>org.holodeckb2b.ebms3.util.AbstractUserMessageHandler</code>
 */
public abstract class AbstractUserMessageHandler extends AbstractBaseHandler {

    /**
     * Checks whether this flow contains a User Message unit for processing. Only when it does message processing
     * is passed on to the implementation class.
     *
     * {@inheritDoc}
     */
    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Log log) throws Exception {
        log.trace("Check if MessageContext contains a MessageUnit");
        final int currentFlow = procCtx.getParentContext().getFLOW();
        IUserMessageEntity userMessage = currentFlow == MessageContext.IN_FLOW 
        								 || currentFlow == MessageContext.IN_FAULT_FLOW ?
                                                                    procCtx.getReceivedUserMessage() :
                                                                    procCtx.getSendingUserMessage();
                                                                    
        if (userMessage == null || ProcessingState.FAILURE == userMessage.getCurrentProcessingState().getState()) {
            // This flow does not contain a UserMessage message unit to process, so nothing to do
            return InvocationResponse.CONTINUE;
        } else {
            // The flow contains a UserMessage, continue processing in implementation
            return doProcessing(userMessage, procCtx, log);
        }
    }

    /**
     * Abstract method that implementations must use to do the actual user message processing.
     * 
     * @param userMessage   The currently processed User Messsge as a {@link IUserMessageEntity}
     * @param procCtx       The Holodeck B2B {@link MessageProcessingContext}
     * @param log			Log to use
     * @return              How to continue processing of the message. If message processing should not continue, it is
     *                      RECOMMENDED to throw an AxisFault instead of returning <code>InvocationResponse.ABORT</code>
     *                      because this enables sending a response.
     * @throws Exception    If an error occurs during the processing of the user message that should prevent further
     *                      processing. Note that this will stop processing of the complete flow and may leave message
     *                      units in an undefined state! Also ensure that all information needed for a response is set
     *                      in the message context to make it available for handlers in the fault flow!
     */
    protected abstract InvocationResponse doProcessing(final IUserMessageEntity userMessage,
                                                       final MessageProcessingContext procCtx, Log log) 
                                                    		   										throws Exception;
}
