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
package org.holodeckb2b.security.util;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.holodeckb2b.security.DefaultSecurityAlgorithms;

/**
 * Is a decorator for a {@link ISigningConfiguration} instance to ensure that the default security algorithms are
 * returned if none is specified by the source instance.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SigningConfigWithDefaults implements ISigningConfiguration {
    /**
     * The source signature configuration
     */
    private ISigningConfiguration original;

    /**
     * Create a new decorator for the given source configuration
     *
     * @param original  The original configuration
     */
    public SigningConfigWithDefaults(ISigningConfiguration original) {
        this.original = original;
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
    public X509ReferenceType getKeyReferenceMethod() {
        return original.getKeyReferenceMethod() != null ? original.getKeyReferenceMethod() :
                                                          DefaultSecurityAlgorithms.KEY_REFERENCE;
    }

    @Override
    public Boolean includeCertificatePath() {
        return original.includeCertificatePath() != null ? original.includeCertificatePath() : Boolean.FALSE;
    }

    @Override
    public String getSignatureAlgorithm() {
        return !Utils.isNullOrEmpty(original.getSignatureAlgorithm()) ? original.getSignatureAlgorithm() :
                                                                        DefaultSecurityAlgorithms.SIGNATURE;
    }

    @Override
    public String getHashFunction() {
        return !Utils.isNullOrEmpty(original.getHashFunction()) ? original.getHashFunction() :
                                                                  DefaultSecurityAlgorithms.MESSAGE_DIGEST;
    }
}
