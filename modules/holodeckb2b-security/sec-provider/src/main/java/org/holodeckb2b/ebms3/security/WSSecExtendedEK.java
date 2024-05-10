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

import java.security.Provider;
import java.security.cert.X509Certificate;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.xml.security.encryption.keys.content.AgreementMethodImpl;
import org.apache.xml.security.encryption.params.ConcatKDFParams;
import org.apache.xml.security.encryption.params.KeyAgreementParameters;
import org.apache.xml.security.encryption.params.KeyDerivationParameters;
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
	private String	concatAlgorithmID;
	private String	concatPartyUInfo;
	private String	concatPartyVInfo;

	public WSSecExtendedEK(Document doc) {
		super(doc);
	}

	public WSSecExtendedEK(Document doc, Provider provider) {
		super(doc, provider);
	}

	public WSSecExtendedEK(WSSecHeader securityHeader) {
		super(securityHeader);
	}

	public void setRecipientCertInSecRef(boolean rcptCertInSecRef) {
		this.rcptCertInSecRef = rcptCertInSecRef;
	}

	public boolean recipientCertInSecRef() {
		return rcptCertInSecRef;
	}

	public void setConcatKDFAlgorithmID(String concatAlgorithmID) {
		this.concatAlgorithmID = concatAlgorithmID;
	}

	public void setConcatKDFPartyUInfo(String concatPartyUInfo) {
		this.concatPartyUInfo = concatPartyUInfo;
	}

	public void setConcatKDFPartyVInfo(String concatPartyVInfo) {
		this.concatPartyVInfo = concatPartyVInfo;
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
            	KeyDerivationParameters kdfParams = dhSpec.getKeyDerivationParameter();
            	if (kdfParams instanceof ConcatKDFParams) {
            		ConcatKDFParams concatKDFParams = (ConcatKDFParams)kdfParams;
            		concatKDFParams.setAlgorithmID(concatAlgorithmID);
					concatKDFParams.setPartyUInfo(concatPartyUInfo);
					concatKDFParams.setPartyVInfo(concatPartyVInfo);
            	}
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
}
