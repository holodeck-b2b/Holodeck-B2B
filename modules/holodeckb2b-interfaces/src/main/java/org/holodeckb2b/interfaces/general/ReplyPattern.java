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
package org.holodeckb2b.interfaces.general;


/**
 * Is an enumeration that defines constants for the reply patterns that can be used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public enum ReplyPattern {

    /**
     * The reply should be sent as a response on the back channel of the transport protocol. Of course this pattern
     * is only valid if a two-way transport protocol is used and and the message being replied is received on the first
     * leg of this transport protocol
     */
    RESPONSE,
    /**
     * The reply should be sent as a separate message. Note that this will require additional (P-Mode) configuration to
     * determine the destination of the reply.
     */
    CALLBACK
}
