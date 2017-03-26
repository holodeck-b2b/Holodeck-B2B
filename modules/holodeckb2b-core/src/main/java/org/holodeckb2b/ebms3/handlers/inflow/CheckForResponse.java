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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;

/**
 * Is the last handler in the in pipe line, called the <i>Message Receiver</i> in Axis2
 * architecture. In Holodeck B2B its only job is to check whether a HTTP response
 * is required and if so start it.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CheckForResponse extends AbstractMessageReceiver {

    /**
     * Log facility.
     */
    protected   Log     log = LogFactory.getLog("org.holodeckb2b.msgproc." + this.getClass().getSimpleName());

    /**
     * Checks whether a HTTP response has to be sent back and if so, triggers it.
     * <p>Whether a response is needed is indicated by the message context property
     * {@see MessageContextProperties#RESPONSE_REQUIRED}. Only if its value is
     * <code>true</code> a response will be send.
     *
     * @param messageCtx    The current (incoming) message context
     * @throws AxisFault    Never
     */
    @Override
    protected void invokeBusinessLogic(final MessageContext messageCtx) throws AxisFault {

        log.debug("Check if a response must be sent");
        final Boolean responseRequired = (Boolean) messageCtx.getProperty(MessageContextProperties.RESPONSE_REQUIRED);
        if (responseRequired != null && responseRequired.booleanValue()) {
            log.debug("There is a response to be sent, prepare message context and start send process");
            AxisEngine.send(createResponseContext(messageCtx));
        } else {
            log.debug("No response required, done processing");
        }
    }

    /**
     * Creates a new {@see MessageContext} for the response.
     *
     * @param msgContext    The current, i.e. in flow, message context
     * @return              The new message context for the outgoing flow
     * @throws AxisFault    When an error occurs during the creation of the response
     *                      message context
     */
    protected MessageContext createResponseContext(final MessageContext msgContext) throws AxisFault {
        final MessageContext outMsgContext = MessageContextBuilder.createOutMessageContext(msgContext);
        outMsgContext.getOperationContext().addMessageContext(outMsgContext);

        replicateState(msgContext);

        return outMsgContext;
    }
}
