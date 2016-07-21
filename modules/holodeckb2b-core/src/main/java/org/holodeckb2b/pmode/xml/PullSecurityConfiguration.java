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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;

/**
 * Contains the P-Mode parameters related to the authentication and authorization of the PullRequest signal on a
 * specific sub-channel. See section 7.11 for general information about securing the pull request.
 * <p>Normally the authentication information is provided in the <code>Initiator/SecurityConfiguration</code> element of
 * the P-Mode. If however the sub-channel feature as described in section 3.5 of the AS4 profile is used each sub
 * channel can have its own authentication and authorization. The sub-channel information is contained in <code>
 * PullRequestFlow/SecurityConfiguration</code>.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Root
public class PullSecurityConfiguration extends SecurityConfiguration {

    /**
     * Validates the read XML data to ensure that there is at most one UsernameToken child element. This only element
     * should have no <code>target</code> attribute specified or with value <i>"ebms"</i> because the sub-channel
     * authentication and authorization can only use the security header targeted to this role.
     *
     * @throws PersistenceException     When the read XML document contains more than 1 UsernameToken element
     */
    @Override
    @Validate
    public void validate() throws PersistenceException {
        if (usernameTokens == null)
            return;
        else if (usernameTokens.size() > 1)
            throw new PersistenceException("There shall be only one UsernameToken element for PullRequestFlow", null);
    }

    /**
     *
     *
     * @param target    is ignored.
     * @return          The UsernameTokenConfiguration specified specifically for this sub-channel
     */
    @Override
    public IUsernameTokenConfiguration getUsernameTokenConfiguration(final WSSHeaderTarget target) {
        if (usernameTokens != null && usernameTokens.size() == 1)
            return usernameTokens.get(0);
        else
            return null;
    }
}
