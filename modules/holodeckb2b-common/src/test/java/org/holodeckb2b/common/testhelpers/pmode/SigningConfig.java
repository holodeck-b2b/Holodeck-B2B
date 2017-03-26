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

import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.security.X509ReferenceType;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SigningConfig implements ISigningConfiguration {

    private String              keyAlias;
    private String              keyPassword;
    private X509ReferenceType   keyRefMethod;
    private Boolean             includeCertPath;
    private Boolean             enableRevocation;
    private String              signAlgorithm;
    private String              hashAlgorithm;

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
    public X509ReferenceType getKeyReferenceMethod() {
        return keyRefMethod;
    }

    public void setKeyReferenceMethod(final X509ReferenceType refMethod) {
        this.keyRefMethod = refMethod;
    }

    @Override
    public Boolean includeCertificatePath() {
        return includeCertPath;
    }

    public void setIncludeCertPath(final Boolean includePath) {
        this.includeCertPath = includePath;
    }

    @Override
    public Boolean enableRevocationCheck() {
        return enableRevocation;
    }

    public void setRevocationCheck(final Boolean enableRevocation) {
        this.enableRevocation = enableRevocation;
    }

    @Override
    public String getSignatureAlgorithm() {
        return signAlgorithm;
    }

    public void setSignatureAlgorithm(final String algorithm) {
        this.signAlgorithm = algorithm;
    }

    @Override
    public String getHashFunction() {
        return hashAlgorithm;
    }

    public void setHashFunction(final String algorithm) {
        this.hashAlgorithm = algorithm;
    }
}
