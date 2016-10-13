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
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Is an <i>abstract</i> implementation of an Axis2 handler that acts as the base class for the Holodeck B2B handlers.
 * It ensures that the handler runs only in the correct flows and prepares the logging.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public abstract class BaseHandler extends AbstractHandler {

    /**
     * Is used to identify the incoming flow that processes regular SOAP messages, i.e. not containing a SOAP Fault
     */
    protected static final byte   IN_FLOW = 4;

    /**
     * Is used to identify the outgoing flow that processes regular SOAP messages, i.e. not containing a SOAP Fault
     */
    protected static final byte   OUT_FLOW = 8;

    /**
     * Is used to identify the incoming flow that processes SOAP fault messages, i.e. containing a SOAP Fault
     */
    protected static final byte   IN_FAULT_FLOW = 16;

    /**
     * Is used to identify the outgoing flow that processes SOAP fault messages, i.e. containing a SOAP Fault
     */
    protected static final byte   OUT_FAULT_FLOW = 32;

    /**
     * Is the indication that Holodeck B2B is the initiator of the current message exchange (Leg in P-Mode terms),
     * i.e. the message is sent as a request (when processing the in flow) or received as a response (when processing
     * the out flow).
     */
    protected static final byte   INITIATOR = 1;

    /**
     * Is the indication that Holodeck B2B is the responder in the current message exchange (Leg in P-Mode terms),
     * i.e. the message is received in a request (when processing the in flow) or sent as a response (when processing
     * the out flow).
     */
    protected static final byte   RESPONDER = 2;

    /**
     * The log facility. The name of the log will include both identification of the handler as well as the flow it is
     * running in.
     */
    protected Log log = null;

    /**
     * The current flow as a byte
     */
    private byte currentFlow = 0;
    /**
     * The name of the current flow
     */
    private String currentFlowName = null;

    /**
     * Checks that the handler runs in the given flow.
     *
     * @param   flow    The flow the handler is expected to run in. Should be expressed using constants defined in this
     *                  class.<br>
     *                  <b>NOTE:</b> You can only check for one flow consisting of message type (normal or fault) and
     *                  whether Holodeck B2B is initiator of or responder to transfer.<br>
     *                  Examples: <code>IN_FLOW | INITIATOR</code> is legal, <code>IN_FLOW | OUT_FLOW</code> is illegal
     *                  and <code>INITIATOR</code> is legal.
     * @return  <code>true</code> when handler runs in the given flow,<br><code>false</code> when not.
     */
    protected boolean isInFlow(final byte flow) {
        // Check has two parts, first checking flow, second checking initiator or responder
        return  ((currentFlow>>>2 & flow>>>2) >= flow>>>2)
             && (((currentFlow&0x03) & (flow&0x03)) >= (flow&0x03));
    }

    /**
     * Prepares the handler for processing of the message. Checks if the handler is run in the correct flow and
     * creates a correctly named {@link Log}.
     * <p>NOTE: To prevent sub classes from overriding this method it is declared final.
     *
     * @param mc            The Axis2 {@link MessageContext}. Will be passed onto the implementation for the actual
     *                      processing
     * @return              The result of the message processing, i.e. the result of
     *                      {@link #doProcessing(org.apache.axis2.context.MessageContext)} of the implementation
     * @throws AxisFault    If an error occurs during the processing of the message that should prevent further
     *                      processing. Note that this will stop processing of the complete flow and may leave message
     *                      units in an undefined state!
     */
    public final InvocationResponse invoke(final MessageContext mc) throws AxisFault {
        // Determine which flow the handler currently runs is
        if (mc.isServerSide()) {
            // Running serverside means Holodeck B2B acts as responder
            currentFlow = RESPONDER;
            currentFlowName = "RESPONDER_";
        } else {
            currentFlow = INITIATOR;
            currentFlowName = "INITIATOR_";
        }

        switch (mc.getFLOW()) {
            case MessageContext.IN_FLOW :
                currentFlowName += "IN_FLOW"; currentFlow |= IN_FLOW;
                break;
            case MessageContext.IN_FAULT_FLOW :
                currentFlowName += "IN_FAULT_FLOW"; currentFlow |= IN_FAULT_FLOW;
                break;
            case MessageContext.OUT_FLOW :
                currentFlowName += "OUT_FLOW"; currentFlow |= OUT_FLOW;
                break;
            case MessageContext.OUT_FAULT_FLOW :
                currentFlowName += "OUT_FAULT_FLOW"; currentFlow |= OUT_FAULT_FLOW;
                break;
        }

        // Check if running in correct flow (check has two parts, first check the for IN or OUT flow,
        //   then check whether message is initiated by Holodeck B2B or response)
        if (!runningInCorrectFlow()) {
            // This is handler is not supposed to run in the current flow
            return InvocationResponse.CONTINUE;
        }

        // Running in correct flow, create a logger
        log = LogFactory.getLog("org.holodeckb2b.msgproc." + currentFlowName + "." + this.getClass().getSimpleName());

        // Do actual processing in implementation
        try {
            log.trace("Start processing");
            final InvocationResponse result = doProcessing(mc);
            log.trace("End processing");
            return result;
        } catch (final Throwable t) {
            // Unhandled exception during processing, should not happen!
            log.fatal("An unhandled exception occurred while processing the message! Details: " + t.getMessage());
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
        if (runningInCorrectFlow())
            doFlowComplete(mc);
    }

    /**
     * Checks if the handler is running in the correct flow. The check has two parts, first check the for IN or OUT
     * flow, then check whether message is initiated by Holodeck B2B or is a response.
     *
     * @return  <code>true</code>   When running in the correct flow, or<br>
     *          <code>false</code>  otherwise
     */
    private boolean runningInCorrectFlow() {
        return ((currentFlow>>>2 & inFlows()>>>2) > 0)
               && (((currentFlow&0x03) & (inFlows()&0x03)) >= (inFlows()&0x03));
    }

    /**
     * Abstract method that implementation should use to return the flows in which the handler should run. This is used
     * to prevent the handler from running in an incorrect flow.
     * <p>Implementations SHOULD use the constants defined in this abstract class to express flows. Flows are indicated
     * as bits, so to indicate that a handler runs in multiple flows the or operations must be used.<br>
     * For example: If the handler should run in both the in and fault in flow this method should return:
     * <code>IN_FLOW | IN_FAULT_FLOW</code><br>
     * By default the handler will run independent of whether Holodeck B2B is the initiator of or responder in the
     * message exchange on this Leg. If the handler should only run in a specific flow, for example only when a message
     * is sent by Holodeck B2B in a request to another MSH this must be indicated using the <code>OUT_FLOW | INITIATOR
     * </code> flag.
     * <p><b>NOTE 1: </b>The handler still MUST be defined in <b>module.xml</b> to add it to the correct flow.<br>
     * <b>NOTE 2: </b>As ebMS error message units can be combined with SOAP Fault ebMS messages it is RECOMMENDED that
     * handlers run in both the regular as well as the fault flow.
     *
     * @return  The flows in which the handler should run
     */
    protected abstract byte inFlows();

    /**
     * Abstract method that implementations must use to do the actual message processing.
     *
     * @param mc            The Axis2 {@link MessageContext} of the processed message
     * @return              How to continue processing of the message. If message processing should not continue, it is
     *                      RECOMMENDED to throw an AxisFault instead of returning <code>InvocationResponse.ABORT</code>
     *                      because this enables sending a response.
     * @throws Exception    If an error occurs during the processing of the message that should prevent further
     *                      processing. Note that this will stop processing of the complete flow and may leave message
     *                      units in an undefined state! Also ensure that all information needed for a response is set
     *                      in the message context to make it available for handlers in the fault flow!
     */
    protected abstract InvocationResponse doProcessing(MessageContext mc) throws Exception;

    /**
     * Runs when the execution of the flow is completed and if the handler is executed during that flow. This method
     * is always executed independent of the flow's processing result. It can be used to check the processing result
     * and take action action.
     * <p>NOTE: It is possible that not all handlers in the flow have been executed when this method is called, so this
     * method should not depend on execution of handlers later in the flow.
     * <p>NOTE: A default no-op implementation is provided so sub classes only need to override if action is required.
     *
     * @param mc    The current message context
     */
    protected void doFlowComplete(final MessageContext mc) {}
}
