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
package org.holodeckb2b.common.testhelpers.pmode;

import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class EncryptionConfig implements IEncryptionConfiguration {

    private String              keyAlias;
    private String              keyPassword;
    private String              encryptionAlgorithm;
    private KeyTransportConfig  keytransportCfg;

    @Override
    public String getKeystoreAlias() {
        return keyAlias;
    }

    public void setKeystoreAlias(final String alias) {
        this.keyAlias = alias;
    }

    @Override
    public String getCertificatePassword() {
        return keyPassword;
    }

    public void setCertificatePassword(final String password) {
        this.keyPassword = password;
    }

    @Override
    public String getAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.encryptionAlgorithm = algorithm;
    }

    @Override
    public KeyTransportConfig getKeyTransport() {
        return keytransportCfg;
    }

    public void setKeyTransport(final KeyTransportConfig keytransport) {
        this.keytransportCfg = keytransport;
    }
}
