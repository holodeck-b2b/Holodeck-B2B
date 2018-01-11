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
package org.holodeckb2b.events.security;

import org.holodeckb2b.events.security.AbstractSecurityProcessingFailureEvent;
import org.holodeckb2b.interfaces.events.security.IUTProcessingFailureEvent;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is the implementation of {@link IUTProcessingFailureEvent} to indicate that the processing of a username token in a
 * received message failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class UTProcessingFailureEvent extends AbstractSecurityProcessingFailureEvent implements IUTProcessingFailureEvent {

    /**
     * The role/actor targeted by the username token which failed to process
     */
    private SecurityHeaderTarget    target;

    /**
     * Creates a new <code>UTProcessingFailureEvent</code> for the given message unit, target of the SOAP header and
     * failure reason.
     *
     * @param subject   The message unit
     * @param target    The target of the header which processing failed
     * @param reason    The reason why the username token processing failed
     */
    public UTProcessingFailureEvent(IMessageUnit subject, SecurityHeaderTarget target,
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
