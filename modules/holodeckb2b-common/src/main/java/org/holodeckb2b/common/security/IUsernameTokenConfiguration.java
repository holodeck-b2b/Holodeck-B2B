/*
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

package org.holodeckb2b.common.security;

/**
 * Defines the configuration of a WSS UsernameToken contained in the security header of the ebMS message. Depending on
 * the direction (incoming or outgoing) of the message the information is used to set or validate the UsernameToken in
 * the WSS header. 
 * <p>As specified in section 7 of the Core Specification there can exist two <code>wsse:UsernameToken</code> elements
 * in the SOAP header, one targeted at the "default" actor/role and one at a specific "ebms" actor/role. The latter
 * can be used for message authorization (see section 7.10 and 7.11). Which element an instance of this interface 
 * configures is determined by the parent class. 
 * <p>Depending on the target (default or ebms) this interface corresponds with the P-Mode parameter groups 
 * PMode.[Initiator|Responder].Authorization or PMode[1].Security.UsernameToken.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IUsernameTokenConfiguration {
    
    /**
     * Returns the username that should be (included) in the token.
     */
    public String getUsername();
    
    /**
     * Returns the password that should be (included) in the token. The password must be returned in clear text for 
     * processing by Holodeck B2B. Implementations MUST take care of proper protection of the password.
     */
    public String getPassword();
    
    /**
     * Enumeration defining the supported password types
     */
    public enum PasswordType { 
        /**
         * Indicates the password is included in clear text. NOT RECOMMENDED!
         */ 
        TEXT,
        
        /**
         * Indicates the password is included as a digest, optionally including nonce and/or creation timestamp
         */
        DIGEST
    }

    /**
     * Constant for the URI used to identify the clear text password type in the WSSE UsernameToken element.
     */
    public static final String PWD_TYPE_TEXT_URI = 
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";
    
    /**
     * Constant for the URI used to identify the digested password type in the WSSE UsernameToken element.
     */
    public static final String PWD_TYPE_DIGEST_URI = 
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest";
    
    
    /**
     * Returns how the password should be included in the token as defined in the <i>WSS UsernameToken Profile</i>.
     * <p>Currently supported are the types defined in version 1.1.1 of the profile: plain text and digest as defined
     * in the enumeration above.
     */
    public PasswordType getPasswordType();
    
    /**
     * Returns indication whether the <code>wsse:Nonce</code> element should be (when sending the message) or is 
     * included (when receiving the message) in the UsernameToken. 
     * <p>This element is used for protecting the password and preventing replay attacks. It is RECOMMENDED to include
     * it in the username token.
     * 
     * @return  <code>true</code> if Nonce element must be included. This means that when validating an incoming 
     *          request a <code>wsse:Nonce</code> MUST be present and is used for detection of replay attacks.
     *          <code>false</code> otherwise. In this case there is no validation performed on the existence of the 
     *          <code>wsse:Nonce</code> element. It MAY occur in the UsernameToken and if it does it will be used to
     *          prevent replay attacks.
     */
    public boolean includeNonce();
    
    /**
     * Returns indication whether the <code>wsu:Created</code> element should be (when sending the message) or is 
     * included (when receiving the message) in the UsernameToken. 
     * <p>This element contains the timestamp when the username token is created. It is used to protect the password
     * and prevent replay attacks.
     * 
     * @return  <code>true</code> if Created element must be included. This means that when validating an incoming 
     *          request a <code>wsu:Created</code> MUST be present and is also used for detection of replay attacks.
     *          <code>false</code> otherwise. In this case there is no validation performed on the existence of the 
     *          <code>wsu:Created</code> element. It MAY occur in the UsernameToken and if it does it will be used to
     *          prevent replay attacks.
     */
    public boolean includeCreated();
}
