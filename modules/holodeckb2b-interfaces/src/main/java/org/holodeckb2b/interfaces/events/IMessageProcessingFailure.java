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
package org.holodeckb2b.interfaces.events;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;

/**
 * Is a generic <i>message processing event</i> to indicate that a problem occurred during the processing of a message
 * unit. All events that indicate failures in processing of a message extend this interface, so it can be used as a
 * generic filter when configuring event handling.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public interface IMessageProcessingFailure extends IMessageProcessingEvent {

	 /**
     * Gets the exception that caused the processing failure. In most cases where the processing of a message unit fails
     * the cause is an exception. Based on the context descendant interfaces may specify a more specific exception type.
     * Also there can be circumstances that the failure is not caused by an exception but a "regular" failure, for
     * example when a message fails to meet certain conditions. In such cases this method may return <code>null</code>
     * and other methods should be available to get details on the failure.
     *
     * @return  The {@link Throwable} that caused the failure, may be <code>null</code> in case the failure was not
     * 			caused by an exception but a "regular" failure.
     */
	Throwable getFailureReason();
}
