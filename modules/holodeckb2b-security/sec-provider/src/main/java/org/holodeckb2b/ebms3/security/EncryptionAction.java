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

import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SecurityActionToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.action.Action;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a customised version of {@link org.apache.wss4j.dom.action.EncryptionAction} that uses our own implementation of
 * the encryption action so we can support the different methods to include the reference to the receiver's certificate
 * that was used for the key agreement.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class EncryptionAction implements Action {
	/**
	 * Value to use for the <i>TokenType</i> to indicate that the meta-date of the receiver's certificate used in the
	 * key agreement should be included in a <code>wss:SecurityTokenReference</code> element.
	 */
	public static final String	KA_CERT_ASREF = "kaRcptCertAsRef";

	@Override
	public void execute(WSHandler handler, SecurityActionToken actionToken, RequestData reqData)
			throws WSSecurityException {

        WSSecExtendedEK wsEncrypt = new WSSecExtendedEK(reqData.getSecHeader());
        wsEncrypt.setIdAllocator(reqData.getWssConfig().getIdAllocator());
        wsEncrypt.setWsDocInfo(reqData.getWsDocInfo());
        wsEncrypt.setExpandXopInclude(reqData.isExpandXopInclude());

        EncryptionActionToken encryptionToken = null;
        if (actionToken instanceof EncryptionActionToken)
            encryptionToken = (EncryptionActionToken)actionToken;
        if (encryptionToken == null)
            encryptionToken = reqData.getEncryptionToken();
        if (encryptionToken.getKeyIdentifierId() != 0)
            wsEncrypt.setKeyIdentifierType(encryptionToken.getKeyIdentifierId());
        if (encryptionToken.getSymmetricAlgorithm() != null)
            wsEncrypt.setSymmetricEncAlgorithm(encryptionToken.getSymmetricAlgorithm());
        if (encryptionToken.getKeyTransportAlgorithm() != null)
            wsEncrypt.setKeyEncAlgo(encryptionToken.getKeyTransportAlgorithm());
        if (encryptionToken.getKeyAgreementMethodAlgorithm() != null)
            wsEncrypt.setKeyAgreementMethod(encryptionToken.getKeyAgreementMethodAlgorithm());
        if (encryptionToken.getDigestAlgorithm() != null)
            wsEncrypt.setDigestAlgorithm(encryptionToken.getDigestAlgorithm());

        if (encryptionToken.getMgfAlgorithm() != null)
            wsEncrypt.setMGFAlgorithm(encryptionToken.getMgfAlgorithm());

        wsEncrypt.setIncludeEncryptionToken(encryptionToken.isIncludeToken());

        wsEncrypt.setUserInfo(encryptionToken.getUser());
        wsEncrypt.setUseThisCert(encryptionToken.getCertificate());
        Crypto crypto = encryptionToken.getCrypto();
        boolean enableRevocation = Boolean.parseBoolean(handler.getStringOption(WSHandlerConstants.ENABLE_REVOCATION));
        if (enableRevocation && crypto != null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(encryptionToken.getUser());
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            if (certs != null && certs.length > 0)
                crypto.verifyTrust(certs, enableRevocation, null, null);
        }
        if (!encryptionToken.getParts().isEmpty())
            wsEncrypt.getParts().addAll(encryptionToken.getParts());


        KeyGenerator keyGen = KeyUtils.getKeyGenerator(wsEncrypt.getSymmetricEncAlgorithm());
        SecretKey symmetricKey = keyGen.generateKey();

        wsEncrypt.setEncryptSymmKey(encryptionToken.isEncSymmetricEncryptionKey());

        wsEncrypt.setAttachmentCallbackHandler(reqData.getAttachmentCallbackHandler());
        wsEncrypt.setStoreBytesInAttachment(reqData.isStoreBytesInAttachment());

        wsEncrypt.setRecipientCertInSecRef(Utils.isTrue((String) handler.getOption(DefaultProvider.P_CERT_AS_WSSECREF)));

        try {
            wsEncrypt.build(encryptionToken.getCrypto(), symmetricKey);
        } catch (WSSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "empty",
                                          new Object[] {"Error during encryption: "});
        }

	}

}
