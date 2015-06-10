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
package org.holodeckb2b.common.pmode;

import org.holodeckb2b.common.security.ISecurityConfiguration;

/**
 * Contains the P-Mode parameters for the exchange of a pull request and related error messages. 
 * <p>As a pull request does not contain any business information other than the MPC the requested user message should 
 * be pulled from the configuration is limited to error handling and security. The security information is to 
 * authenticate and authorize a PullRequest (see section 7.10 of the Core Specification for more info). Normally this
 * information is provided with the Initiator configuration in the P-Mode. If however the sub-channel feature as 
 * described in section 3.5 of the AS4 profile is used each sub-channel can have its own authentication and 
 * authorization. The sub-channel information should then contained then contained in this PullRequestFlow.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPullRequestFlow extends IFlow {
    
    /**
     * Gets the business information that must included in the message. Most of this information only applies to user
     * messages as pull request do not contain business information. Only the MPC name which is contained in the 
     * business info is contained in the pull request.
     * <p><b>NOTE: </b>A P-Mode does not need to include this information. If not specified the information to include
     * must be supplied during message submit.
     * 
     * @return The business information to include in the message as an {@link IBusinessInfo} object if specified by the
     *         P-Mode, or<br>
     *         <code>null</code> when not specified.
     */
    public String getMPC();
    
    /**
     * Gets the specific authentication configuration that should be used for this pull request operation. This can
     * consist of a user name token or a X.509 signature. 
     * <p>The configuration specified on the flow overrides the security settings specified for the trading partner that
     * initiates the pull request. This setting is most useful on the responding MSH to define the authorization of the 
     * sub-channels within the P-Mode. 
     * <p>NOTE: Although the {@link ISecurityConfiguration} allows to specify two user name token and also a encryption 
     * configurations, here only the user name token for the "ebms" role/actor and signature configuration are used.
     * Other information is ignored.
     * 
     * @return An {@link ISecurityConfiguration} object representing the security configuration, or<br>
     *         <code>null</code> when not specified
     */
    public ISecurityConfiguration getSecurityConfiguration();
}
