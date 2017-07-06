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
 * Is used to indicate that an error occurred while processing (part of) the WS-Security header of a message. This
 * exception should be used only for <i>internal</i> errors in components of the security provider and not to report
 * problems with the WS-Security header. Such issues need to be reported using the applicable {@link
 * ISecurityProcessingResult} implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SecurityProcessingException extends Exception {

    public SecurityProcessingException() {
        super();
    }

    public SecurityProcessingException(final String message) {
        super(message);
    }

    public SecurityProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
