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
package org.holodeckb2b.interfaces.messagemodel;



import java.util.Collection;

/**
 * Represents the information available of the Error type of signal message.
 * <p>The error signal message unit is used to signal problems that occur while processing other ebMS message units.
 * It is exchanged between two MSHs when a problem is detected with the exchanged messages. Depending on the P-Mode
 * configuration it can however also be used to signal problems to the <i>business</i> application.
 * <p>An error signal message contains one or more <code>eb:Error</code> elements which give detailed information on the
 * errors that occurred. See section 6 of the ebMS Core Specification for more information on error handling and the
 * structure of the error signal message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see ISignalMessage
 * @see IMessageUnit
 */
public interface IErrorMessage extends ISignalMessage {

    /**
     * Gets the details of all errors included in this error signal message.
     *
     * @return  A collection of {@link IEbmsError} objects representing all errors in this error signal.
     */
    public Collection<IEbmsError> getErrors();
}
