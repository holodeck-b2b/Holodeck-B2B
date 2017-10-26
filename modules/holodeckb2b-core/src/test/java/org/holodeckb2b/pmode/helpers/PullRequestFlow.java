/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode.helpers;

import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PullRequestFlow implements IPullRequestFlow {

    private String mpc;
    private SecurityConfig secConfig;
    private ErrorHandlingConfig errHandlingCfg;

    @Override
    public String getMPC() {
        return mpc;
    }

    public void setMPC(final String mpc) {
        this.mpc = mpc;
    }

    @Override
    public ISecurityConfiguration getSecurityConfiguration() {
        return secConfig;
    }

    public void setSecurityConfiguration(SecurityConfig securityCfg) {
        this.secConfig = securityCfg;
    }

    @Override
    public IErrorHandling getErrorHandlingConfiguration() {
        return errHandlingCfg;
    }

    public void setErrorHandlingConfiguration(ErrorHandlingConfig errorCfg) {
        this.errHandlingCfg = errorCfg;
    }

}
