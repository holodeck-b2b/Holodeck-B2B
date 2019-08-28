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
 * Is the generic interface to represent the results of processing a part of the message level security of a received
 * message, e.g. the signature or username token. For each security feature that should be processed, a separate sub
 * interface is defined that includes method to get details on the processing specific to that feature.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public interface ISecurityProcessingResult {

    /**
     * Gets the target of WS-Security SOAP header in which the processed feature was included.
     *
     * @return The target of the SOAP header containing the processed feature
     */
    SecurityHeaderTarget getTargetedRole();

    /**
     * Indicates whether the processing of the security was completed successfully.
     *
     * @return  <code>true</code> if this part of the security header could be processed successfully, or<br>
     *          <code>false</code> otherwise
     */
    boolean isSuccessful();

    /**
     * Gets the {@link SecurityProcessingException} that caused the security processing to fail.
     * <p>NOTE: This method should only be called when the {@link #isSuccessfull()} returns <code>false</code>.
     *
     * @return  Exception providing information on the reason why security processing failed
     */
    SecurityProcessingException getFailureReason();
}
