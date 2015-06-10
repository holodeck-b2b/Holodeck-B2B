/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.security;

/**
 * Represents the authentication info that can be included in an ebMS message and should be used to authorize the sender
 * of the message.
 * <p>Note that this information is provided for the complete message and applies to all message units contained in the
 * message. It is therefor important that the P-Modes that configure the processing of the individual message units in 
 * the message share the same settings to avoid conflicts.
 * <p>Because the format for transferring this info is not specified in detail by the ebMS specifications this interface 
 * is just a placeholder to indicate such information can / should be available.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IAuthenticationInfo  {
}
