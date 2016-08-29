/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

/**
 * Indicates a problem that occurred in the processing of a {@link IMessageProcessingEvent}.
 * This exception can be thrown because a event processing handler can not be successfully instantiated, i.e. the factory class can not be
 * initialized or during the actual handling of the of a processing event.
 *
 * @author Jerry Dimitriou <jerouris at unipi.gr>
 * @see IMessageProcessingEventHandler
 * @see IMessageProcessingEventHandlerFactory
 */
public class MessageProccesingEventHandlingException extends Exception {

    private static final long serialVersionUID = -5368707258538227063L;

    public MessageProccesingEventHandlingException() {
        super();
    }

    public MessageProccesingEventHandlingException(final String message) {
        super(message);
    }

    public MessageProccesingEventHandlingException(final String message, final Exception cause) {
        super(message, cause);
    }
    
    public MessageProccesingEventHandlingException(final Exception cause) {
        super(cause);
    }
}
