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

import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Contains the parameters related to the message level signature.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SigningConfig implements ISigningConfiguration, Serializable {
	private static final long serialVersionUID = -1791692568947001563L;
	
    @Element(name = "KeystoreAlias", required = false)
    private KeystoreAlias keyStoreRef;

    // This element is not supported anymore, but to prevent old P-Mode files from breaking it is still read
    @Element(name = "enableRevocationCheck", required = false)
    private Boolean enableRevocation = null;

    @Element(name = "KeyReferenceMethod", required = false)
    @Convert(KeyReferenceMethodConverter.class)    
    private X509ReferenceType keyReferenceMethod;

    @Element(name = "IncludeCertificatePath", required = false)
    private Boolean includeCertPath = null;

    @Element(name = "Algorithm", required = false)
    private String signatureAlgorithm = null;

    @Element(name = "HashFunction", required = false)
    private String hashFunction = null;

    /**
     * Default constructor creates a new and empty <code>SigningConfig</code> instance.
     */
    public SigningConfig() {
    	this.keyStoreRef = new KeystoreAlias();        
    }

    /**
     * Creates a new <code>SigningConfig</code> instance using the parameters from the provided {@link
     * ISigningConfiguration} object.
     *
     * @param source The source object to copy the parameters from
     */
    public SigningConfig(final ISigningConfiguration source) {
        this.keyStoreRef = new KeystoreAlias();
        this.keyStoreRef.name = source.getKeystoreAlias();
        this.keyStoreRef.password = source.getCertificatePassword();
        this.keyReferenceMethod = source.getKeyReferenceMethod();
        this.includeCertPath = source.includeCertificatePath();
        this.signatureAlgorithm = source.getSignatureAlgorithm();
        this.hashFunction = source.getHashFunction();
    }

    @Override
    public String getKeystoreAlias() {
        return keyStoreRef.name;
    }

    public void setKeystoreAlias(final String alias) {
        this.keyStoreRef.name = alias;
    }

    @Override
    public String getCertificatePassword() {
        return keyStoreRef.password;
    }

    public void setCertificatePassword(final String password) {
        this.keyStoreRef.password = password;
    }

    @Override
    public X509ReferenceType getKeyReferenceMethod() {
        return keyReferenceMethod;
    }

    public void setKeyReferenceMethod(final X509ReferenceType refMethod) {
        this.keyReferenceMethod = refMethod;
    }

    @Override
    public Boolean includeCertificatePath() {
        return includeCertPath;
    }

    public void setIncludeCertPath(final Boolean includePath) {
        this.includeCertPath = includePath;
    }

    @Override
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(final String algorithm) {
        this.signatureAlgorithm = algorithm;
    }

    @Override
    public String getHashFunction() {
        return hashFunction;
    }

    public void setHashFunction(final String algorithm) {
        this.hashFunction = algorithm;
    }
}
