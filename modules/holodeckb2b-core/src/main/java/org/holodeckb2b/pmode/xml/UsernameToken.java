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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

/**
 * Represents the <code>UsernameToken</code> element in the P-Mode XML document that contains the P-Mode parameters for
 * including a WSS username token in the message. Required elements are the username and password. The other parameters
 * are optional and will use the most secure values as default: digested password including nonce and created elements.
 * <p>The XML element also contains a <code>target</code> attribute that identifies the target of WSS header in which
 * the username token should be added.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Root
public class UsernameToken implements IUsernameTokenConfiguration {

    @Attribute(name = "target", required = false)
    String target;

    @Element (name = "username")
    private String username;

    /**
     *  Represents the password element with type attribute
     */
    static class Password {
        @Text(required = true)
        private String value;

        @Attribute(required = false)
        private String type = "Digest";
    }

    @Element (name = "password")
    private Password password;

    @Element (name = "includeNonce", required = false)
    private Boolean includeNonce = Boolean.TRUE;

    @Element (name = "includeCreated", required = false)
    private Boolean includeCreated = Boolean.TRUE;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password.value;
    }

    @Override
    public UTPasswordType getPasswordType() {
        return "Text".equalsIgnoreCase(password.type) ?  UTPasswordType.TEXT : UTPasswordType.DIGEST;
    }

    @Override
    public boolean includeNonce() {
        return includeNonce;
    }

    @Override
    public boolean includeCreated() {
        return includeCreated;
    }

}
