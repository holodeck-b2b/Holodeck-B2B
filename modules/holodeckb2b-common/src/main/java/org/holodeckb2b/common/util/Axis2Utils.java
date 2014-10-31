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
package org.holodeckb2b.common.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;

/**
 * This class contains helper functions related to the Axis2.
 * 
 * @author safi
 */
public class Axis2Utils {

/**
 * Creates the {@link MessageContext} for the response to message currently being
 * processed.
 * 
 * @param reqContext    The MessageContext of the received message
 * @return  The MessageContext for the response message
 */
public static MessageContext createResponseMessageContext(MessageContext reqContext) {

    try {
    MessageContext resCtx = null;
    
    // First try to get the context for the response from the OperationContext
    OperationContext opContext = reqContext.getOperationContext();
    if (opContext != null) 
        resCtx = opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
    
    // If that fails, construct a new context
    if (resCtx == null) {
        resCtx = MessageContextBuilder.createOutMessageContext(reqContext);
        resCtx.getOperationContext().addMessageContext(resCtx);
    }
    
    return resCtx;
    } catch (AxisFault af) {
        // Somewhere the construction of the new MessageContext failed
        //@todo: Logging!
        return null;
    }
}
}
