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
package org.holodeckb2b.core.axis2;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.ClientUtils;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.MessageProcessingContext;


/**
 * Is a special Axis2 {@link AxisOperation} implementation that supports the Out In MEP but does not require a response
 * message and which allows to specify  
 * 
 * <p>This class extends {@link OutInAxisOperation} to return a different {@link OperationClient} implementation.
 * Although there is just one method that changes in the <code>OperationClient</code> it must be copied from the super
 * class a an inner class can not be extended.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class OutOptInAxisOperation extends OutInAxisOperation {

    /**
     * Create a new instance of the OutOptInAxisOperation
     */
    public OutOptInAxisOperation(final QName name) {
        super(name);
        setMessageExchangePattern(WSDL2Constants.MEP_URI_OUT_OPTIONAL_IN);
    }

    /**
     * Returns the MEP client for an Out-IN operation that accepts an empty response. To use the client, you must call
     * addMessageContext() with a message context and then call execute() to execute the client.
     *
     * @param sc      The service context for this client to live within. Cannot be
     *                null.
     * @param options Options to use as defaults for this client. If any options are
     *                set specifically on the client then those override options
     *                here.
     */
    @Override
    public OperationClient createClient(final ServiceContext sc, final Options options) {
        return new OutOptInAxisOperationClient(this, sc, options);
    }

    /**
     * The client to handle the MEP. This is a copy of <code>OutInAxisOperationClient<code> inner class of {@link
     * OutInAxisOperation} with an adjusted {@link #handleResponse(MessageContext)} method.
     */
    protected class OutOptInAxisOperationClient extends OperationClient {

        private final Log log = LogFactory.getLog(OutOptInAxisOperationClient.class);

        public OutOptInAxisOperationClient(final OutInAxisOperation axisOp, final ServiceContext sc, 
        								   final Options options) {
            super(axisOp, sc, options);
        }

        /**
         * Adds message context to operation context, so that it will handle the logic correctly if the OperationContext
         * is null then new one will be created, and Operation Context will become null when some one calls reset().
         *
         * @param msgContext the MessageContext to add
         * @throws AxisFault
         */
        @Override
        public void addMessageContext(final MessageContext msgContext) throws AxisFault {
            msgContext.setServiceContext(sc);
            if (msgContext.getMessageID() == null) {
                setMessageID(msgContext);
            }
            axisOp.registerOperationContext(msgContext, oc);
        }

        /**
         * Returns the message context for a given message label.
         *
         * @param messageLabel : label of the message and that can be either "Out" or "In" and nothing else
         * @return Returns MessageContext.
         * @throws AxisFault
         */
        @Override
        public MessageContext getMessageContext(final String messageLabel) throws AxisFault {
            return oc.getMessageContext(messageLabel);
        }

        /**
         * Executes the MEP. 
         *
         * @param block 		IGNORED BY THIS MEP CLIENT.
         * @throws AxisFault 	if something goes wrong during the execution of the MEP.
         */
        @Override
        public void executeImpl(final boolean block) throws AxisFault {
            if (completed) {
                throw new AxisFault(Messages.getMessage("mepiscomplted"));
            }
            final ConfigurationContext cc = sc.getConfigurationContext();

            // copy interesting info from options to message context.
            final MessageContext mc = oc.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            if (mc == null) {
                throw new AxisFault(Messages.getMessage("outmsgctxnull"));
            }
            prepareMessageContext(cc, mc);

            if (options.getTransportIn() == null && mc.getTransportIn() == null) {
                mc.setTransportIn(ClientUtils.inferInTransport(cc .getAxisConfiguration(), options, mc));
            } else if (mc.getTransportIn() == null) {
                mc.setTransportIn(options.getTransportIn());
            }

            // Send the SOAP Message and receive a response
            MessageContext responseMessageContext = send(mc);
            handleResponse(responseMessageContext);
            completed = true;
        }

        /**
         * Synchronously send the request and receive a response. This relies on the transport correctly connecting the
         * response InputStream!
         *
         * @param msgContext the request MessageContext to send.
         * @return Returns MessageContext.
         * @throws AxisFault Sends the message using a two way transport and waits for a response
         */
        protected MessageContext send(final MessageContext msgContext) throws AxisFault {

        // create the responseMessageContext
            final MessageContext responseMessageContext
                    = msgContext.getConfigurationContext().createMessageContext();

            responseMessageContext.setServerSide(false);
            responseMessageContext.setOperationContext(msgContext.getOperationContext());
            responseMessageContext.setOptions(new Options(options));
            responseMessageContext.setMessageID(msgContext.getMessageID());
            addMessageContext(responseMessageContext);
            responseMessageContext.setServiceContext(msgContext.getServiceContext());
            responseMessageContext.setAxisMessage(axisOp.getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));

            //sending the message
            AxisEngine.send(msgContext);

            responseMessageContext.setDoingREST(msgContext.isDoingREST());
            
            // Copy RESPONSE properties which the transport set onto the request message context when it processed
            // the incoming response received in reply to an outgoing request.
            MessageProcessingContext.getFromMessageContext(msgContext).setParentContext(responseMessageContext);
            // We convert the http headers to lowercase for unambigious processing
            @SuppressWarnings("unchecked")
			final Map<String, String> httpHeaders = (Map<String, String>) 
            												msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
            if (!Utils.isNullOrEmpty(httpHeaders)) {
            		final Map<String, String> lcHeaders = new HashMap<>(httpHeaders.size());
            		httpHeaders.entrySet().forEach(e -> lcHeaders.put(e.getKey().toLowerCase(), e.getValue()));
            		responseMessageContext.setProperty(MessageContext.TRANSPORT_HEADERS, lcHeaders);
            } else
            	responseMessageContext.setProperty(MessageContext.TRANSPORT_HEADERS, httpHeaders);
            
            responseMessageContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE,
                    msgContext.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));

            responseMessageContext.setProperty(MessageContext.TRANSPORT_IN, msgContext
                    .getProperty(MessageContext.TRANSPORT_IN));
            responseMessageContext.setTransportIn(msgContext.getTransportIn());
            responseMessageContext.setTransportOut(msgContext.getTransportOut());
            
            return responseMessageContext;
        }
        
        /**
         * Handles the (optional) response.
         *
         * @param responseMessageContext the active response MessageContext
         * @throws AxisFault if something went wrong
         */
        protected void handleResponse(final MessageContext responseMessageContext) throws AxisFault {  
        	SOAPEnvelope resenvelope = responseMessageContext.getEnvelope(); 
    		final InputStream is = (InputStream) responseMessageContext.getProperty(MessageContext.TRANSPORT_IN);
        	if (resenvelope == null && is != null) {        		
            	@SuppressWarnings("unchecked")
				Map<String, String> httpHeaders = (Map<String, String>) 
         										 responseMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS);
            	final String contentType = httpHeaders.get(HTTPConstants.CONTENT_TYPE.toLowerCase());            	
        		
        		// Check if the Service specifies its own Message Builder
            	final Builder msgBuilder = Axis2Utils.getBuilderFromService(responseMessageContext.getAxisService());
            	try {
					if (msgBuilder != null) {
						log.debug("Using " + msgBuilder.getClass().getSimpleName() 
																		+ " Builder to prepare messaging processing");					
						// The builder SHOULD return a SOAP info-set, but for safety we do an extra check
						final OMElement response = msgBuilder.processDocument(is, contentType, responseMessageContext);
						if (response != null)
							resenvelope = TransportUtils.createSOAPEnvelope(response);
					} else {
			        	// Options object reused above so soapAction needs to be removed so
			            // that soapAction+wsa:Action on response don't conflict
			            responseMessageContext.setSoapAction(null);
	                    resenvelope = TransportUtils.createSOAPMessage(responseMessageContext);
					}
            	} catch (Exception responseBuildError) {
            		// This probably indicates that there was no response, therefore only log
            		log.debug("Execption in building response message: " + responseBuildError.getMessage());
            	}
                responseMessageContext.setEnvelope(resenvelope);
            }
            if (resenvelope != null) {
                AxisEngine.receive(responseMessageContext);
                if (responseMessageContext.getReplyTo() != null) {
                    sc.setTargetEPR(responseMessageContext.getReplyTo());
                }
            }
        }
        
    }        	    
}

