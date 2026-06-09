/*
 * Copyright (C) 2026 The Holodeck B2B Team
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
package org.holodeckb2b.core.axis2;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.dispatchers.AbstractOperationDispatcher;
import org.apache.axis2.kernel.http.HTTPConstants;

/**
 * Is an Axis2 <i>request dispatcher</i> to find the {@link AxisOperation} based on the request method.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.2.0
 */
public class RequestMethodOperationDispatcher extends AbstractOperationDispatcher {

	@Override
	public AxisOperation findOperation(AxisService service, MessageContext messageContext) throws AxisFault {
		String reqMethod = (String) messageContext.getProperty(HTTPConstants.HTTP_METHOD);
		for(Iterator<AxisOperation> ops = service.getOperations(); ops.hasNext();) {
			AxisOperation operation = ops.next();
			Parameter methodsP = operation.getParameter("transportMethod");
			if (methodsP != null && methodsP.getParameterType() == Parameter.TEXT_PARAMETER
				 && Arrays.asList(((String) methodsP.getValue()).split(",")).contains(reqMethod))
				return operation;
		}
		return null;
	}

	@Override
	public void initDispatcher() {
		init(new HandlerDescription(RequestMethodOperationDispatcher.class.getSimpleName()));
	}
}
