/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.pmode.helpers;

import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.security.tokens.UsernameToken;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UsernameTokenConfig extends UsernameToken
        implements IUsernameTokenConfiguration {

    private String          username;
    private String          password;
    private UTPasswordType  pwdType;
    private boolean         includeNonce;
    private boolean         includeCreated;

    /**
     * Creates a new <code>UsernameToken</code> based on a WSS4J {@link UsernameTokenPrincipal} that is read from
     * the SOAP message.
     *
     * @param principal The data to construct the UsernameToken
     */
    public UsernameTokenConfig(UsernameTokenPrincipal principal) {
        super(principal);
    }

    public UsernameTokenConfig() {
        super(new UsernameTokenPrincipalForTest());
    }

        @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public UTPasswordType getPasswordType() {
        return pwdType;
    }

    public void setPasswordType(final UTPasswordType pwdType) {
        this.pwdType = pwdType;
    }

    @Override
    public boolean includeNonce() {
        return includeNonce;
    }

    public void setIncludeNonce(final boolean includeNonce) {
        this.includeNonce = includeNonce;
    }

    @Override
    public boolean includeCreated() {
        return includeCreated;
    }

    public void setIncludeCreated(final boolean includeCreated) {
        this.includeCreated = includeCreated;
    }

}

class UsernameTokenPrincipalForTest implements UsernameTokenPrincipal {

    @Override
    public boolean isPasswordDigest() {
        return false;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public byte[] getNonce() {
        return new byte[0];
    }

    @Override
    public String getCreatedTime() {
        return null;
    }

    @Override
    public String getPasswordType() {
        return null;
    }

    @Override
    public boolean equals(Object another) {
        return false;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }
}
