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
package org.holodeckb2b.persistency.entities;

import java.io.Serializable;

import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;

/**
 * Represents a general ebMS <b>Signal Message</b> message unit. As each signal message is of a specific type this is an
 * abstract class and each signal type must have its own child class.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public abstract class SignalMessage extends MessageUnit implements Serializable, ISignalMessage {

}
