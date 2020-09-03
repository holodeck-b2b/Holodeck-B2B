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

import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Contains the parameters related to the key transport for message level encryption.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class KeyTransportConfig implements IKeyTransport, Serializable {
	private static final long serialVersionUID = 2740107193474230438L;

    @Element (name = "Algorithm", required = false)
    private String algorithm;

    @Element (name = "MGFAlgorithm", required = false)
    private String MGFAlgorithm;

    @Element (name = "DigestAlgorithm", required = false)
    private String digestAlgorithm;

    @Element(name = "KeyReferenceMethod", required = false)
    @Convert(KeyReferenceMethodConverter.class)
    private X509ReferenceType keyReferenceMethod;

    /**
     * Default constructor creates a new and empty <code>KeyTransportConfig</code> instance.
     */
    public KeyTransportConfig() {}

    /**
     * Creates a new <code>KeyTransportConfig</code> instance using the parameters from the provided {@link
     * IKeyTransport} object.
     *
     * @param source The source object to copy the parameters from
     */
    public KeyTransportConfig(final IKeyTransport source) {
        this.keyReferenceMethod = source.getKeyReferenceMethod();
        this.algorithm = source.getAlgorithm();
        this.MGFAlgorithm = source.getMGFAlgorithm();
        this.digestAlgorithm = source.getDigestAlgorithm();
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
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
        return keyReferenceMethod;
    }

    public void setKeyReferenceMethod(final X509ReferenceType refMethod) {
        this.keyReferenceMethod = refMethod;
    }
}
