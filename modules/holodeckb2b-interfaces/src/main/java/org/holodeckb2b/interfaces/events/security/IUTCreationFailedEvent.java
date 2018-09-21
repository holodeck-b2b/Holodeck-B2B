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
package org.holodeckb2b.interfaces.events.security;

import org.holodeckb2b.interfaces.events.security.ISecurityHeaderCreationFailureEvent;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;

/**
 * Is the <i>message processing event</i> that indicates that a username token could not be added to the message to be
 * sent containing the message unit failed. This event is to inform the business application (or extensions) that a
 * submitted message unit could not be sent because it a required username token could not be added to the message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public interface IUTCreationFailedEvent extends ISecurityHeaderCreationFailureEvent {

    /**
     * Gets the target of Username token WS-Security header which could not be added to the message.
     *
     * @return The target of the Username token header
     */
    SecurityHeaderTarget getTargetedRole();
}
