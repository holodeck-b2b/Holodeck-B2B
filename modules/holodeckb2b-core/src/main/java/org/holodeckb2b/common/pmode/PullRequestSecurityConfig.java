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

import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the message level security used for the authorization of a Pull Request. This
 * is a subset of the regular security configuration of a trading partner which only includes the user name token 
 * targeted to the "ebms" role and / or signature configuration.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class PullRequestSecurityConfig implements ISecurityConfiguration, Serializable {
	private static final long serialVersionUID = -1540128458046710579L;

	@Element (name = "UsernameToken", required = false)
    protected UsernameTokenConfig   usernameToken;

    @Element (name = "Signing", required = false)
    protected SigningConfig     signingConfiguration;

    /**
     * Default constructor creates a new and empty <code>PullRequestSecurityConfig</code> instance.
     */
    public PullRequestSecurityConfig() {}

    /**
     * Creates a new <code>PullRequestSecurityConfig</code> instance using the parameters from the provided {@link
     * ISecurityConfiguration} object.
     *
     * @param source The source object to copy the parameters from
     */
    public PullRequestSecurityConfig(final ISecurityConfiguration source) {
        this.signingConfiguration = source.getSignatureConfiguration() != null ?
                                    new SigningConfig(source.getSignatureConfiguration()) : null;
        IUsernameTokenConfiguration ebms = source.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS);        
        usernameToken = ebms != null ? new UsernameTokenConfig(SecurityHeaderTarget.EBMS, ebms) : null;
    }
    
    @Override
    public UsernameTokenConfig getUsernameTokenConfiguration(SecurityHeaderTarget target) {
        return target == SecurityHeaderTarget.EBMS ? usernameToken : null;
    }

    public void setUsernameTokenConfiguration(UsernameTokenConfig utConfig) {
    	if (SecurityHeaderTarget.EBMS.id().equals(utConfig.target))
    		this.usernameToken = utConfig;
    	else
    		throw new IllegalArgumentException("Target must be \"ebms\" when used for Pull Request");
    }

    @Override
    public SigningConfig getSignatureConfiguration() {
        return signingConfiguration;
    }

    public void setSignatureConfiguration(final SigningConfig signingConfig) {
        this.signingConfiguration = signingConfig;
    }

    @Override
    public EncryptionConfig getEncryptionConfiguration() {
        return null;
    }
}
