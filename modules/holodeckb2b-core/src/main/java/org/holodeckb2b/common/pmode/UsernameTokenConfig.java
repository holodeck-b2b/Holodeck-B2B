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

import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Text;

/**
 * Contains the parameters related to the username tokens included in the security header.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class UsernameTokenConfig implements IUsernameTokenConfiguration, Serializable {
	private static final long serialVersionUID = 4569870208430404059L;

    @Attribute(name = "target", required = false)
    String target;

    @Element (name = "username")
    private String username;

    /**
     *  Represents the password element with type attribute
     */
    static class Password implements Serializable {
		private static final long serialVersionUID = -2561070532874543228L;

		@Text(required = true)
        private String value;

        @Attribute(required = false)
        private String type = "Digest";
    }
    
    @Element (name = "password")
    private Password password;

    @Element (name = "includeNonce", required = false)
    private Boolean includeNonce = Boolean.TRUE;

    @Element (name = "includeCreated", required = false)
    private Boolean includeCreated = Boolean.TRUE; 

    /**
     * Default constructor creates a new and empty <code>UsernameTokenConfig</code> instance.
     */
    public UsernameTokenConfig() {
    }

    /**
     * Creates a new <code>UsernameTokenConfig</code> instance using the specified target and parameters from the 
     * provided {@link IUsernameTokenConfiguration} object.
     *
     * @param target  The role at which this username token is targeted
     * @param source  The source object to copy the parameters from
     */
    public UsernameTokenConfig(final SecurityHeaderTarget target, final IUsernameTokenConfiguration source) {
    	this.target = target.id();
        this.username = source.getUsername();
        this.password = new Password(); 
        this.password.value = source.getPassword();
        this.password.type = source.getPasswordType() == UTPasswordType.TEXT ? "Text" : "Digest";
        this.includeNonce = source.includeNonce();
        this.includeCreated = source.includeCreated();
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password != null ? password.value : null;
    }

    public void setPassword(final String password) {
    	if (this.password == null)
    		this.password = new Password();
        this.password.value = password;
    }

    @Override
    public UTPasswordType getPasswordType() {
        return (password != null && "Text".equals(password.type)) ? UTPasswordType.TEXT : UTPasswordType.DIGEST;
    }

    public void setPasswordType(final UTPasswordType pwdType) {
    	if (this.password == null)
    		this.password = new Password();
    	this.password.type = pwdType == UTPasswordType.TEXT ? "Text" : "Digest";
    }

    @Override
    public boolean includeNonce() {
        return includeNonce;
    }

    public void setIncludeNonce(final boolean includeNonce) {
        this.includeNonce = includeNonce;
    }

    @Override
    public boolean includeCreated() {
        return includeCreated;
    }

    public void setIncludeCreated(final boolean includeCreated) {
        this.includeCreated = includeCreated;
    }

}
