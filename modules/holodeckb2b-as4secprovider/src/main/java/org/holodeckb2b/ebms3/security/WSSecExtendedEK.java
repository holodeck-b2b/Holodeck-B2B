/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.security;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.Provider;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.xml.security.encryption.XMLCipherUtil;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.encryption.keys.content.AgreementMethodImpl;
import org.apache.xml.security.encryption.params.KeyAgreementParameters;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Extends {@link WSSecEncrypt} to support inclusion of the recipient certificate meta-data used in the key agreement
 * in the <code>ds:X509Data</code> element as specified in XML Encryption Syntax and Processing Version 1.1. As key
 * agreement is not specified in the WS-Security specifications we by default follow the XML Encryption specification
 * and use the <code>ds:X509Data</code> element instead of the <code>wss:SecurityTokenReference</code> element.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class WSSecExtendedEK extends WSSecEncrypt {

	private boolean rcptCertInSecRef = false;

	private Provider jceProvider;

	public WSSecExtendedEK(WSSecHeader securityHeader, Provider provider) {
		super(securityHeader);
		this.jceProvider = provider;
	}

	public void setRecipientCertInSecRef(boolean rcptCertInSecRef) {
		this.rcptCertInSecRef = rcptCertInSecRef;
	}

	public boolean recipientCertInSecRef() {
		return rcptCertInSecRef;
	}

	/**
     * Create the EncryptedKey Element for inclusion in the security header, by encrypting the
     * symmetricKey parameter using either a public key or certificate that is set on the class,
     * and adding the encrypted bytes as the CipherValue of the EncryptedKey element. The KeyInfo
     * is constructed according to the keyIdentifierType and also the type of the encrypting
     * key
     *
     * @param crypto An instance of the Crypto API to handle keystore and certificates
     * @param symmetricKey The symmetric key to encrypt and insert into the EncryptedKey
     * @throws WSSecurityException
     */
    @Override
	public void prepare(Crypto crypto, SecretKey symmetricKey) throws WSSecurityException {
    	if (!WSConstants.AGREEMENT_METHOD_ECDH_ES.equals(getKeyAgreementMethod()))
    		super.prepare(crypto, symmetricKey);
    	else {
    		/* We need to call super.prepare() to make sure the list of encrypted attachment is initialised. The only
    		 way to make sure that nothing else is done in super.prepare() is to set encryptSymmKey to false and
    		 override setEncryptedKeySHA1() to do nothing when ECDH is used
    		*/
    		setEncryptSymmKey(false);
    		super.prepare(crypto, symmetricKey);
            //
            // Get the certificate that contains the public key for the public key
            // algorithm that will encrypt the generated symmetric (session) key.
            //
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(user);
            if (crypto == null)
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noUserCertsFound",
                                              new Object[] {user, "encryption"});
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            if (certs == null || certs.length <= 0)
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noUserCertsFound",
                                              new Object[] {user, "encryption"});

            X509Certificate partnerCert = certs[0];

            KeyAgreementParameters dhSpec = XMLCipherUtil.constructAgreementParameters(getKeyAgreementMethod(),
                    									KeyAgreementParameters.ActorType.ORIGINATOR,
                    									getKeyDerivationParameters(), null, partnerCert.getPublicKey());
            try {
				dhSpec.setOriginatorKeyPair(org.apache.xml.security.utils.KeyUtils.generateEphemeralDHKeyPair(
															partnerCert.getPublicKey(), jceProvider));
			} catch (XMLEncryptionException e) {
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION, e);
			}

            createEncryptedKeyElement(partnerCert, crypto, dhSpec);

			try {
				Key kek = org.apache.xml.security.utils.KeyUtils.aesWrapKeyWithDHGeneratedKey(dhSpec);
				Cipher cipher = KeyUtils.getCipherInstance(getKeyEncAlgo());
				cipher.init(Cipher.WRAP_MODE, kek);
				byte[] encryptedEphemeralKey = cipher.wrap(symmetricKey);
				addCipherValueElement(encryptedEphemeralKey);
			} catch (InvalidKeyException | IllegalBlockSizeException | XMLEncryptionException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION, e);
			}
        }
    }

	@Override
	protected void createEncryptedKeyElement(X509Certificate remoteCert, Crypto crypto, KeyAgreementParameters dhSpec)
			throws WSSecurityException {
		if (!WSConstants.AGREEMENT_METHOD_ECDH_ES.equals(getKeyAgreementMethod()) || rcptCertInSecRef)
			// The default implementation includes the certificate in a WS-Security Token Reference, so we re-use it
			super.createEncryptedKeyElement(remoteCert, crypto, dhSpec);
		else {
			Document doc = getDocument();
	        Element encryptedKey = doc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":EncryptedKey");
            Element encryptionMethod = doc.createElementNS(WSConstants.ENC_NS,
            															WSConstants.ENC_PREFIX + ":EncryptionMethod");
            encryptionMethod.setAttributeNS(null, "Algorithm", getKeyEncAlgo());
            encryptedKey.appendChild(encryptionMethod);
	        if (Utils.isNullOrEmpty(getId()))
	            setEncKeyId(IDGenerator.generateID("EK-"));
	        encryptedKey.setAttributeNS(null, "Id", getId());

            Element keyInfoElement = getDocument().createElementNS(WSConstants.SIG_NS,
            													 WSConstants.SIG_PREFIX + ":" + WSConstants.KEYINFO_LN);
            keyInfoElement.setAttributeNS(WSConstants.XMLNS_NS, "xmlns:" + WSConstants.SIG_PREFIX, WSConstants.SIG_NS);
            try {
            	AgreementMethodImpl agreementMethod = new AgreementMethodImpl(getDocument(), dhSpec);
                X509Data x509Data = new X509Data(getDocument());
                switch (keyIdentifierType) {
                case WSConstants.SKI_KEY_IDENTIFIER:
					x509Data.addSKI(CertificateUtils.getSKI(remoteCert));
					break;
                case WSConstants.ISSUER_SERIAL:
                	x509Data.addIssuerSerial(CertificateUtils.getIssuerName(remoteCert), remoteCert.getSerialNumber());
                	break;
                case WSConstants.BST_DIRECT_REFERENCE:
                default:
                	x509Data.addCertificate(remoteCert);
                }
                agreementMethod.getRecipientKeyInfo().add(x509Data);
                keyInfoElement.appendChild(agreementMethod.getElement());
            } catch (XMLSecurityException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "unsupportedKeyId",
                        new Object[] {keyIdentifierType});
            }
            encryptedKey.appendChild(keyInfoElement);
            setEncryptedKeyElement(encryptedKey);
		}
	}

	@Override
	protected void setEncryptedKeySHA1(byte[] encryptedEphemeralKey) throws WSSecurityException {
		if (!WSConstants.AGREEMENT_METHOD_ECDH_ES.equals(getKeyAgreementMethod()))
			super.setEncryptedKeySHA1(encryptedEphemeralKey);
	}
}
