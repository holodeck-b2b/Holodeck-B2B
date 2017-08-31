/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.simpleframework.xml.Element;

/**
 * Represents the <code>KeyTransport</code> element in the P-Mode XML document
 * that contains the P-Mode parameters for key transport when encrypting message.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class KeyTransport implements IKeyTransport {

    @Element (name = "Algorithm", required = false)
    private String algorithm;

    @Element (name = "MGFAlgorithm", required = false)
    private String MGFAlgorithm;

    @Element (name = "DigestAlgorithm", required = false)
    private String digestAlgorithm;

    @Element(name = "KeyReferenceMethod", required = false)
    private KeyReferenceMethod keyReferenceMethod;

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getMGFAlgorithm() {
        return MGFAlgorithm;
    }

    @Override
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    @Override
    public X509ReferenceType getKeyReferenceMethod() {
        return (keyReferenceMethod != null ? keyReferenceMethod.getRefMethod() : null);
    }
}
