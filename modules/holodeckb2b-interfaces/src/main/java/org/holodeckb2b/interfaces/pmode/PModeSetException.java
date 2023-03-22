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
package org.holodeckb2b.interfaces.pmode;

/**
 * Is an exception that signals an issue when an operation is performed on a {@link IPModeSet}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PModeSetException extends Exception {

	/**
     * Default constructor creates the exception without specifying details why it was caused.
     */
    public PModeSetException() {
        super();
    }

    /**
     * Creates a new exception with a description of the reason it was thrown.
     *
     * @param message	description of the reason that caused the exception
     */
    public PModeSetException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception with a description and a related exception that were the reason it was thrown
     *
     * @param message	description of the reason that caused the exception
     * @param cause		the exception that caused the exception
     */
    public PModeSetException(final String message, final Throwable cause) {
    	super(message, cause);
    }
}
