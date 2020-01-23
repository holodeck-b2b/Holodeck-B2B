/*
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
package org.holodeckb2b.ebms3.security.results;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.holodeckb2b.common.security.results.AbstractSecurityProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.UTPasswordType;

/**
 * Is the security provider's implementation of {@link IUsernameTokenProcessingResult} containing the result of
 * processing a username token in a message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class UsernameTokenProcessingResult extends AbstractSecurityProcessingResult
                                                implements IUsernameTokenProcessingResult {
    private final String username;
    private final String password;
    private final UTPasswordType passwordType;
    private final String nonce;
    private final String created;

    /**
     * Creates a new <code>UsernameTokenProcessingResult</code> instance to indicate that there was a problem in
     * processing the username token part.
     *
     * @param target    The SOAP actor/role that was targeted
     * @param failure   Exception indicating the problem that occurred
     */
    public UsernameTokenProcessingResult(final SecurityHeaderTarget target, final SecurityProcessingException failure) {
        super(target, failure);
        this.username = null;
        this.password = null;
        this.passwordType = null;
        this.nonce = null;
        this.created = null;
    }

    /**
     * Creates a new <code>UsernameTokenProcessingResult</code> instance to indicate that processing of the username
     * token part completed successfully.
     *
     * @param target    The SOAP actor/role that was targeted
     * @param username  The username included in the token
     * @param password  The password as included in the token, may be in digested form
     * @param pwdType   Indicating how the password is included
     * @param nonce     The nonce value
     * @param created   The timestamp the username token was created, as the String as included in the XML
     */
    public UsernameTokenProcessingResult(final SecurityHeaderTarget target, final String username,
                                         final String password, final UTPasswordType pwdType, final String nonce,
                                         final String created) {
        super(target);
        this.username = null;
        this.password = null;
        this.passwordType = null;
        this.nonce = null;
        this.created = null;
    }

    /**
     * Creates a new <code>UsernameTokenProcessingResult</code> instance to indicate that processing of the username
     * token part was completed successfully.
     *
     * @param target        The target of the WS-Security header this username token is part of
     * @param wss4jToken    The WSS4J token that includes result of processing the username token
     */
    public UsernameTokenProcessingResult(final SecurityHeaderTarget target, final UsernameToken wss4jToken) {
        super(target);
        this.username = wss4jToken.getName();
        this.password = wss4jToken.getPassword();
        this.passwordType =  WSConstants.PASSWORD_DIGEST.equals(wss4jToken.getPasswordType()) ? UTPasswordType.DIGEST :
                                                                                                UTPasswordType.TEXT;
        this.nonce = wss4jToken.getNonce();
        this.created = wss4jToken.getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UTPasswordType getPasswordType() {
        return passwordType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNonce() {
        return nonce;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreatedTimestamp() {
        return created;
    }

}
