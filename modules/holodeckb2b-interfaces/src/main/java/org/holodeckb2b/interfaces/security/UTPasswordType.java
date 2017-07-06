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

/**
 * Enumeration defining the supported password types as defined in the Web Services Security Username Token Profile
 * Version 1.1.1, section 3.1. The enumeration values also provide the URI that is included in the security header of
 * the message using the {@link #URI()} method.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public enum UTPasswordType {
    /**
     * Indicates the password is included in clear text. NOT RECOMMENDED!
     */
    TEXT("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText"),

    /**
     * Indicates the password is included as a digest, optionally including nonce and/or creation timestamp
     */
    DIGEST("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest");

    private String uri;

    private UTPasswordType(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the URI that identifies this password type in the WS-Security header.
     *
     * @return The URI identifying this password type
     */
    public String URI() {
        return uri;
    }
}
