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
package org.holodeckb2b.security.results;

import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is the abstract base class reporting the results of the security processing done by the security provider. It
 * implements the {@link ISecurityProcessingResult} base interface. Sub classes add the part specific details.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
class AbstractSecurityProcessingResult implements ISecurityProcessingResult {

    private final SecurityHeaderTarget          target;
    private final boolean                       success;
    private final SecurityProcessingException   failureReason;

    /**
     * Initialize the fields of this abstract class to indicate that the security processing failed.
     *
     * @param target    The SOAP actor/role that was targeted
     * @param failure   Exception indicating the problem that occurred
     */
    protected AbstractSecurityProcessingResult(SecurityHeaderTarget target, SecurityProcessingException failure) {
        this.target = target;
        this.success = false;
        this.failureReason = failure;
    }

    /**
     * Initialize the fields of this abstract class to indicate that the security processing completed successfully. The
     * result details are provided in the sub classes.
     *
     * @param target    The SOAP actor/role that was targeted
     */
    protected AbstractSecurityProcessingResult(SecurityHeaderTarget target) {
        this.target = target;
        this.success = true;
        this.failureReason = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityHeaderTarget getTargetedRole() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuccessful() {
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityProcessingException getFailureReason() {
        return failureReason;
    }

}
