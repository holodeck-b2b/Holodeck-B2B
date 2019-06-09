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
package org.holodeckb2b.common.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;

/**
 * This class contains helper functions related to the Axis2 framework
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public final class Axis2Utils {

	/**
	 * The parameter name to be used in the Service description to specify which <i>Message Builder</i> should be used
	 * to prepare the message processing 
	 */
	public static final String SVC_BUILDER_PARAM = "hb2b:builder";
	
	/**
	 * Checks if the executed Service has specified its own <i>Message Builder</i> and if so returns an instance of that
	 * builder.   
	 * 
	 * @param service		The executed Service
	 * @return				An instance of the custom message builder that should be used if specified,<br>
	 * 						<code>null</code> if no custom builder is specified
	 * @throws AxisFault	When the specified message builder could not be instantiated
	 */
	public static Builder getBuilderFromService(final AxisService service) throws AxisFault {
		Builder msgBuilder = null;
		final Parameter builderParameter = service.getParameter(SVC_BUILDER_PARAM);
		if (builderParameter != null) {
			try {
				final String builderClass = (String) builderParameter.getValue();
				msgBuilder = (Builder) Class.forName(builderClass).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
				throw new AxisFault("Specified builder (" + builderParameter.getValue() + ") not available!");
			}		
		}
		return msgBuilder;
	}
	
    /**
     * Creates the {@link MessageContext} for the response to message currently being processed.
     *
     * @param reqContext The MessageContext of the received message
     * @return The MessageContext for the response message
     */
    public static MessageContext createResponseMessageContext(final MessageContext reqContext) {

        try {
            MessageContext resCtx = null;

            // First try to get the context for the response from the OperationContext
            final OperationContext opContext = reqContext.getOperationContext();
            if (opContext != null) {
                resCtx = opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            }

            // If that fails, construct a new context
            if (resCtx == null) {
                resCtx = MessageContextBuilder.createOutMessageContext(reqContext);
                resCtx.getOperationContext().addMessageContext(resCtx);
            }

            return resCtx;
        } catch (final AxisFault af) {
            // Somewhere the construction of the new MessageContext failed
            return null;
        }
    }
}
