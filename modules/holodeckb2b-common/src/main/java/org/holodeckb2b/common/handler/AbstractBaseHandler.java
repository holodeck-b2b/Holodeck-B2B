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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.util.Utils;

/**
 * Is an <i>abstract</i> implementation of an Axis2 handler that acts as the base class for the Holodeck B2B handlers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0  This handler is a refactored version of the old <code>org.holodeckb2b.common.handler.BaseHandler</code>
 */
public abstract class AbstractBaseHandler extends AbstractHandler {

    /**
     * The name of the messaging protocol that this handler is processing, taken from the parent module
     */
    protected String 	handledMsgProtocol = null;
    /**
     * Indicator whether the handler is configured to only run when it is responding to a received request. This
     * should be indicated by setting the handler parameter <i>onlyAsResponder</i> to "true". 
     */
    private	boolean 	runOnlyAsResponder;
    
    /**
     * Initializes the handler by checking it is contained in a Holodeck B2B module that is dealing with a specific
     * messaging protocol, e.g. ebMS V3 or AS2. The protocol handled by the module must be specified in the <i>
     * HandledMessagingProtocol</i> parameter of the module.
     *
     */
    @Override
	public void init(HandlerDescription handlerdesc) {
        super.init(handlerdesc);
        try {
    		handledMsgProtocol = (String) handlerdesc.getParent().getParameter("HandledMessagingProtocol").getValue();
        } catch (Exception e) {
    		LogFactory.getLog(this.getClass().getName())
        				  					.warn("Not running inside a Holodeck B2B module for message processing");
        }
        
        Parameter restricted = handlerdesc.getParameter("onlyAsResponder");
        runOnlyAsResponder = restricted != null && JavaUtils.isTrueExplicitly(restricted.getValue());
    }

    /**
     * Prepares the handler for processing of the message. Checks if the handler is run in the correct flow and
     * creates a correctly named {@link Log}.
     * <p>NOTE: To prevent sub classes from overriding this method it is declared final.
     *
     * @param mc            The Axis2 {@link MessageContext}. Will be passed onto the implementation for the actual
     *                      processing
     * @return              The result of the message processing, i.e. the result of
     *                      {@link #doProcessing(MessageProcessingContext, Log)} of the implementation
     * @throws AxisFault    If an error occurs during the processing of the message that should prevent further
     *                      processing. Note that this will stop processing of the complete flow and may leave message
     *                      units in an undefined state!
     */
    @Override
	public final InvocationResponse invoke(final MessageContext mc) throws AxisFault {
    	
    	// Check if the handler is restricted to run only when responding to a request
    	if (runOnlyAsResponder && !mc.isServerSide())
    		return InvocationResponse.CONTINUE;
    		
    	Log log = prepareLog(mc);
    	
        // Do actual processing in implementation
        try {
            log.trace("Start processing");
            final InvocationResponse result = doProcessing(getProcContext(mc), log);
            log.trace("End processing");
            return result;
        } catch (final Throwable t) {
            // Unhandled exception during processing, should not happen!
            log.error("An unhandled exception occurred while processing the message! Details: " + t.getMessage());
            throw new AxisFault("Internal error", t);
        }
    }
    
    /**
     * Runs when the execution of the flow is completed and if the handler is executed during that flow. Actual
     * processing is only needed when implementation is supposed to run in this flow.
     *
     * @param mc    The current message context
     */
    @Override
    public final void flowComplete(final MessageContext mc) {
        if (!runOnlyAsResponder || mc.isServerSide())
            doFlowComplete(getProcContext(mc), prepareLog(mc));
    }
    
    /**
     * Helper method to prepare a corrected named log.
     *  
     * @param mc	The message context
     * @return		The log to be used during this execution of the handler
     */
    private Log prepareLog(final MessageContext mc) {
        // Determine which flow the handler currently runs is
        String 	currentFlowName = mc.isServerSide() ? "RESPONSE_" : "REQUEST_";
        
        switch (mc.getFLOW()) {
            case MessageContext.IN_FLOW :
                currentFlowName += "IN_FLOW"; break;
            case MessageContext.IN_FAULT_FLOW :
                currentFlowName += "IN_FAULT_FLOW"; break;
            case MessageContext.OUT_FLOW :
                currentFlowName += "OUT_FLOW"; break;
            case MessageContext.OUT_FAULT_FLOW :
                currentFlowName += "OUT_FAULT_FLOW"; 
        }
        
        // Running in correct flow, create a logger
        return LogFactory.getLog("org.holodeckb2b.msgproc." + (!Utils.isNullOrEmpty(handledMsgProtocol) ?
                                                                handledMsgProtocol + "." : "")
        													+ currentFlowName + "." + getHandlerDesc().getName());
    }
    
    /**
     * Helper method to get the Holodeck B2B processing context from the Axis message context. If not yet set a new
     * processing context will be created and set.
     * 
     * @param mc	The Axis2 message context
     * @return		The Holodeck B2B processing context
     */
    private MessageProcessingContext getProcContext(final MessageContext mc) {
    	MessageProcessingContext procCtx = (MessageProcessingContext) 
    														mc.getProperty(MessageProcessingContext.AXIS_MSG_CTX_PROP);
    	if (procCtx == null)
    		procCtx = new MessageProcessingContext(mc);
    	
    	return procCtx;
    }

    /**
     * Abstract method that implementations must use to do the actual message processing.
     *
     * @param procCtx       The current Holodeck B2B {@link MessageProcessingContext}
     * @param log 			Log to use 
     * @return              How to continue processing of the message. If message processing should not continue, it is
     *                      RECOMMENDED to throw an AxisFault instead of returning <code>InvocationResponse.ABORT</code>
     *                      because this enables sending a response.
     * @throws Exception    If an error occurs during the processing of the message that should prevent further
     *                      processing. Note that this will stop processing of the complete flow and may leave message
     *                      units in an undefined state! Also ensure that all information needed for a response is set
     *                      in the message context to make it available for handlers in the fault flow!
     */
    protected abstract InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Log log) 
    																								throws Exception;

    /**
     * Runs when the execution of the flow is completed and if the handler is executed during that flow. This method
     * is always executed independent of the flow's processing result. It can be used to check the processing result
     * and take action action.
     * <p>NOTE: It is possible that not all handlers in the flow have been executed when this method is called, so this
     * method should not depend on execution of handlers later in the flow.
     * <p>NOTE: A default no-op implementation is provided so sub classes only need to override if action is required.
     *
     * @param procCtx       The current Holodeck B2B {@link MessageProcessingContext}
     * @param log 			Log to use 
     */
    protected void doFlowComplete(final MessageProcessingContext procCtx, final Log log) {}
}
