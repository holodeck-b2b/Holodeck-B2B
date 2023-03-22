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
package org.holodeckb2b.interfaces.eventprocessing;

/**
 * Indicates a problem that occurred in the processing of a {@link IMessageProcessingEvent}.
 * This exception can be thrown because a event processing handler can not be successfully instantiated, i.e. the factory class can not be
 * initialized or during the actual handling of the of a processing event.
 *
 * @author Jerry Dimitriou (jerouris at unipi.gr)
 * @see IMessageProcessingEventHandler
 * @see IMessageProcessingEventHandlerFactory
 */
public class MessageProccesingEventHandlingException extends Exception {

    private static final long serialVersionUID = -5368707258538227063L;

    /**
     * Default constructor creates the exception without specifying details why it was caused.
     */
    public MessageProccesingEventHandlingException() {
        super();
    }

    /**
     * Creates a new exception with a description of the reason it was thrown.
     *
     * @param message	description of the reason that caused the exception
     */
    public MessageProccesingEventHandlingException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception with a description and a related exception that were the reason it was thrown
     *
     * @param message	description of the reason that caused the exception
     * @param cause		the exception that caused the event processing failure
     */
    public MessageProccesingEventHandlingException(final String message, final Exception cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with only the related exception that was the reason it was thrown
     *
     * @param cause		the exception that caused the event processing failure
     */
    public MessageProccesingEventHandlingException(final Exception cause) {
        super(cause);
    }
}
