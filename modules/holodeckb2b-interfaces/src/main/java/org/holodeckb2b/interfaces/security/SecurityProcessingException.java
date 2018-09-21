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
 * Is used to indicate that an error occurred in a generic function of the <i>security provider</i> or in a function
 * related to the processing of (part of) the WS-Security header of a message.
 * <p>NOTE 1: This exception is used both for reporting <i>internal</i> errors in components of the security provider and
 * to report problems with the WS-Security headers themselves. In the first case the security provider MUST <b>throw</b>
 * the exception, in the second case it SHOULD include it in the applicable {@link ISecurityProcessingResult}
 * implementation.
 * <p>NOTE 2: When processing the WS-Security headers of a message the <i>security provider</i> MAY check that the
 * message conforms to defined security policies. If an a violation of the security policies is detected the exception
 * that is used to report problems with the processing of the header will have its <i>isPolicyViolation</i> indicator
 * set to <code>true</code>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 * @see ISecurityProvider
 */
public class SecurityProcessingException extends Exception {

    /**
     * Indicates whether the problem constitutes a violation of the security policies
     */
    private boolean isPolicyViolation = false;

    public SecurityProcessingException() {
        super();
    }

    public SecurityProcessingException(final String message) {
        super(message);
    }

    public SecurityProcessingException(final String message, final boolean isPolicyError) {
        super(message);
        this.isPolicyViolation = isPolicyError;
    }

    public SecurityProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SecurityProcessingException(final String message, final Throwable cause, final boolean isPolicyError) {
    	super(message, cause);
    	this.isPolicyViolation = isPolicyError;
    }
    
    /**
     * Indicates whether this found error in processing of the WS-Security headers is caused by a violation of the
     * security policies.
     * <p>NOTE 1: This only applies to exceptions included in {@link ISecurityProcessingResult} objects.
     * <p>NOTE 2: This indicator is only used when the <i>security provider</i> implements security policies.
     *
     * @return <code>true</code> if this error constitutes a violation of the security policies,<br>
     *         <code>false</code> otherwise
     */
    public boolean isPolicyViolation() {
        return isPolicyViolation;
    }
}
