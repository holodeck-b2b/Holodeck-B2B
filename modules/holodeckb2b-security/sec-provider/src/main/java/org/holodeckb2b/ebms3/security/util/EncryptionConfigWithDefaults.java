/*
 * Copyright (C) 2018 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.security.util;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.ebms3.security.DefaultSecurityAlgorithms;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.IKeyAgreement;
import org.holodeckb2b.interfaces.pmode.IKeyTransport;

/**
 * Is a decorator for a {@link IEncryptionConfiguration} instance to ensure that the default security algorithms are
 * returned if none is specified by the source instance.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class EncryptionConfigWithDefaults implements IEncryptionConfiguration {
    /**
     * The source configuration for the main encryption settings
     */
    private IEncryptionConfiguration original;
    /**
     * Decorator also for the key transport settings
     */
    private KTConfigWithDefaults keyTransportCfg = null;
    /**
     * Decorator also for the key agreement settings
     * @since 7.0.0
     */
    private KAConfigWithDefaults keyAgreementCfg = null;

    /**
     * Create a new decorator for the given source configuration
     *
     * @param original  The original configuration
     */
    public EncryptionConfigWithDefaults(IEncryptionConfiguration original) {
        this.original = original;
        if (original.getKeyAgreement() != null)
        	this.keyAgreementCfg = new KAConfigWithDefaults(original.getKeyAgreement());
        else
        	this.keyTransportCfg = new KTConfigWithDefaults(original.getKeyTransport());
    }

    @Override
    public String getKeystoreAlias() {
        return original.getKeystoreAlias();
    }

    @Override
    public String getCertificatePassword() {
        return original.getCertificatePassword();
    }

    @Override
    public String getAlgorithm() {
        return !Utils.isNullOrEmpty(original.getAlgorithm()) ? original.getAlgorithm() :
                                                               DefaultSecurityAlgorithms.ENCRYPTION;
    }

    @Override
    public IKeyTransport getKeyTransport() {
        return keyTransportCfg;
    }

    @Override
    public IKeyAgreement getKeyAgreement() {
    	return keyAgreementCfg;
    }
}
