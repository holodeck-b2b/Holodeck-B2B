/*
 * Copyright (C) 2018 The Holodeck B2B Team.
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

import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;

/**
 * Is the <i>message processing event</i> that indicates that the processing of a user name token in a received message
 * failed. This event is to inform the business application (or extensions) that a message unit was received but
 * can not be delivered due to the invalid user name token. Based on the event the problem might be handled out of band.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IUTProcessingFailure extends ISecurityProcessingFailure, IUTProcessingFailureEvent {

    /**
     * Gets the target of Username token WS-Security header in which processing failed.
     *
     * @return The target of the Username token header
     */
    @Override
	SecurityHeaderTarget getTargetedRole();
}
