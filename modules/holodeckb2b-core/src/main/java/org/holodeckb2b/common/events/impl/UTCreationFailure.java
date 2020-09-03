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
package org.holodeckb2b.common.events.impl;

import org.holodeckb2b.interfaces.events.security.IUTCreationFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is the implementation of {@link IUTCreationFailure} to indicate that the creation of a username token security
 * header in the message containing a submitted message unit failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class UTCreationFailure extends AbstractSecurityProcessingFailureEvent implements IUTCreationFailure {

    /**
     * The role/actor targeted by the username token which could not be added
     */
    private SecurityHeaderTarget    target;

    /**
     * Creates a new <code>UTCreationFailure</code> for the given message unit and failure reason.
     *
     * @param subject   The message unit
     * @param target    The target of the username token security header
     * @param reason    The reason why the encryption failed
     */
    public UTCreationFailure(IMessageUnit subject, SecurityHeaderTarget target,
                                 SecurityProcessingException reason) {
        super(subject, reason);
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityHeaderTarget getTargetedRole() {
        return target;
    }
}
