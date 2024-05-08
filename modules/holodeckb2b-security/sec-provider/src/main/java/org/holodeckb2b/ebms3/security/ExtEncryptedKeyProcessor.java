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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.DOMX509IssuerSerial;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.EncryptedKeyProcessor;
import org.apache.wss4j.dom.str.EncryptedKeySTRParser;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.dom.str.STRParser.REFERENCE_TYPE;
import org.apache.wss4j.dom.str.STRParserParameters;
import org.apache.wss4j.dom.str.STRParserResult;
import org.apache.wss4j.dom.util.EncryptionUtils;
import org.apache.wss4j.dom.util.X509Util;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.XMLCipherUtil;
import org.apache.xml.security.encryption.keys.content.AgreementMethodImpl;
import org.apache.xml.security.encryption.params.KeyAgreementParameters;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.holodeckb2b.commons.util.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Extends {@link EncryptedKeyProcessor} to support processing of the recipient certificate meta-data used in the key
 * agreement in the <code>ds:X509Data</code> element as specified in XML Encryption Syntax and Processing Version 1.1.
 * As key agreement is not specified in the WS-Security specifications both the <code>ds:X509Data</code> element and the
 * <code>wss:SecurityTokenReference</code> element are used for including the recipient certificate. The WSS4J processor
 * only supports the token reference, so we extend it here to handle both cases.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class ExtEncryptedKeyProcessor extends EncryptedKeyProcessor {
	private static final Logger log = LogManager.getLogger();

	@Override
	public List<WSSecurityEngineResult> handleToken(Element elem, RequestData data, AlgorithmSuite algorithmSuite)
			throws WSSecurityException {

        // See if this key has already been processed. If so then just return the result
        String id = elem.getAttributeNS(null, "Id");
        if (!Utils.isNullOrEmpty(id)) {
             WSSecurityEngineResult result = data.getWsDocInfo().getResult(id);
             if (result != null
                 && WSConstants.ENCR == (Integer)result.get(WSSecurityEngineResult.TAG_ACTION)
             ) {
                 return Collections.singletonList(result);
             }
        }

        /* Check if this EncryptedKey uses the ECDH-ES key agreement method and if it does process the AgreementMethod
         * element. If it doesn't, we let the regular WSS4J processor handle the token.
         */
        String encryptedKeyTransportMethod = X509Util.getEncAlgo(elem);
        if (encryptedKeyTransportMethod == null)
            throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, "noEncAlgo");

        Element keyInfo = XMLUtils.getDirectChildElement(elem, "KeyInfo", WSConstants.SIG_NS);
        Element agreement = XMLUtils.getDirectChildElement(keyInfo, "AgreementMethod", WSConstants.ENC_NS);
        if (agreement == null)
        	return super.handleToken(elem, data, algorithmSuite);

        log.debug("KeyAgreement is used for symmetric key exchange");
        if (!Utils.nullSafeEqual(agreement.getAttribute("Algorithm"), DefaultSecurityAlgorithms.KEY_AGREEMENT)) {
        	log.error("Unsupported key agreement method : {}", agreement.getAttribute("Algorithm"));
        	throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, "noKAAlg");
        }

        log.trace("Get the encrypted key");
        Element cipherValue = EncryptionUtils.getCipherValueFromEncryptedData(elem);
        if (cipherValue == null) {
        	log.error("Missing the encrypted key in EncryptedKey//CipherValue");
        	throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noCipher");
        }
        byte[] encryptedKey = EncryptionUtils.getDecodedBase64EncodedData(cipherValue);

        log.trace("Retrieve certificate reference");
        Element rcptKeyInfp = XMLUtils.getDirectChildElement(agreement, "RecipientKeyInfo",
        													SecurityConstants.XENC_NAMESPACE_URI);
        if (rcptKeyInfp == null) {
        	log.error("Missing AgreementMethod/RecipientKeyInfo element");
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientSecTokRef");
        }
        STRParserResult certInfo;
        Element secRef = XMLUtils.getDirectChildElement(rcptKeyInfp, SecurityTokenReference.SECURITY_TOKEN_REFERENCE,
        												SecurityConstants.WSS_NAMESPACE_URI);
        Element x509data = XMLUtils.getDirectChildElement(rcptKeyInfp, WSConstants.X509_DATA_LN,
															SecurityConstants.DSIG_NAMESPACE_URI);
        if (secRef != null) {
        	log.trace("Certificate reference is included in wsse:SecurityTokenReference element");
            STRParserParameters parameters = new STRParserParameters();
            parameters.setData(data);
            parameters.setStrElement(secRef);

            STRParser strParser = new EncryptedKeySTRParser();
            certInfo = strParser.parseSecurityTokenReference(parameters);
        } else if (x509data != null) {
        	log.trace("Certificate reference is included in ds:X509Data element");
            certInfo = getCertInfoFromX509Data(x509data, data.getDecCrypto());
        } else {
        	log.error("Missing reference to receiver's certificate");
        	throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientSecTokRef");
        }
        X509Certificate[] certs = certInfo.getCertificates();
        if (certs == null || certs.length == 0) {
        	log.error("Missing certificate info in AgreementMethod");
        	throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientSecTokRef");
        }

        log.trace("Get the private key associated with referenced certificate");
        PrivateKey privateKey = data.getDecCrypto().getPrivateKey(certs[0], data.getCallbackHandler());

        log.trace("Decrypt the symmetric encryption key");
        byte[] decryptedKey = null;
        decryptedKey = decryptKey(encryptedKey, encryptedKeyTransportMethod, agreement, privateKey);

        log.trace("Decrypt the data");
        Element refList = XMLUtils.getDirectChildElement(elem, "ReferenceList", WSConstants.ENC_NS);
        List<WSDataRef> dataRefs = decryptDataRefs(refList, data.getWsDocInfo(), decryptedKey, data);

        WSSecurityEngineResult result = new WSSecurityEngineResult(WSConstants.ENCR, decryptedKey, encryptedKey,
        															dataRefs, certs);

        result.put(WSSecurityEngineResult.TAG_ENCRYPTED_KEY_TRANSPORT_METHOD, encryptedKeyTransportMethod);
        result.put(WSSecurityEngineResult.TAG_TOKEN_ELEMENT, elem);
        if (!Utils.isNullOrEmpty(id)) {
            result.put(WSSecurityEngineResult.TAG_ID, id);
        }
        result.put(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE, certInfo.getCertificatesReferenceType());

        data.getWsDocInfo().addResult(result);
        data.getWsDocInfo().addTokenElement(elem);

        return Collections.singletonList(result);
	}

    /**
     * Decrypts the symmetric encryption key using the specified Key Agreement algorithm and asymmetric private key.
     *
     * @param encryptedKey 			the encrypted key
     * @param encryptedKeyAlgorithm algorithm used to encrypt the key
     * @param agreementMethod 		the AgreementMethod element
     * @param privateKey 			the private key of the recipient
     * @return the encoded decrypted key
     * @throws WSSecurityException if the key decryption fails
     */
    private static byte[] decryptKey(byte[] encryptedKey, String encryptedKeyAlgorithm, Element agreementMethod,
            						 PrivateKey privateKey) throws WSSecurityException {
        SecretKey kek;
        try {
            KeyAgreementParameters parameterSpec = XMLCipherUtil.constructRecipientKeyAgreementParameters(
                    encryptedKeyAlgorithm, new AgreementMethodImpl(agreementMethod), privateKey);

            kek = org.apache.xml.security.utils.KeyUtils.aesWrapKeyWithDHGeneratedKey(parameterSpec);
        } catch (XMLSecurityException ex) {
            log.debug("Error occurred while resolving the Diffie Hellman key: " + ex.getMessage());
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }
        Cipher cipher = KeyUtils.getCipherInstance(encryptedKeyAlgorithm);
        try {
            cipher.init(Cipher.UNWRAP_MODE, kek);
            String keyAlgorithm = JCEMapper.translateURItoJCEID(encryptedKeyAlgorithm);
            return cipher.unwrap(encryptedKey, keyAlgorithm, Cipher.SECRET_KEY).getEncoded();
        } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }
    }

    /**
     * Gets the receiver's certificate from the <code>ds:X509Data</code> element.
     *
     * @param elem		the <code>X509Data</code> element
     * @param crypto	the Crypto instance used to retrieve certificates
     * @return	a {@link STRParserResult} instance containing the found certificate and indication how the certificate
     * 			was referenced
     * @throws WSSecurityException if an error occurs parsing the content of the <code>X509Data</code> element
     */
    private STRParserResult getCertInfoFromX509Data(Element elem, Crypto crypto) throws WSSecurityException {
    	STRParserResult result = new STRParserResult();

        NodeList childNodes = elem.getChildNodes();
        Element x509Child = null;
        for(int i = 0; x509Child == null && i < childNodes.getLength(); i++)
        	if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE)
        		x509Child = (Element) childNodes.item(i);

        if (x509Child == null || !SecurityConstants.DSIG_NAMESPACE_URI.equals(x509Child.getNamespaceURI())) {
        	log.error("Missing child element of <ds:X509Data> element in RecipientKeyInfo");
        	throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientSecTokRef");
        }

        switch(x509Child.getLocalName()) {
        case WSConstants.X509_ISSUER_SERIAL_LN : {
        	result.setReferenceType(REFERENCE_TYPE.ISSUER_SERIAL);
        	DOMX509IssuerSerial issuerSerial = new DOMX509IssuerSerial(x509Child);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(issuerSerial.getIssuer(), issuerSerial.getSerialNumber());
            result.setCerts(crypto.getX509Certificates(cryptoType));
        	break;
        } case "X509SKI" : {
        	result.setReferenceType(REFERENCE_TYPE.KEY_IDENTIFIER);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SKI_BYTES);
            cryptoType.setBytes(EncryptionUtils.getDecodedBase64EncodedData(x509Child));
            result.setCerts(crypto.getX509Certificates(cryptoType));
        	break;
        } case WSConstants.X509_CERT_LN : {
        	result.setReferenceType(REFERENCE_TYPE.DIRECT_REF);
        	try (InputStream in = new ByteArrayInputStream(EncryptionUtils.getDecodedBase64EncodedData(x509Child))) {
        		result.setCerts(new X509Certificate[]{crypto.loadCertificate(in)});
        	} catch (Exception e) {
        		log.error("Could not parse certificate from RecipientKeyInfo/ds:X509Data/ds:X509Certificate");
        		throw new WSSecurityException(
        									WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "parseError");
        	}
        	break;
        } default:
        	log.error("Unsupported certificate reference {} used in RecipientKeyInfo/ds:X509Data",
        				x509Child.getLocalName());
        	throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientSecTokRef");
        }

        return result;
    }
}

