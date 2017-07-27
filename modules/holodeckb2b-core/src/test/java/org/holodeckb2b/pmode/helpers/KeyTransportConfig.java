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

import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.security.X509ReferenceType;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class KeyTransportConfig implements IKeyTransport {

    private X509ReferenceType   keyRefMethod;
    private String              encryptionAlgorithm;
    private String              MGFAlgorithm;
    private String              digestAlgorithm;

    @Override
    public String getAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.encryptionAlgorithm = algorithm;
    }

    @Override
    public String getMGFAlgorithm() {
        return MGFAlgorithm;
    }

    public void setMGFAlgorithm(final String algorithm) {
        this.MGFAlgorithm = algorithm;
    }

    @Override
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(final String algorithm) {
        this.digestAlgorithm = algorithm;
    }

    @Override
    public X509ReferenceType getKeyReferenceMethod() {
        return keyRefMethod;
    }

    public void setKeyReferenceMethod(final X509ReferenceType refMethod) {
        this.keyRefMethod = refMethod;
    }
}
