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

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.dispatchers.AbstractOperationDispatcher;

/**
 * Is an Axis2 <i>request dispatcher</i> to find the {@link AxisOperation} based on the target endpoint URL. It uses the
 * complete path part of the URL to find the operation instead of only the first name after the service name.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.2.0
 */
public class RequestURIOperationDispatcher extends AbstractOperationDispatcher {

	@Override
	public AxisOperation findOperation(AxisService service, MessageContext messageContext) throws AxisFault {

		EndpointReference toEPR = messageContext.getTo();
		if (toEPR != null) {
			String filePart = toEPR.getAddress();
			String opName = getOperationName(filePart, service.getName());

			return opName != null ? service.getOperation(new QName(opName)) : null;
		} else {
			return null;
		}
	}

	@Override
	public void initDispatcher() {
		init(new HandlerDescription(RequestURIOperationDispatcher.class.getSimpleName()));
	}

    private String getOperationName(String path, String serviceName) {
        if (path == null || serviceName == null) {
            return null;
        }
        int idx = path.lastIndexOf(serviceName + "/");
        String operationName = idx != -1 ? path.substring(idx + serviceName.length() + 1) : null;

        if (operationName != null) {
            //remove everything after '?'
            int queryIndex = operationName.indexOf('?');
            if (queryIndex > 0) {
                operationName = operationName.substring(0, queryIndex);
            }
        }
        return operationName;
    }

}
