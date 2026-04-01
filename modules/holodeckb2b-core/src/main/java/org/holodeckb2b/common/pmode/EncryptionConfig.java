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
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.IKeyAgreement;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Contains the parameters related to the message level encryption.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class EncryptionConfig implements IEncryptionConfiguration, Serializable {
	private static final long serialVersionUID = -3364424899897499432L;

    @ElementList(entry = "KeystoreAlias", type = KeystoreAlias.class, inline = true, required = true)
    private List<KeystoreAlias> keyStoreRefs = new ArrayList<>();

    // encryption algorithm
    @Element(name = "Algorithm", required = false)
    private String algorithm = null;

    @Element(name = "KeyTransport", required = false)
    private KeyTransportConfig  keytransportCfg;

    @Element(name = "KeyAgreement", required = false)
    private KeyAgreementConfig  keyAgreementCfg;

    /**
     * Default constructor creates a new and empty <code>EncryptionConfig</code> instance.
     */
    public EncryptionConfig() {
    }

    /**
     * Creates a new <code>EncryptionConfig</code> instance using the parameters from the provided {@link
     * IEncryptionConfiguration}  object.
     *
     * @param source The source object to copy the parameters from
     */
    public EncryptionConfig(final IEncryptionConfiguration source) {
    	if (!Utils.isNullOrEmpty(source.getDecryptionKeypairs()))
    		source.getDecryptionKeypairs().forEach((a, p) -> keyStoreRefs.add(new KeystoreAlias(a,p)));
    	else if (!Utils.isNullOrEmpty(source.getEncryptionCertificate()))
    		keyStoreRefs.add(new KeystoreAlias(source.getEncryptionCertificate(), null));

        algorithm = source.getAlgorithm();
        keytransportCfg = source.getKeyTransport() != null ? new KeyTransportConfig(source.getKeyTransport())
        														: null;
        keyAgreementCfg = source.getKeyAgreement() != null ? new KeyAgreementConfig(source.getKeyAgreement())
        														: null;
    }

    @Override
    public String getEncryptionCertificate() {
    	return keyStoreRefs.size() > 0 ? keyStoreRefs.get(0).name : null;
    }

    /**
     * Sets the reference to the certificate managed by the <i>Certificate Manager</i> to be used for encryption.
     *
     * @param alias	the alias of the certificate
     * @since 8.2.0
     */
    public void setEncryptionCertificate(final String alias) {
        keyStoreRefs = List.of(new KeystoreAlias(alias, null));
    }

    @Override
    public Map<String, String> getDecryptionKeypairs() {
    	return keyStoreRefs.stream().collect(Collectors.toMap(ka -> ka.name, ka -> ka.password));
    }

    /**
     * Sets the reference(s) to the keypair(s) managed by the <i>Certificate Manager</i> to be used for decryption.
     *
     * @param keypairs	map of keypair references consisting of the alias and password to access the keypair
     * @since 8.2.0
     */
	public void setDecryptionKeypairs(final Map<String, String> keypairs) {
		if (keypairs == null)
			keyStoreRefs = new ArrayList<>();
		else
			keyStoreRefs = keypairs.entrySet().stream().map(e -> new KeystoreAlias(e.getKey(), e.getValue()))
															.collect(Collectors.toList());
	}

	/**
	 * Adds a reference to a keypair managed by the <i>Certificate Manager</i> to be used for decryption.
	 *
	 * @param alias		the alias the keypair is registered with in the <i>Certificate Manager</i>
	 * @param password	the password to access the {@link KeyPair}
	 * @since 8.2.0
	 */
	public void addDecryptionKeypair(final String alias, final String password) {
		if (Utils.isNullOrEmpty(alias))
			throw new IllegalArgumentException("Keypair alias cannot be null or empty");

		keyStoreRefs.add(new KeystoreAlias(alias, password));
	}

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public KeyTransportConfig getKeyTransport() {
        return keytransportCfg;
    }

    public void setKeyTransport(final KeyTransportConfig keytransport) {
        this.keytransportCfg = keytransport;
    }

    @Override
    public IKeyAgreement getKeyAgreement() {
    	return keyAgreementCfg;
    }

	public void setKeyAgreement(final IKeyAgreement keyAgreement) {
		this.keyAgreementCfg = keyAgreement != null ? new KeyAgreementConfig(keyAgreement) : null;
	}
}
