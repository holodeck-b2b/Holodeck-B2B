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
package org.holodeckb2b.ebms3.constants;

import javax.xml.namespace.QName;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSSecurityEngineResult;

/**
 * Constants used in the security related processing of the messages. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class SecurityConstants {

    /**
     * Standard prefix for message context properties related to security. This is to avoid collision with other
     * context properties
     */
    private static final String PREFIX = "hb2b-sec:";
    
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
     * The QName for the wsu:Id attribute defined in the WS-Security spec
     */
    public static final QName  QNAME_WSU_ID = new QName(WSU_NAMESPACE_URI, "Id");
    
    /**
     * The namespace URI for XML Signatures
     */
    public static final String DSIG_NAMESPACE_URI = "http://www.w3.org/2000/09/xmldsig#";
    
    /**
     * Identifier for the default WSS header (without actor/role attribute). Used to indicate in which security header
     * tokens were found 
     */
    public static final String DEFAULT_WSS_HEADER = "wsse";
    
    /**
     * Identifier for the WSS header targeted to "ebms" actor/role. Used to indicate in which security header tokens 
     * were found 
     */
    public static final String EBMS_WSS_HEADER = "ebms";
    
    /**
     * Indicator whether security headers must be added to this message. 
     */
    public static final String ADD_SECURITY_HEADERS = PREFIX + "create-headers";
    
    /**
     * Key for including the {@link WSSecurityException} in the {@link WSSecurityEngineResult} when the processing of
     * an element in the header fails.
     */
    public static final String WSS_PROCESSING_FAILURE = PREFIX + "wss4j-exception";
    
    /**
     * Identifier for the MessageContext property that indicates that default WS-Sec header was invalid. The value of
     * the property indicates what caused the problem.
     */
    public static final String INVALID_DEFAULT_HEADER = PREFIX + "invalid:" + DEFAULT_WSS_HEADER;

    /**
     * Identifier for the MessageContext property that indicates that WS-Sec header targeted to the "ebms" role was 
     * invalid. The value of the property indicates what caused the problem. Note that currently the only fault cause
     * can be the Username Token.
     */
    public static final String INVALID_EBMS_HEADER = PREFIX + "invalid:" + EBMS_WSS_HEADER;
    
    /**
     * Enumeration of fault causes in processing the WS-Security header.  
     */
    public static enum WSS_FAILURES { DECRYPTION, SIGNATURE, UT, UNKNOWN }
    
    /**
     * Identifier for the MessageContext property that holds all authentication information for the current message.
     */
    public static final String MC_AUTHENTICATION_INFO = PREFIX + "authinfo";
    
    /**
     * Identifier for the WSS UsernameToken included in the default WSS header. 
     */
    public static final String DEFAULT_USERNAMETOKEN = PREFIX + DEFAULT_WSS_HEADER + ":UsernameToken";
    
    /**
     * Identifier for the WSS UsernameToken included in the WSS header targeted to the "ebms" role.
     */
    public static final String EBMS_USERNAMETOKEN = PREFIX + EBMS_WSS_HEADER + ":UsernameToken";
    
    /**
     * Identifier for the WSS Signature info included in the WSS header
     */
    public static final String SIGNATURE = PREFIX + "Signature";
    
    /**
     * Identifier for the WSS encryption info included in the WSS header
     */
    public static final String ENCRYPTION = PREFIX + "Encryption";
    
    /**
     * Identifier for the indicator whether the SOAP Body should be encrypted
     */
    public static final String ENCRYPT_BODY = PREFIX + "encrypt-body";
}
