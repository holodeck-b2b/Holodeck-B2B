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

import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Root (name = "PullRequestFlow", strict = false)
public class PullRequestFlow implements IPullRequestFlow {
    
    @Element (name = "Mpc", required = true)
    private String mpc;

    @Element (name = "ErrorHandling", required = false)
    private ErrorHandling errorHandling;

    @Element (name = "SecurityConfiguration", required = false)
    private PullSecurityConfiguration securityCfg;
    
    /**
     * Gets the MPC for this pull request operation. If none is specified in the XML document the default MPC is 
     * returned.
     * 
     * @return The MPC for this pull request
     */
    @Override
    public String getMPC() {
        return (mpc == null || mpc.isEmpty() ? EbMSConstants.DEFAULT_MPC : mpc);
    }
    
    @Override
    public ErrorHandling getErrorHandlingConfiguration() {
        return errorHandling;
    }

    @Override
    public ISecurityConfiguration getSecurityConfiguration() {
        return securityCfg;
    }
      
}
