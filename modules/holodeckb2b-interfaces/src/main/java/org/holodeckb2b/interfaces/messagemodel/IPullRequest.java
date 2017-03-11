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

/**
 * Represents the information available of the PullRequest type of signal message.
 * <p>The only information contained in a PullRequest is the <i>MPC</i> (message partition channel) the message should
 * be pulled from. See section 3 of the ebMS Core Specification for more information on the concept of pulling.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IMessageUnit
 */
public interface IPullRequest extends ISignalMessage {

    /**
     * Gets the MPC that is included in the <code>PullRequest</code>.
     * <p>Corresponds to the <code>//eb:PullRequest/@mpc</code>. See section 5.2.3.1 of the ebMS Core Specification.
     *
     * @return      The MPC from which a message should be pulled
     */
    String getMPC();
}
