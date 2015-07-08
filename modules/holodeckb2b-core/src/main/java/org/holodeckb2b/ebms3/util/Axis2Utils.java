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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.xml.security.utils.XMLUtils;
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
     * @return  A {@link Document} object that represents to the SOAP envelope element contained in the message, or<br>
     *          <code>null</code> when the SOAP envelope can not be converted to a standard DOM representation
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
     * @param document  The standard DOM representation of the SOAP Envelope
     * @return          An {@link SOAPEnvelope} object containing the Axiom representation of the SOAP envelope, or <br>
     *                  <code>null</code> if the conversion fails
     */
    public static SOAPEnvelope convertToAxiom(Document document) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLUtils.outputDOM(document.getDocumentElement(), os, true);
            ByteArrayInputStream bais =  new ByteArrayInputStream(os.toByteArray());

            SOAPModelBuilder stAXSOAPModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder( bais, null);
            SOAPEnvelope env = stAXSOAPModelBuilder.getSOAPEnvelope();
            env.build();
            return env;
        } catch (Exception e) {
            // If anything goes wrong converting the document, just return null
            return null;
        }
    }
}
