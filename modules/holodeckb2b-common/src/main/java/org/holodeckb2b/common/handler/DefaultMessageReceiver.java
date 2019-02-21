/*
 * Copyright (C) 2018 The Holodeck B2B Team, Sander Fieten
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
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.MessageContextBuilder;

/**
 * Is Holodeck B2B's default implementation of the Axis2 {@link MessageReceiver} interface. It checks whether a response
 * should be send and if so sets up the outgoing message context and triggers sending of the response. If a messaging 
 * protocol requires specific processing to prepare the outgoing message context it should use a descendant 
 * implementation and override the {@link #prepareOutMessageContext(MessageContext)} method.   
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public class DefaultMessageReceiver implements MessageReceiver {
	
    @Override
    public void receive(MessageContext messageCtx) throws AxisFault {
    	final MessageProcessingContext hb2bCtx = MessageProcessingContext.getFromMessageContext(messageCtx);
    	if (hb2bCtx.responseNeeded()) {
            final MessageContext outMsgContext = MessageContextBuilder.createOutMessageContext(messageCtx);
            // Copy the HB2B message processing context to the outgoing context
            hb2bCtx.setParentContext(outMsgContext);            
            // Handle protocol specific requirement (if any)
            prepareOutMessageContext(outMsgContext);
            // Start the outgoing flow
            AxisEngine.send(outMsgContext);
        }    	
    }	
    
    /**
     * This method can be used to implement specific initialisation of the out message context if needed.
     * 
     * @param outMsgContext		the new Axis2 message context for the response flow 
     * @throws AxisFault when the response context cannot be initialised
     */
    protected void prepareOutMessageContext(final MessageContext outMsgContext) throws AxisFault {}
}
