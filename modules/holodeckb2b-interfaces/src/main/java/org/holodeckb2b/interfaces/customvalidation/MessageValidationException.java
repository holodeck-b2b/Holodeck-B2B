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
package org.holodeckb2b.interfaces.customvalidation;

/**
 * Indicates that a problem occurred in the custom validation of a User Message message unit.
 * <p>It can for example be thrown when the custom validation can not be successfully instantiated, i.e. when the
 * validator's factory can not be initialized, or during the actual execution of the validation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class MessageValidationException extends Exception {

	/**
     * Default constructor creates the exception without specifying details why it was caused.
     */
    public MessageValidationException() {
        super();
    }

    /**
     * Creates a new exception with a description of the reason it was thrown.
     *
     * @param message	description of the reason that caused the exception
     */
    public MessageValidationException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception with a description and a related exception that were the reason it was thrown
     *
     * @param message	description of the reason that caused the exception
     * @param cause		the exception that caused the validation failure
     */
    public MessageValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
