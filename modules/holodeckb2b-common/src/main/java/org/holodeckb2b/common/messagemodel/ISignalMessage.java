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
package org.holodeckb2b.common.messagemodel;

/**
 * Represents an ebMS Signal Message message unit. Because the information about an actual signal message depends on the 
 * type of signal message this interface does not define any methods. Although it does not define any methods to 
 * implement the interface is defined to closely align the object structure with the message structure as defined in the 
 * ebMS specification.
 * <p>The type of a signal message is determined by the child element(s) of the <code>eb:SignalMessage</code> element.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IErrorMessage
 * @see IPullRequest
 * @see IReceipt
 */
public interface ISignalMessage extends IMessageUnit {   
}
