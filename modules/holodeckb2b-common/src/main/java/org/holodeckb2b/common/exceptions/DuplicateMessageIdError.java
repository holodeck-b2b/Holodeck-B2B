/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.exceptions;

/**
 * Signals that a duplicate message id is detected in the database. As specified in the ebMS Core Specifications section
 * 5.2.2.1 message id must be globally unique. This constraint is only checked for messages that should be sent by 
 * Holodeck B2B and not for received messages. It will be raised when a business application submits a message to with
 * a message id that already exists in the message database.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DuplicateMessageIdError extends DatabaseException {
    
    public DuplicateMessageIdError(String dupId) {
        super("Duplicate messageId [" + dupId + " detected in database!");
    }
}
