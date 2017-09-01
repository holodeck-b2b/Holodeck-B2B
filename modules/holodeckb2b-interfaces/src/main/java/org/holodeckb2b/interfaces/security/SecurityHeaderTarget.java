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
 * Enumerates the targets that the WS-Security headers in an ebMS message can be targeted to and which should be handled
 * by the security processor. The ebMS V3 Core Specification defines two WS-Security headers that can be used, see
 * section 7 of the specification for more information.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public enum SecurityHeaderTarget {

    /**
     * Indicates the <i>default</i> security header which has no specific target. This header is used for the signing,
     * encryption and authentication using a username token.
     */
    DEFAULT(null),

    /**
     * Indicates the ebMS specific security header with target "ebms". This header is used for the authorization using a
     * username token.
     */
    EBMS("ebms");

    private String identifier;

    private SecurityHeaderTarget(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the identifier of this target as used in the SOAP header <i>role</i> or <i>actor</i> attribute. If <code>
     * null</code> is returned it means that the header has no specific target.
     *
     * @return The identifier of this target to be used in SOAP header
     */
    public String id() {
        return identifier;
    }
}
