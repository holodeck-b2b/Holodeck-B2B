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

import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Represents the <code>SignatureConfiguration</code> element in the P-Mode XML document that contains the P-Mode
 * parameters for including a WSS signature in the message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
@Root
public class SignatureConfiguration implements ISigningConfiguration {

    @Element(name = "KeystoreAlias")
    private KeystoreAlias keyStoreRef;

    // This element is not supported anymore, but to prevent old P-Mode files from breaking it is still read
    @Element(name = "enableRevocationCheck", required = false)
    private Boolean enableRevocation = null;

    @Element(name = "KeyReferenceMethod", required = false)
    private KeyReferenceMethod keyReferenceMethod;

    @Element(name = "IncludeCertificatePath", required = false)
    private Boolean includeCertPath = null;

    @Element(name = "Algorithm", required = false)
    private String signatureAlgorithm = null;

    @Element(name = "HashFunction", required = false)
    private String hashFunction = null;

    @Override
    public String getKeystoreAlias() {
        return keyStoreRef.name;
    }

    @Override
    public String getCertificatePassword() {
        return keyStoreRef.password;
    }

    @Override
    public X509ReferenceType getKeyReferenceMethod() {
        return (keyReferenceMethod != null ? keyReferenceMethod.getRefMethod() : null);
    }

    @Override
    public Boolean includeCertificatePath() {
        return includeCertPath;
    }

    @Override
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Override
    public String getHashFunction() {
        return hashFunction;
    }

}
