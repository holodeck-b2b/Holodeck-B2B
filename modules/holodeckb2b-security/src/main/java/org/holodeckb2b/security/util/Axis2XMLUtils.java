/**
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.security.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class contains some helper functions to convert XML between the Axiom representation used by the Axis2 library
 * and the more generic DOM representation, used by the WSS4J framework.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public final class Axis2XMLUtils {

    /**
     * Converts the SOAP Envelope element from the Axis2 representation to the standard DOM representation.
     *
     * @param mc The MessageContext representing the SOAP message
     * @return A {@link Document} object that represents to the SOAP envelope element contained in the message, or<br>
     * <code>null</code> when the SOAP envelope can not be converted to a standard DOM representation
     */
    public static Document convertAxiomSOAPEnvToDOM(final MessageContext mc) {
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
    public static SOAPEnvelope convertDOMSOAPEnvToAxiom(final Document document) {
        try {
            // If no security action is performed the Santuario may not have been initialized which causes the
            // XMLUtils.outputDOM to fail
            if (!org.apache.xml.security.Init.isInitialized())
                org.apache.xml.security.Init.init();
            // The call of the Document.normalizeDocument() method is to fix the exception described here:
            // http://apache-xml-project.6118.n7.nabble.com/Undeclared-namespace-prefix-quot-ds-quot-error-td36346.html
            document.normalizeDocument();

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
     * Converts the DOM representation of an Element to the Axiom one.
     *
     * @param element   The DOM representation of the element
     * @return          The Axiom representation of the same element
     */
    public static OMElement convertDOMElementToAxiom(final Element element) {
        OMXMLParserWrapper parser = OMXMLBuilderFactory.createOMBuilder(element, false);
        OMElement omElement = parser.getDocumentElement();
        omElement.build();
        return omElement;
    }
}
