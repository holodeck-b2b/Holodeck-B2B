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
package org.holodeckb2b.common.delivery;

/**
 * Indicates a problem that occurred in the delivery of a message unit to the business application. This exception can
 * be thrown because a delivery method can not be successfully instantiated, i.e. the factory class can not be 
 * initialized or during the actual delivery of a message.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageDeliverer
 * @see IMessageDelivererFactory
 */
public class MessageDeliveryException extends Exception {
 
    public MessageDeliveryException() {
        super();
    }
    
    public MessageDeliveryException(String message) {
        super(message);
    }
    
    public MessageDeliveryException(String message, Exception cause) {
        super(message, cause);
    }
    
}
