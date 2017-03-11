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
package org.holodeckb2b.axis2;

import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;

/**
 * This class contains helper functions related to the Axis2 framework
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public final class Axis2Utils {

    /**
     * Name for the anonymous AxisService that uses correct Axis2 operation
     */
    private static final String HB2B_ANON_SVC = "hb2b:axis2utils:anon_svc";

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

    /**
     * Converts the SOAP Envelope element from the Axis2 representation to the standard DOM representation.
     *
     * @param mc The MessageContext representing the SOAP message
     * @return A {@link Document} object that represents to the SOAP envelope element contained in the message, or<br>
     * <code>null</code> when the SOAP envelope can not be converted to a standard DOM representation
     */
    public static Document convertToDOM(final MessageContext mc) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mc.getEnvelope().serialize(baos);
            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(bais);
        } catch (final Exception e) {
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
    public static SOAPEnvelope convertToAxiom(final Document document) {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLUtils.outputDOM(document.getDocumentElement(), os, true);
            final ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());

            final SOAPModelBuilder stAXSOAPModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(bais, null);
            final SOAPEnvelope env = stAXSOAPModelBuilder.getSOAPEnvelope();
            env.build();
            return env;
        } catch (final Exception e) {
            // If anything goes wrong converting the document, just return null
            return null;
        }
    }

    /**
     * Create an axisService with one (anonymous) operation for OutIn MEP but that does accept an empty responses.
     *
     * @return The configured anonymous service
     */
    public static AxisService createAnonymousService() {
        final AxisService axisService = new AxisService(HB2B_ANON_SVC + ":" + UUID.randomUUID());

        final OutOptInAxisOperation outInOperation = new OutOptInAxisOperation(ANON_OUT_IN_OP);
        axisService.addOperation(outInOperation);

        return axisService;
    }
}
