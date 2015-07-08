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
package org.holodeckb2b.ebms3.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.xml.security.utils.XMLUtils;
import org.holodeckb2b.common.config.Config;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.w3c.dom.Document;

/**
 * This class contains helper functions related to the Axis2.
 *
 * @author safi
 */
public final class Axis2Utils {

    /**
     * Creates the {@link MessageContext} for the response to message currently being processed.
     *
     * @param reqContext The MessageContext of the received message
     * @return The MessageContext for the response message
     */
    public static MessageContext createResponseMessageContext(MessageContext reqContext) {

        try {
            MessageContext resCtx = null;

            // First try to get the context for the response from the OperationContext
            OperationContext opContext = reqContext.getOperationContext();
            if (opContext != null) {
                resCtx = opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            }

            // If that fails, construct a new context
            if (resCtx == null) {
                resCtx = MessageContextBuilder.createOutMessageContext(reqContext);
                resCtx.getOperationContext().addMessageContext(resCtx);
            }

            return resCtx;
        } catch (AxisFault af) {
            // Somewhere the construction of the new MessageContext failed
            return null;
        }
    }

    /**
     * Converts the SOAP Envelope element from the Axis2 representation to the standard DOM representation.
     *
     * @param mc The MessageContext representing the SOAP message
     * @return A {@link Document} object that represents to the SOAP envelope element contained in the message, or<br>
     * <code>null</code> when the SOAP envelope can not be converted to a standard DOM representation
     */
    public static Document convertToDOM(MessageContext mc) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mc.getEnvelope().serialize(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(bais);
        } catch (Exception e) {
            // If anything goes wrong converting the document, just return null
            return null;
        }
    }

    /**
     * Converts a {@link Document} representation of the SOAP Envelope into a Axiom representation.
     *
     * @param document The standard DOM representation of the SOAP Envelope
     * @return An {@link SOAPEnvelope} object containing the Axiom representation of the SOAP envelope, or <br>
     * <code>null</code> if the conversion fails
     */
    public static SOAPEnvelope convertToAxiom(Document document) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLUtils.outputDOM(document.getDocumentElement(), os, true);
            ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());

            SOAPModelBuilder stAXSOAPModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(bais, null);
            SOAPEnvelope env = stAXSOAPModelBuilder.getSOAPEnvelope();
            env.build();
            return env;
        } catch (Exception e) {
            // If anything goes wrong converting the document, just return null
            return null;
        }
    }

    public static void sendMessage(MessageUnit message, Log log) {
        ServiceClient sc;
        OperationClient oc;

        try {
            log.debug("Prepare Axis2 client to send message");
            sc = new ServiceClient(Config.getAxisConfigurationContext(), null);
            sc.engageModule(Constants.HOLODECKB2B_CORE_MODULE);
            oc = sc.createClient(ServiceClient.ANON_OUT_IN_OP);

            log.debug("Create an empty MessageContext for message with current configuration");
            MessageContext msgCtx = new MessageContext();

            if (message instanceof UserMessage) {
                log.debug("Message to send is a UserMessage");
                msgCtx.setProperty(MessageContextProperties.OUT_USER_MESSAGE, message);
            } else if (message instanceof PullRequest) {
                log.debug("Message to send is a PullRequest");
                msgCtx.setProperty(MessageContextProperties.OUT_PULL_REQUEST, message);
            } else if (message instanceof ErrorMessage) {
                log.debug("Message to send is a ErrorMessage");
                MessageContextUtils.addErrorSignalToSend(msgCtx, (ErrorMessage) message);
            } else if (message instanceof Receipt) {
                log.debug("Message to send is a Receipt");
                MessageContextUtils.addReceiptToSend(msgCtx, (Receipt) message);
            }
            oc.addMessageContext(msgCtx);

            // This dummy EPR has to be provided to be able to trigger message sending. It will be replaced later
            // with the correct URL defined in the P-Mode
            EndpointReference targetEPR = new EndpointReference("http://holodeck-b2b.org/transport/dummy");
            Options options = new Options();
            options.setTo(targetEPR);
            oc.setOptions(options);

            log.debug("Axis2 client configured for sending ebMS message");
        } catch (AxisFault af) {
            // Setting up the Axis environment failed. As it prevents sending the message it is logged as a fatal error 
            log.fatal("Setting up Axis2 to send message failed! Details: " + af.getReason());
            return;
        }

        try {
            log.debug("Start the message send process");
            oc.execute(false);
        } catch (AxisFault af) {
            // An error occurred while sending the message, 
            log.error("An unexpected error occurred while sending the PullRequest with msg-id: "
                    + message.getMessageId());
        } finally {
            try {
                sc.cleanup();
            } catch (AxisFault af) {
                log.error("Clean up of Axis2 context to send message failed! Details: " + af.getReason());
            }
        }
    }
}
