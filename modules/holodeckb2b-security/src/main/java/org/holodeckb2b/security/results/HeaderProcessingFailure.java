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

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.security.Action;
import org.holodeckb2b.security.SecurityHeaderCreator;
import org.holodeckb2b.security.SecurityHeaderProcessor;

/**
 * Is an extension of the {@link SecurityProcessingException} that adds an indication in which security action the
 * exception occurred. This is used to create the correct {@link ISecurityProcessingResult} instances in {@link
 * SecurityHeaderCreator} and {@link SecurityHeaderProcessor}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class HeaderProcessingFailure extends SecurityProcessingException {

    /**
     * Indication in which action the problem occurred
     */
    private final Action  action;

    public HeaderProcessingFailure(final Action failedAction, final Throwable cause) {
        super(null, cause);
        this.action = failedAction;
    }

    /**
     * Gets the action which failed to execute
     *
     * @return The action in which the problem occurred
     */
    public Action getFailedAction() {
        return action;
    }

    /**
     * Gets the error message about the cause of this error by generating a list of causes and error descriptions or if
     * no cause is known returning a "unknown" message.
     *
     * @return Description of what caused the issue, if available.
     */
    @Override
    public String getMessage() {
        return getCause() != null ? Utils.getExceptionTrace(getCause()) : "No error description available";
    }
}
