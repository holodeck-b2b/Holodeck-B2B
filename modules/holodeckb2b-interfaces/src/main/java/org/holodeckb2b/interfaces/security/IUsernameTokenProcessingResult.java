/*
 * Copyright (C) 2017 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.security;

import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;

/**
 * Represents the results of processing of the username token part of the WS-Security header. It provides information on
 * when the token was created, the username, password and whether a nonce was used.
 * <p>NOTE: The security processor only reads the information from the security header. It does not verify whether the
 * information contained in the username token matches to the P-Mode. This verification is performed by the Holodeck B2B
 * Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public interface IUsernameTokenProcessingResult extends ISecurityProcessingResult {

    /**
     * Gets the timestamp that indicates when the username token was created. This corresponds to the <code>
     * wsse:UsernameToken/wsu:Created</code> element.
     *
     * @return  The timestamp when the username token was created
     */
    String getCreatedTimestamp();

    /**
     * Gets the username included in the token. This corresponds to the <code>wsse:UsernameToken/wsse:Usernam</code>
     * element.
     *
     * @return The username included in the username token.
     */
    String getUsername();

    /**
     * Gets the actual value of the password included in the username token. This corresponds to the <code>
     * wsse:UsernameToken/wsse:Password</code> element. Note that depending on the type of password the value may
     * be clear text (password type equals {@link IUsernameTokenConfiguration#PasswordType.TEXT} or a digest.
     *
     * @return The actual password value
     */
    String getPassword();

    /**
     * Gets the indication how the password is included in the username token, i.e. as digest or in clear text. This
     * corresponds to the <code>Type</code> attribute of the <code>wsse:UsernameToken/wsse:Password</code> element.
     *
     * @return  The type of password being provided expressed using the {@link UTPasswordType} enumeration.
     */
    UTPasswordType getPasswordType();

    /**
     * Gets the nonce that is included with the username token. This corresponds to the <code>
     * wsse:UsernameToken/wsse:Nonce</code> element.
     *
     * @return  The nonce value
     */
    String getNonce();
}
