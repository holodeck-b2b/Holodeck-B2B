/*
 * Copyright (C) 2015 The Holodeck B2B Team
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

import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration.WSSHeaderTarget;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SecurityConfig implements ISecurityConfiguration {

    private UsernameTokenConfig[]   usernameTokens = new UsernameTokenConfig[2];
    private SigningConfig           signatureConfig;
    private EncryptionConfig        encryptionConfig;

    @Override
    public UsernameTokenConfig getUsernameTokenConfiguration(WSSHeaderTarget target) {
        if (target == WSSHeaderTarget.EBMS)
            return usernameTokens[1];
        else
            return usernameTokens[0];
    }

    public void setUsernameTokenConfiguration(WSSHeaderTarget target, UsernameTokenConfig utConfig) {
        if (target == WSSHeaderTarget.EBMS)
            this.usernameTokens[1] = utConfig;
        else
            this.usernameTokens[0] = utConfig;
    }

    @Override
    public SigningConfig getSignatureConfiguration() {
        return signatureConfig;
    }

    public void setSignatureConfiguration(final SigningConfig signingConfig) {
        this.signatureConfig = signingConfig;
    }

    @Override
    public EncryptionConfig getEncryptionConfiguration() {
        return encryptionConfig;
    }

    public void setEncryptionConfiguration(final EncryptionConfig encryptionConfig) {
        this.encryptionConfig = encryptionConfig;
    }
}
