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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.kernel.TransportListener;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.util.JavaUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;

/**
 * Handles the dispatch phase of the request. Extends the default Axis2 implementation with additional checks on the
 * "message type" and request methods.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 9.0.0
 */
public class DispatchPhase extends Phase {
	private static final Logger log = LogManager.getLogger();

	public DispatchPhase() {
	}

	public DispatchPhase(String phaseName) {
		super(phaseName);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void checkPostConditions(MessageContext msgContext) throws AxisFault {
		EndpointReference toEPR = msgContext.getTo();
		AxisService service = msgContext.getAxisService();
		AxisOperation operation = msgContext.getAxisOperation();

		if (service == null) {
			log.error("No service found for request {}", ((toEPR != null) ? toEPR.getAddress() : "N/A"));
			AxisFault fault = new AxisFault(
					Messages.getMessage("servicenotfoundforepr", ((toEPR != null) ? toEPR.getAddress() : "")));
			fault.setFaultCode(org.apache.axis2.namespace.Constants.FAULT_CLIENT);
			msgContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, HttpStatus.SC_NOT_FOUND);
			throw fault;
		}

		if (operation == null && JavaUtils.isTrue(service.getParameterValue(AxisService.SUPPORT_SINGLE_OP))) {
			Iterator<AxisOperation> ops = service.getOperations();
			// If there's exactly one, that's the one we want. If there's more, forget it.
			if (ops.hasNext()) {
				operation = ops.next();
				if (ops.hasNext()) {
					operation = null;
				}
			}
			msgContext.setAxisOperation(operation);
		}

		// If we still don't have an operation, fault.
		if (operation == null) {
			log.error("No operation found for request {}", ((toEPR != null) ? toEPR.getAddress() : "N/A"));
			AxisFault fault = new AxisFault(Messages.getMessage("operationnotfoundforepr2",
					((toEPR != null) ? toEPR.getAddress() : ""), msgContext.getWSAAction()));
			fault.setFaultCode(org.apache.axis2.namespace.Constants.FAULT_CLIENT);
			msgContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, HttpStatus.SC_NOT_FOUND);
			throw fault;
		}

		validateTransport(msgContext);

		validateMessageType(msgContext);

		validateRequestMethod(msgContext);

		loadContexts(service, msgContext);

		if (msgContext.getOperationContext() == null) {
			throw new AxisFault(Messages.getMessage("cannotBeNullOperationContext"));
		}

		if (msgContext.getServiceContext() == null) {
			throw new AxisFault(Messages.getMessage("cannotBeNullServiceContext"));
		}

		ArrayList operationChain = msgContext.getAxisOperation().getRemainingPhasesInFlow();
		msgContext.setExecutionChain((ArrayList<Handler>) operationChain.clone());
	}

	/**
	 * Checks whether the request has come in on a valid transport channel as declared in the service configuration.
	 *
	 * @param msgctx the current MessageContext
	 * @throws AxisFault in case the request arrived on a disallowed transport channel
	 */
	private void validateTransport(MessageContext msgctx) throws AxisFault {
		log.trace("Validating transport binding");
		AxisService service = msgctx.getAxisService();
		if (service.isEnableAllTransports()) {
			return;
		} else {
			String incomingTrs = msgctx.getIncomingTransportName();
			// local transport is a special case, it need not be exposed.
			if (Constants.TRANSPORT_LOCAL.equals(incomingTrs))
				return;

			if (!service.getExposedTransports().contains(incomingTrs)) {
				log.error("Invalid transport ({}) used for request to {} service", incomingTrs, service.getName());
				msgctx.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, HttpStatus.SC_NOT_FOUND);
				EndpointReference toEPR = msgctx.getTo();
				throw new AxisFault(
						Messages.getMessage("servicenotfoundforepr", ((toEPR != null) ? toEPR.getAddress() : "")));
			}
		}
	}

	/**
	 * Checks whether the request is of the expected message type, e.g. SOAP/REST.
	 * <p>
	 * The expected message type of the request can be set on the service level by setting the <i>messageType</i>
	 * parameter to either "soap", "soap11", "soap12" or "rest".<br/>
	 * For backward compatibility with older Axis2 configurations, also the <i>disable«Protocol»</i> parameter is
	 * checked to determine correct message type.
	 *
	 * @param msgctx current MessageContext
	 * @throws AxisFault if the request did not come in on a valid binding
	 */
	private void validateMessageType(MessageContext msgctx) throws AxisFault {
		log.trace("Validating message type binding");
		AxisService service = msgctx.getAxisService();

		Parameter msgTypeP = service.getParameter("messageType");
		String svcMessageType = msgTypeP != null && msgTypeP.getParameterType() == Parameter.TEXT_PARAMETER
								? (String) msgTypeP.getValue() : null;

		String reqMessageType = msgctx.isSOAP11() ? "soap11" : !msgctx.isDoingREST() ? "soap12" : "rest";

		if (svcMessageType != null && reqMessageType.startsWith(svcMessageType.toLowerCase()))
			return;

		if (svcMessageType != null
			|| (msgctx.isDoingREST() && JavaUtils.isTrueExplicitly(service
									.getParameter(org.apache.axis2.Constants.Configuration.DISABLE_REST)))
			|| (msgctx.isSOAP11() && JavaUtils.isTrueExplicitly(service
									.getParameter(org.apache.axis2.Constants.Configuration.DISABLE_SOAP11)))
			|| (!msgctx.isSOAP11() && !msgctx.isDoingREST() && JavaUtils.isTrueExplicitly(service
									.getParameter(org.apache.axis2.Constants.Configuration.DISABLE_SOAP12)))) {
			log.error("Invalid message type ({}) used for request to {} service", reqMessageType, service.getName());
			msgctx.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, HttpStatus.SC_BAD_REQUEST);
			throw new AxisFault(Messages.getMessage("bindingDisabled", reqMessageType));
		}
	}

	/**
	 * Checks whether the request used the expected transport method, e.g. http GET/POST.
	 * <p>
	 * The expected transport of the request can be set either on service or the operation level by setting the
	 * "transportMethod" parameter which should contain a comma separated list of allowed transport methods.
	 *
	 * @param msgctx current MessageContext
	 * @throws AxisFault if the request did not come in on a valid binding
	 */
	private void validateRequestMethod(MessageContext msgctx) throws AxisFault {
		log.trace("Validating transport method");
		AxisService service = msgctx.getAxisService();
		AxisOperation operation = msgctx.getAxisOperation();

		String reqMethod = (String) msgctx.getProperty(HTTPConstants.HTTP_METHOD);

		List<String> allowedMethods;

		Parameter allowedMethodsP = operation.getParameter("transportMethod");
		allowedMethods = allowedMethodsP != null && allowedMethodsP.getParameterType() == Parameter.TEXT_PARAMETER
									? Arrays.asList(((String) allowedMethodsP.getValue()).split(",")) : null;

		if (Utils.isNullOrEmpty(allowedMethods)) {
			allowedMethodsP = service.getParameter("transportMethod");
			allowedMethods = allowedMethodsP != null && allowedMethodsP.getParameterType() == Parameter.TEXT_PARAMETER
										? Arrays.asList(((String) allowedMethodsP.getValue()).split(",")) : null;
		}

		if (!Utils.isNullOrEmpty(allowedMethods) && !allowedMethods.contains(reqMethod)) {
			log.error("Invalid transport method ({}) used for request to {} service", reqMethod, service.getName());
			msgctx.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, HttpStatus.SC_METHOD_NOT_ALLOWED);
			throw new AxisFault(Messages.getMessage("bindingDisabled", reqMethod));
		}
	}

	private void loadContexts(AxisService service, MessageContext msgContext) throws AxisFault {
		String scope = service == null ? null : service.getScope();
		ServiceContext serviceContext = msgContext.getServiceContext();

		if ((msgContext.getOperationContext() != null) && (serviceContext != null)) {
			msgContext.setServiceGroupContextId(((ServiceGroupContext) serviceContext.getParent()).getId());
			return;
		}
		if (Constants.SCOPE_TRANSPORT_SESSION.equals(scope)) {
			fillContextsFromSessionContext(msgContext);
		} else if (Constants.SCOPE_SOAP_SESSION.equals(scope)) {
			extractServiceGroupContextId(msgContext);
		}

		AxisOperation axisOperation = msgContext.getAxisOperation();
		OperationContext operationContext = axisOperation.findForExistingOperationContext(msgContext);

		if (operationContext != null) {
			// register operation context and message context
			axisOperation.registerMessageContext(msgContext, operationContext);

			serviceContext = (ServiceContext) operationContext.getParent();
			ServiceGroupContext serviceGroupContext = (ServiceGroupContext) serviceContext.getParent();

			msgContext.setServiceContext(serviceContext);
			msgContext.setServiceGroupContext(serviceGroupContext);
			msgContext.setServiceGroupContextId(serviceGroupContext.getId());
		} else { // 2. if null, create new opCtxt
			if (serviceContext == null) {
				// fill the service group context and service context info
				msgContext.getConfigurationContext().fillServiceContextAndServiceGroupContext(msgContext);
				serviceContext = msgContext.getServiceContext();
			}
			operationContext = serviceContext.createOperationContext(axisOperation);
			axisOperation.registerMessageContext(msgContext, operationContext);
		}

		serviceContext.setMyEPR(msgContext.getTo());
	}

	private void fillContextsFromSessionContext(MessageContext msgContext) throws AxisFault {
		AxisService service = msgContext.getAxisService();
		if (service == null) {
			throw new AxisFault(Messages.getMessage("unabletofindservice"));
		}
		SessionContext sessionContext = msgContext.getSessionContext();
		if (sessionContext == null) {
			TransportListener listener = msgContext.getTransportIn().getReceiver();
			sessionContext = listener.getSessionContext(msgContext);
			if (sessionContext == null) {
				createAndFillContexts(service, msgContext, sessionContext);
				return;
			}
		}
		String serviceGroupName = msgContext.getAxisServiceGroup().getServiceGroupName();
		ServiceGroupContext serviceGroupContext = sessionContext.getServiceGroupContext(serviceGroupName);
		if (serviceGroupContext != null) {
			// setting service group context
			msgContext.setServiceGroupContext(serviceGroupContext);
			// setting Service context
			msgContext.setServiceContext(serviceGroupContext.getServiceContext(service));
		} else {
			createAndFillContexts(service, msgContext, sessionContext);
		}
		ServiceContext serviceContext = sessionContext.getServiceContext(service);
		// found the serviceContext from session context , so adding that into msgContext
		if (serviceContext != null) {
			msgContext.setServiceContext(serviceContext);
			serviceContext.setProperty(HTTPConstants.COOKIE_STRING, sessionContext.getCookieID());
		}
	}

	private void createAndFillContexts(AxisService service, MessageContext msgContext, SessionContext sessionContext)
			throws AxisFault {
		ServiceGroupContext serviceGroupContext;
		AxisServiceGroup axisServiceGroup = service.getAxisServiceGroup();
		ConfigurationContext configCtx = msgContext.getConfigurationContext();
		serviceGroupContext = configCtx.createServiceGroupContext(axisServiceGroup);

		msgContext.setServiceGroupContext(serviceGroupContext);
		ServiceContext serviceContext = serviceGroupContext.getServiceContext(service);
		msgContext.setServiceContext(serviceContext);
		if (sessionContext != null) {
			sessionContext.addServiceContext(serviceContext);
			sessionContext.addServiceGroupContext(serviceGroupContext);
		}
	}

	private static final QName SERVICE_GROUP_QNAME = new QName(Constants.AXIS2_NAMESPACE_URI,
			Constants.SERVICE_GROUP_ID, Constants.AXIS2_NAMESPACE_PREFIX);

	private void extractServiceGroupContextId(MessageContext msgContext) throws AxisFault {
		SOAPHeader soapHeader = msgContext.getEnvelope().getHeader();
		if (soapHeader != null) {
			OMElement serviceGroupId = soapHeader.getFirstChildWithName(SERVICE_GROUP_QNAME);
			if (serviceGroupId != null) {
				msgContext.setServiceGroupContextId(serviceGroupId.getText());
			}
		}
	}
}
