/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.handlers.inflow.ReadUserMessage;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;

/**
 * Extends {@link BaseHandler} to ensure the handler only runs when there is a User Message to process. It checks 
 * whether a {@link UserMessage} object is available in the current <code>MessageContext</code> for the flow the handler
 * is running in and that its current processing state is not <code>FAILURE</code>.
 * <p>Implementation that run in the <i>IN_FLOW</i> or <i>IN_FAULT_FLOW</i> must be configured to run after the {@link
 * ReadUserMessage} handler because this handler is responsible for reading the user message unit and adding the 
 * {@link UserMessage} object it to the message context.
 * <p>Implementations MUST implement the {@link BaseHandler#inFlows()} to indicate the flows which the handler can run 
 * in. Only when the handler runs in the correct flow <code>AbstractUserMessageHandler</code> will check if there is a
 * user message present in the current context.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public abstract class AbstractUserMessageHandler extends BaseHandler {

    /**
     * Checks whether this flow contains a user message unit for processing. Only when it does message processing
     * is passed on to the implementation class.
     * <p>In the in flows the <code>UserMessage</code> should be available in the {@link MessageContextProperties#IN_USER_MESSAGE}
     * message context property. For the out flows it should be available in the {@link MessageContextProperties#OUT_USER_MESSAGE}.
     * 
     * @param mc            The current {@link MessageContext} 
     * @return              How to continue processing. If there is no UserMessage unit available message processing
     *                      is always continued; if there is a UserMessage unit the implementation class decides how
     *                      to continue processing {@link #doProcessing(org.apache.axis2.context.MessageContext, 
     *                      org.holodeckb2b.ebms3.persistent.message.UserMessage)}
     * @throws AxisFault    When the message context property that should contain the <code>UserMessage</code> object
     *                      exists but does not contain a <code>UserMessage</code> object <b>OR</b> when the 
     *                      implementation throws the exception in 
     *                      {@link #doProcessing(org.apache.axis2.context.MessageContext, org.holodeckb2b.ebms3.persistent.message.UserMessage)}
     */
    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws AxisFault {
        
        log.debug("Check if MessageContext contains a MessageUnit");
        UserMessage mu = null;
        try {            
            mu = (UserMessage) mc.getProperty((isInFlow(IN_FLOW) ? 
                    MessageContextProperties.IN_USER_MESSAGE : MessageContextProperties.OUT_USER_MESSAGE));
        } catch (ClassCastException cce) {
            // MessageContext did contain an object for the given key, but it is not a UserMessage! 
            // This SHOULD not occur, abort processing
            log.fatal("Illegal state of processing! MessageContext contained non UserMessage object as user message!");
            throw new AxisFault("Internal error", 
                                new IllegalStateException(
                                        "MessageContext contained a non UserMessage object as user message!"));
        }
        
        if (mu == null || ProcessingStates.FAILURE.equals(mu.getCurrentProcessingState())) {
            // This flow does not contain a UserMessage message unit to process, so nothing to do
            log.debug("MessageContext does not contain a UserMessage message unit for processing, continue flow");
            return InvocationResponse.CONTINUE;
        } else {
            // The flow contains a UserMessage, continue processing in implementation
            return doProcessing(mc, mu);
        }
    }
    
    /**
     * Abstract method that implementations must use to do the actual user message processing.
     * 
     * @param mc            The Axis2 {@link MessageContext} of the processed message
     * @param um            The currently processed {@links UserMessage}
     * @return              How to continue processing of the message. If message processing should not continue, it is
     *                      RECOMMENDED to throw an AxisFault instead of returning <code>InvocationResponse.ABORT</code>
     *                      because this enables sending a response.
     * @throws AxisFault    If an error occurs during the processing of the user message that should prevent further 
     *                      processing. Note that this will stop processing of the complete flow and may leave message
     *                      units in an undefined state! Also ensure that all information needed for a response is set
     *                      in the message context to make it available for handlers in the fault flow!
     */
    protected abstract InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws AxisFault;    
}
