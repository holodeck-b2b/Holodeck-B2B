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
import java.util.ArrayList;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Contains the parameters related to the message level security.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class SecurityConfig implements ISecurityConfiguration, Serializable {
	private static final long serialVersionUID = -5685657921442392001L;

    @ElementList (entry = "UsernameToken", inline = true, required = false)
    protected ArrayList<UsernameTokenConfig>   usernameTokens = new ArrayList<>(2);

    @Element (name = "Signing", required = false)
    protected SigningConfig     signingConfiguration;

    @Element (name = "Encryption", required = false)
    protected EncryptionConfig  encryptionConfiguration;

    /**
     * Default constructor creates a new and empty <code>SecurityConfig</code> instance.
     */
    public SecurityConfig() {}

    /**
     * Creates a new <code>SecurityConfig</code> instance using the parameters from the provided {@link
     * ISecurityConfiguration} object.
     *
     * @param source The source object to copy the parameters from
     */
    public SecurityConfig(final ISecurityConfiguration source) {
        this.signingConfiguration = source.getSignatureConfiguration() != null ?
                                    new SigningConfig(source.getSignatureConfiguration()) : null;
        this.encryptionConfiguration = source.getEncryptionConfiguration() != null ?
                                    new EncryptionConfig(source.getEncryptionConfiguration()) : null;                                        
        IUsernameTokenConfiguration def = source.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT);
        IUsernameTokenConfiguration ebms = source.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS);
        if (def != null)
        	usernameTokens.add(new UsernameTokenConfig(SecurityHeaderTarget.DEFAULT, def));
        if (ebms != null)
        	usernameTokens.add(new UsernameTokenConfig(SecurityHeaderTarget.EBMS, ebms));
    }
    
    @Override
    public UsernameTokenConfig getUsernameTokenConfiguration(SecurityHeaderTarget target) {
        int idx = findUTConfig(target);
        return idx < 0 ? null : usernameTokens.get(idx);
    }

    public void setUsernameTokenConfiguration(SecurityHeaderTarget target, UsernameTokenConfig utConfig) {    	
    	int cIdx = findUTConfig(target);
    	if (utConfig != null) {
    		utConfig.target = target.id();    		    	
    		if (cIdx > 0) 
    			this.usernameTokens.set(cIdx, utConfig);    	
    		else 
    			this.usernameTokens.add(utConfig);
    	} else if (cIdx > 0)
    		this.usernameTokens.remove(cIdx);
    }

    /**
     * Gets the index at which the configuration of the Username token targeted at the given role is stored in the 
     * list.
     *  
     * @param target	The targeted role
     * @return			The index at which the configured is stored, or -1 if not found
     */
    private int findUTConfig(final SecurityHeaderTarget target) {
    	int idx = -1;    	
    	if (!Utils.isNullOrEmpty(usernameTokens)) {
    		for (int i = 0; i < usernameTokens.size() && idx == -1; i++)
    			if (Utils.nullSafeEqual(target.id(), usernameTokens.get(i).target))
    				idx = i;
    	}
    	return idx;
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
        return encryptionConfiguration;
    }

    public void setEncryptionConfiguration(final EncryptionConfig encryptionConfig) {
        this.encryptionConfiguration = encryptionConfig;
    }
}
