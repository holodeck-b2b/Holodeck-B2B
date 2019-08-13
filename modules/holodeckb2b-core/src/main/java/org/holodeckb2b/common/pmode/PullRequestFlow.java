/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the processing of <i>Pull Request</i> message units.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class PullRequestFlow implements IPullRequestFlow, Serializable {
	private static final long serialVersionUID = -7110752845931161722L;

    @Element (name = "Mpc", required = true)
    private String mpc;

    @Element (name = "ErrorHandling", required = false)
    private ErrorHandlingConfig errorHandling;

    @Element (name = "SecurityConfiguration", required = false)
    private PullRequestSecurityConfig securityCfg;

    /**
     * Default constructor creates a new and empty <code>PullRequestFlow</code> instance.
     */
    public PullRequestFlow() {}

    /**
     * Creates a new <code>PullRequestFlow</code> instance using the parameters from the provided {@link
     * IPullRequestFlow} object.
     *
     * @param source The source object to copy the parameters from
     */
    public PullRequestFlow(final IPullRequestFlow source) {
        this.mpc = Utils.isNullOrEmpty(source.getMPC()) ? EbMSConstants.DEFAULT_MPC : source.getMPC();
        this.securityCfg = source.getSecurityConfiguration() != null ?
												new PullRequestSecurityConfig(source.getSecurityConfiguration()) : null;
        this.errorHandling = source.getErrorHandlingConfiguration() != null ? 
        										new ErrorHandlingConfig(source.getErrorHandlingConfiguration()) : null;
    }

    @Override
    public String getMPC() {
        return mpc;
    }

    public void setMPC(final String mpc) {
        this.mpc = Utils.isNullOrEmpty(mpc) ? EbMSConstants.DEFAULT_MPC : mpc;
    }

    @Override
    public ISecurityConfiguration getSecurityConfiguration() {
        return securityCfg;
    }

    public void setSecurityConfiguration(final ISecurityConfiguration secConfig) {
        this.securityCfg = new PullRequestSecurityConfig(secConfig);
    }

    @Override
    public IErrorHandling getErrorHandlingConfiguration() {
        return errorHandling;
    }

    public void setErrorHandlingConfiguration(final ErrorHandlingConfig errConfig) {
        this.errorHandling = errConfig;
    }
}
