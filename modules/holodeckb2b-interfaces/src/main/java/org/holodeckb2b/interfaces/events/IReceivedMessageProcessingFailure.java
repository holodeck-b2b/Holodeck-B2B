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
 * Is a generic <i>message processing event</i> to indicate that a problem occurred during the processing of a received 
 * message unit. All events that indicate failures in processing of the incoming message extend this interface, so it
 * can be used as a generic filter when configuring event handling. This event however is also implemented by the Core 
 * to inform the back-end or extensions about errors that occur during processing of the message and for which no 
 * specific event is defined. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 */
public interface IReceivedMessageProcessingFailure extends IMessageProcessingEvent {

}
