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
package org.holodeckb2b.security;

import javax.xml.namespace.QName;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.holodeckb2b.interfaces.general.EbMSConstants;

/**
 * Constants related to the identification of parts of the security header and messages.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public final class SecurityConstants {

    /**
     * The WS-Security namespace URI
     */
    public static final String WSS_NAMESPACE_URI =
                                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    /**
     * The WS-Security utilities namespace URI
     */
    public static final String WSU_NAMESPACE_URI =
                                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    /**
     * The QName for the <code>wsse:Security</code> element
     */
    public static final QName QNAME_WSS_HEADER = new QName(WSS_NAMESPACE_URI, "Security");

    /**
     * The QName for the wsu:Id attribute defined in the WS-Security spec
     */
    public static final QName  QNAME_WSU_ID = new QName(WSU_NAMESPACE_URI, "Id");

    /**
     * The namespace URI for XML Signatures 1.0
     */
    public static final String DSIG_NAMESPACE_URI = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * The namespace URI for XML Encryption 1.0
     */
    public static final String XENC_NAMESPACE_URI = "http://www.w3.org/2001/04/xmlenc#";

    /**
     * The namespace URI for XML Encryption 1.1
     */
    public static final String XENC11_NAMESPACE_URI = "http://www.w3.org/2009/xmlenc11#";

    /**
     * QName of the WS-Security header <code>ds:Signature</code> child element
     */
    public static final QName SIGNATURE_ELEM = new QName(DSIG_NAMESPACE_URI, "Signature");

    /**
     * QName of the WS-Security header <code>xenc:EncryptedData</code> child element
     */
    public static final QName ENCRYPTED_DATA_ELEM = new QName(XENC_NAMESPACE_URI, "EncryptedData");

    /**
     * QName of the WS-Security header <code>xenc:CipherReference</code> child element
     */
    public static final QName CIPHER_REF_ELEM = new QName(XENC_NAMESPACE_URI, "CipherReference");

    /**
     * QName of the WS-Security header <code>ds:Reference</code> element
     */
    public static final QName REFERENCE_ELEM = new QName(SecurityConstants.DSIG_NAMESPACE_URI, "Reference");

    /**
     * WSS4J identification of the ebMS Messaging header block
     */
    public static final String WSS4J_PART_EBMS_HEADER = "{}{" + EbMSConstants.EBMS3_NS_URI + "}Messaging;";

    /**
     * WSS4J identification of the SOAP Body element in a SOAP 1.1 message
     */
    public static final String WSS4J_PART_S11_BODY = "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;";
    /**
     * WSS4J identification of the SOAP Body element in a SOAP 1.2 message
     */
    public static final String WSS4J_PART_S12_BODY = "{}{http://www.w3.org/2003/05/soap-envelope}Body;";
    /**
     * WSS4J identification of the Username token security header element
     */
    public static final String WSS4J_PART_UT = "{}{" + WSS_NAMESPACE_URI + "}UsernameToken;";
    /**
     * WSS4J identification of the SOAP attachments
     */
    public static final String WSS4J_PART_ATTACHMENTS = "{}cid:Attachments;";
    /**
     * Name of the property in a {@link WSSecurityEngineResult} instance to indicate that processing of the WS-Security
     * header failed
     */
    public static final String WSS4J_FAILURE_INDICATION = "hb2b:def:sec:failure";
}

