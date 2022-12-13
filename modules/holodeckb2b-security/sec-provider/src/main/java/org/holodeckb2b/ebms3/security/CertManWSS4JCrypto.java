/*
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoBase;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.holodeckb2b.ebms3.security.callbackhandlers.PasswordCallbackHandler;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;

/**
 * Is a {@link Crypto} implementation that uses the Holodeck B2B <i>Certificate Manager</i> to retrieve the key pairs
 * and certificates needed in the processing of the WS-Security headers in the ebMS messages. It uses {@link
 * CryptoBase} as base class to implement functionality that does not depend on how key pairs and certificates are
 * managed. Since the trust validation of certificates used for signing the messages is done in {@link
 * SignatureTrustValidator} this class does not provide trust validation functions.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  6.0.0
 */
public class CertManWSS4JCrypto extends CryptoBase {
	private static final MessageDigest SHA1;
	static {
		try {
			SHA1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			LogManager.getLogger().fatal("No SHA-1 implementation available!");
			throw new RuntimeException("No SHA-1 implementation available!");
		}
	}

	/**
	 * The Holodeck B2B Certificate Manager currently in use
	 */
	private ICertificateManager		certManager;
	/**
	 * The cryptographic action, e.g. encryption, being performed
	 */
	private Action	action;

	/**
	 * Initialises a new instance for use with the specified action. The action determines whether certificate searches
	 * are executed againstthe set of registered key pairs or trading partner certificates.
	 *
	 * @param action 	the crypto action in which this instance is used
	 */
	public CertManWSS4JCrypto(final Action action) {
		this.certManager = HolodeckB2BCoreInterface.getCertificateManager();
		this.action = action;
	}

	@Override
	public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        if (cryptoType == null) {
            return null;
        }
        CryptoType.TYPE type = cryptoType.getType();
        try {
	        switch (action) {
			case DECRYPT :
			case SIGN :
				String kpAlias = null;
				switch (type) {
				case ISSUER_SERIAL:
					kpAlias = certManager.findKeyPair(new X500Principal(cryptoType.getIssuer()), cryptoType.getSerial());
					break;
				case THUMBPRINT_SHA1:
					kpAlias = certManager.findKeyPair(cryptoType.getBytes(), SHA1);
					break;
				case SKI_BYTES:
					kpAlias = certManager.findKeyPair(cryptoType.getBytes());
					break;
				case ALIAS:
					kpAlias = cryptoType.getAlias();
					break;
				case SUBJECT_DN:
				case ENDPOINT:
				default:
					// These methods for retrieving a key pair are never needed when processing ebMS messages
					throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN);
				}
				if (kpAlias != null)
					return certManager.getKeyPairCertificates(kpAlias).toArray(new X509Certificate[] {});
				else
					throw new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE);
			case ENCRYPT :
			case VERIFY :
			default :
				X509Certificate cert;
				switch (type) {
				case ISSUER_SERIAL:
					cert = certManager.findCertificate(new X500Principal(cryptoType.getIssuer()), cryptoType.getSerial());
					break;
				case THUMBPRINT_SHA1:
					cert = certManager.findCertificate(cryptoType.getBytes(), SHA1);
					break;
				case SKI_BYTES:
					cert = certManager.findCertificate(cryptoType.getBytes());
					break;
				case ALIAS:
					cert = certManager.getPartnerCertificate(cryptoType.getAlias());
					break;
				case SUBJECT_DN:
				case ENDPOINT:
				default:
					// These methods for retrieving a certificate are never needed when processing ebMS messages
					throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN);
				}
				if (cert != null)
					return new X509Certificate[] { cert };
				else
					return null;
	        }
        } catch (SecurityProcessingException e) {
        	throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
	}

	@Override
	public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
		try {
			switch (action) {
			case DECRYPT :
			case SIGN :
				return certManager.findKeyPair(cert);
			case ENCRYPT :
			case VERIFY :
			default :
				return certManager.findCertificate(cert);
			}
		} catch (SecurityProcessingException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
		}
	}

	@Override
	public PrivateKey getPrivateKey(X509Certificate certificate, CallbackHandler callbackHandler)
																						throws WSSecurityException {
		try {
			String alias = certManager.findKeyPair(certificate);
			return getPrivateKey(alias, getPassword(callbackHandler, alias));
		} catch (SecurityProcessingException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
		}
	}

	@Override
	public PrivateKey getPrivateKey(PublicKey publicKey, CallbackHandler callbackHandler) throws WSSecurityException {
		try {
			String alias = certManager.findKeyPair(publicKey);
			return getPrivateKey(alias, getPassword(callbackHandler, alias));
		} catch (SecurityProcessingException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
		}
	}

	private String getPassword(CallbackHandler callbackHandler, String alias) throws WSSecurityException {
		if (!(callbackHandler instanceof PasswordCallbackHandler))
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "Invalid callback handler");

		WSPasswordCallback pwCb = new WSPasswordCallback(alias, WSPasswordCallback.UNKNOWN);
        try {
			callbackHandler.handle(new Callback[]{pwCb});
		} catch (IOException | UnsupportedCallbackException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
										   "noPassword", new Object[] {alias});
		}
        return pwCb.getPassword();
	}

	@Override
	public PrivateKey getPrivateKey(String identifier, String password) throws WSSecurityException {
		try {
			return certManager.getKeyPair(identifier, password).getPrivateKey();
		} catch (SecurityProcessingException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
		}
	}

	@Override
	public void verifyTrust(X509Certificate[] certs, boolean enableRevocation,
			Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints)
			throws WSSecurityException {
	}

	@Override
	public void verifyTrust(PublicKey publicKey) throws WSSecurityException {
	}
}
