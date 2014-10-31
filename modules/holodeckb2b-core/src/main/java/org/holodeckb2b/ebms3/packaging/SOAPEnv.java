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
package org.holodeckb2b.ebms3.packaging;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.holodeckb2b.common.general.Constants;

/**
 * Is a helper class for handling a SOAPEnv for an ebMS V3 message. 
 * <p>Note: This class is named SOAPEnv to avoid confusion with the SOAPEnvelope 
 * class of the Axis2/Axiom framework.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SOAPEnv {

    /**
     * Enumeration of supported SOAP versions (i.e. that is all versions right now)
     */
    public enum SOAPVersion { SOAP_11, SOAP_12 }

    /**
     * The prefix of the ebMS header block elements.
     */
    private static final String EBMS3_NS_PREFIX = "eb3";
    
    /**
     * Creates a new SOAP Envelope for sending an ebMS 3 message. The created SOAP
     * envelop will already contain a declaration of the ebMS 3 namespace.
     * 
     * @param   v       The SOAP version to use
     * @return  The newly created SOAP envelope
     */
    public static org.apache.axiom.soap.SOAPEnvelope createEnvelope(SOAPVersion v) {
        SOAPFactory omFactory = null;

        // Check which SOAP version to use
        if (v == SOAPVersion.SOAP_11) {
            omFactory = OMAbstractFactory.getSOAP11Factory();
        } else if (v == SOAPVersion.SOAP_12) {
            omFactory = OMAbstractFactory.getSOAP12Factory();
        }
        
        org.apache.axiom.soap.SOAPEnvelope envelope = omFactory.getDefaultEnvelope();
        
        // Declare all namespaces that are needed by default
        envelope.declareNamespace("http://www.w3.org/1999/XMLSchema-instance/", "xsi");
        envelope.declareNamespace("http://www.w3.org/1999/XMLSchema", "xsd");
        envelope.declareNamespace(Constants.EBMS3_NS_URI, EBMS3_NS_PREFIX);
        
        return envelope;
    }
    
    /**
     * Gets an {@see OMNamespace} object for the ebMS 3 namespace for the SOAP
     * envelope the given element is contained in.
     * 
     * @param   e     The element that is contained in the SOAP envelop
     * @return  The {@see OMNamespace} object for the ebMS 3 namespace if it was 
     *          declared in this SOAP message;
     *          <code>null</code> if there is no namespace declared for ebMS 3
     */
    public static OMNamespace getEbms3Namespace(OMElement e) {
        return e.findNamespace(Constants.EBMS3_NS_URI, null);
    }
    
    /**
     * Checks whether this SOAP message is an ebMS V3 message. A SOAP message
     * is an ebMS V3 message when it contains the ebMS header element <code>eb:Messaging</code>
     * 
     * @param   env     The SOAP Envelope of the SOAP message to check
     * @return          <code>true</code> when this is an ebMS V3 message, 
     *                  <code>false</code> if not.
     */
    public static boolean isEbms3Message(SOAPEnvelope env) {
        // Try to get the ebMS header from the SOAP envelope
        OMElement ebMessagingHeader = Messaging.getElement(env);
        // If it returned a object, this is an ebMS message, otherwise it is not
        return (ebMessagingHeader != null);
    }
}
