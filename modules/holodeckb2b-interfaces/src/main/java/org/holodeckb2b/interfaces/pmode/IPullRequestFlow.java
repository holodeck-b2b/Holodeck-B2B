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
package org.holodeckb2b.interfaces.pmode;


import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;

/**
 * Contains the P-Mode parameters for the exchange of a pull request and related error messages.
 * <p>As a pull request does not contain any business information other than the MPC the requested user message should
 * be pulled from the configuration is limited to error handling and security. The security information is to
 * authenticate and authorize a PullRequest (see section 7.10 of the Core Specification for more info). Normally this
 * information is provided with the trading partner configuration in the P-Mode. If however the sub-channel feature
 * (described in section 3.5 of the AS4 profile) is used each sub-channel can have its own authentication and
 * authorization. The sub-channel information should then be contained in this PullRequestFlow.
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPullRequestFlow {

    /**
     * Gets the MPC that must be included in the pull request.
     * <p>This can be a sub-channel MPC specific for this pull request. In that case this sub-channel MPC must start
     * with the MPC provided in/for the (to be) pulled user message.
     *
     * @return The MPC for the pull request, MUST NOT be <code>null</code>
     */
    public String getMPC();

    /**
     * Gets the specific authentication configuration that should be used for this pull request operation.
     * <p>The authentication information for a pull request can be either a user name token included in the WS-Security
     * header targeted at the <i>"ebms"</i> role/actor or a X.509 signature included in the default WS-Security header.
     * <p>The configuration specified on the flow overrides the security settings specified for the trading partner that
     * initiates the pull request. This setting is most useful on the responding MSH to define the authorization of the
     * sub-channels within the P-Mode.
     * <p>NOTE: Although the {@link ISecurityConfiguration} allows to specify two user name token and also a encryption
     * configurations, here only the user name token for the <i>"ebms"</i> role/actor and signature configuration are
     * used. Other information is ignored.
     *
     * @return An {@link ISecurityConfiguration} object representing the security configuration, or<br>
     *         <code>null</code> when not specified
     */
    public ISecurityConfiguration getSecurityConfiguration();

    /**
     * Gets the configuration for handling errors which are caused by the pull request exchanged in this flow.
     * <p>For the pull request the error handling configuration is (currently) limited to indication whether errors
     * should be reported to the business application as the other settings are not useful in case of a pull request,
     * i.e. only {@link IErrorHandling#shouldNotifyErrorToBusinessApplication()} and
     * {@link IErrorHandling#getErrorDelivery()} are used.
     *
     * @return An {@link IErrorHandling} object representing the error handling configuration, or<br>
     *         <code>null</code> when not specified
     */
    public IErrorHandling getErrorHandlingConfiguration();

}
