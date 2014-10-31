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
package org.holodeckb2b.common.messagemodel;

import org.holodeckb2b.common.general.IAuthenticationInfo;

/**
 * Represents the information available of the PullRequest type of signal message.
 * <p>The only information contained in a PullRequest is the <i>MPC</i> (message partition channel) the message should 
 * be pulled from. See section 3 of the ebMS Core Specification for more information on the concept of pulling.
 * <p>It is however recommended that pulling uses authorization, so the message that contains the PullRequest should 
 * also contain information about the trading partner that wants to pull the message. The ebMS specification does not 
 * require how this <i>authentication info</i> should be transfered. Therefor a generic interface is used to define
 * access to this information. Here again the decoupling is used to allow for extension the supported types of partner
 * authentication. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageUnit
 * @see IAuthenticationInfo
 */
public interface IPullRequest extends ISignalMessage {

    /**
     * Gets the MPC that is included in the <code>PullRequest</code>.
     * <p>Corresponds to the <code>//eb:PullRequest/@mpc</code>. See section 5.2.3.1 of the ebMS Core Specification.
     * 
     * @return      The MPC from which a message should be pulled
     */
    String getMPC();
    
    /**
     * Gets the information to use for authorization of the pull.  
     * 
     * @return      If included in the message, an {@link IAuthenticationInfo} object representing the info to be used 
     *              for authorizing the pull request.<br>
     *              <code>null</code> if the message does not include any info for authorizing the pull request.
     */
    IAuthenticationInfo  getAuthenticationInfo();
}
