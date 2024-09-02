/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.events.impl;

import org.holodeckb2b.interfaces.events.security.ISecurityCreationFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is the implementation of {@link ISecurityCreationFailure} to indicate that there was a problem creating the security
 * header of a submitted message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public class SecurityCreationFailure extends AbstractSecurityProcessingFailureEvent implements ISecurityCreationFailure {

    /**
     * Creates a new <code>SecurityCreationFailure</code> for the given message unit and failure reason.
     *
     * @param subject   The message unit
     * @param reason    The reason why the security header could not be created
     */
    public SecurityCreationFailure(IMessageUnit subject, SecurityProcessingException reason) {
        super(subject, reason);
    }
}
