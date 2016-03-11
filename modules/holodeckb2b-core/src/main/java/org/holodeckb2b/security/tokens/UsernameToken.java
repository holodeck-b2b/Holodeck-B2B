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
package org.holodeckb2b.security.tokens;

import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;

/**
 * Is used to represent a WSS UsernameToken that is included in the security header of the message as an 
 * {@link IAuthenticationInfo} so it can be used for the authentication of the sender of the message.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class UsernameToken implements IAuthenticationInfo {

    private final String username;
    private final String password;
    private final IUsernameTokenConfiguration.PasswordType passwordType;
    private final String nonce;
    private final String created;

    /**
     * Creates a new <code>UsernameToken</code> based on a WSS4J {@link UsernameTokenPrincipal} that is read from
     * the SOAP message.
     * 
     * @param principal     The data to construct the UsernameToken
     */
    public UsernameToken(UsernameTokenPrincipal principal) {
        this.username = principal.getName();
        this.password = principal.getPassword();
        
        if (IUsernameTokenConfiguration.PWD_TYPE_DIGEST_URI.equalsIgnoreCase(principal.getPasswordType()))
            this.passwordType = IUsernameTokenConfiguration.PasswordType.DIGEST;
        else 
            this.passwordType = IUsernameTokenConfiguration.PasswordType.TEXT;
        
        this.nonce = org.apache.commons.codec.binary.Base64.encodeBase64String(principal.getNonce());
        this.created = principal.getCreatedTime();               
    }
    
    /**
     * @return The username included in the username token.
     */
    public String getUsername() {
        return username;
    }

    /**
     * The actual value of the password included in the username token. Depending on the type of password the value may
     * be clear text (password type equals {@link IUsernameTokenConfiguration#PasswordType.TEXT} or a digest. In the
     * latter case you will have to recreate the digest for comparing the password a stored [clear text] value.
     * 
     * @return The actual password value
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return  The type of password being provided expressed using the {@link IUsernameTokenConfiguration#PasswordType}
     *          enumeration.
     */
    public IUsernameTokenConfiguration.PasswordType getPasswordType() {
        return passwordType;
    }

    /**
     * The nonce that is included with the username token. This value is needed if you want to check the the digested 
     * password as you will need to recreate the digest based with this nonce and the original (clear text) password.
     * 
     * @return  The String value of the <code>wsse:Nonce</code> child element of the <code>wsse:UsernameToken</code>
     *          element
     */
    public String getNonce() {
        return nonce;
    }
    
    /**
     * The timestamp that indicates when the username token was created. This value is needed if you want to check the
     * the digested password as you will need to recreate the digest based with this timestamp and the original (clear
     * text) password.
     * 
     * @return  The String value of the <code>wsu:Created</code> child element of the <code>wsse:UsernameToken</code>
     *          element
     */
    public String getCreated() {
        return created;
    }
    
    /**
     * @return  <code>true</code> if the <code>wsse:UsernameToken</code> element included a <code>wsse:Nonce</code> 
     *          child element, <br>
     *          <code>false</code> if not.
     */
    public boolean includesNonce() {
        return (nonce != null && !nonce.isEmpty());
    }

    /**
     * @return  <code>true</code> if the <code>wsse:UsernameToken</code> element included a <code>wsu:Created</code> 
     *          child element, <br>
     *          <code>false</code> if not.
     */
    public boolean includesCreated() {
        return (created != null && !created.isEmpty());
    }    
}
