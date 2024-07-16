/*
 * Copyright (C) 2016 The Holodeck B2B Team.
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

import org.holodeckb2b.interfaces.events.IReceivedMessageProcessingFailure;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is a generic <i>message processing event</i> that indicates that there was a problem in the processing of the
 * WS-Security header of a received message unit. Descendent interfaces are defined to indicate in which header the
 * problem occurred.
 * <p>The descendent classes are used by the Holodeck B2B Core to inform the business application (or extensions) that a
 * message unit was received but can not be delivered due to the problem in the security. Based on the event the problem
 * might be handled out of band.<br>
 * Although the Core will always raise specific events this interface can be used when configuring the event handlers in
 * the P-Mode to define one handler for all events related to WS-Security problems.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public interface ISecurityProcessingFailure extends IReceivedMessageProcessingFailure {

    /**
     * Gets the description as given by the <i>Security Provider</i> of what caused the processing of the WS-Security
     * header to fail.
     *
     * @return  The {@link SecurityProcessingException} that caused the failure.
     */
	@Override
	SecurityProcessingException getFailureReason();
}
