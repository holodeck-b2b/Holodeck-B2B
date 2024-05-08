/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.security.results;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.security.IEncryptionProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;

/**
 * Is a general implementation of {@link IEncryptionProcessingResult} containing the result of processing the encryption
 * of a message (includes decryption as well).
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class EncryptionProcessingResult extends AbstractSecurityProcessingResult implements IEncryptionProcessingResult
{
    private final X509Certificate       certificate;
    private final X509ReferenceType     refMethod;
    private final IKeyExchangeInfo      keyExchangeInfo;
    private final String                algorithm;
    private final Collection<IPayloadEntity>  payloads;


    /**
     * Creates a new <code>EncryptionProcessingResult</code> instance to indicate that there was a problem in processing
     * the encryption.
     *
     * @param failure   Exception indicating the problem that occurred
     */
    public EncryptionProcessingResult(final SecurityProcessingException failure) {
        super(SecurityHeaderTarget.DEFAULT, failure);
        this.certificate = null;
        this.refMethod = null;
        this.keyExchangeInfo = null;
        this.algorithm = null;
        this.payloads = null;
    }

    /**
     * Creates a new <code>EncryptionProcessingResult</code> instance to indicate that processing of the encryption
     * completed successfully.
     *
     * @param encryptionCert        Certificate used for encryption
     * @param certReferenceMethod   Method used to include/reference the certificate
     * @param keyExchange 			Method used for key exchange between partners
     * @param algorithm             The encryption algorithm
     * @param payloads              The encrypted payloads
     */
    public EncryptionProcessingResult(final X509Certificate encryptionCert, final X509ReferenceType certReferenceMethod,
                                      final IKeyExchangeInfo keyExchange, final String algorithm,
                                      final Collection<IPayloadEntity> payloads) {
        super(SecurityHeaderTarget.DEFAULT);
        this.certificate = encryptionCert;
        this.refMethod = certReferenceMethod;
        this.keyExchangeInfo = keyExchange;
        this.algorithm = algorithm;
        this.payloads = !Utils.isNullOrEmpty(payloads) ? Collections.unmodifiableCollection(payloads) : null;
    }

    @Override
    public X509Certificate getEncryptionCertificate() {
        return certificate;
    }

    @Override
    public X509ReferenceType getCertificateReferenceType() {
        return refMethod;
    }

    @Override
    public IKeyExchangeInfo getKeyExchangeInfo() {
        return keyExchangeInfo;
    }

    @Override
    public String getEncryptionAlgorithm() {
        return algorithm;
    }

    @Override
    public Collection<IPayloadEntity> getEncryptedPayloads() {
        return payloads;
    }

    /**
     * Is the default implementation of {@link IKeyTransportInfo} containing the meta-data on how the symmetric key for
     * encryption is exchanged using the key transport algorithm.
     */
    public static class KeyTransportInfo implements IKeyTransportInfo {

        private final String transportAlgorithm;
        private final String digestMethod;
        private final String mgfAlgorithm;

        /**
         * Creates a new <code>KeyTransportInfo</code> instance for the given meta-data.
         *
         * @param transportAlgorithm    The key transport algorithm
         * @param digestMethod          The digest method
         * @param mgfAlgorithm          The MGF function used
         */
        public KeyTransportInfo(String transportAlgorithm, String digestMethod, String mgfAlgorithm) {
            this.transportAlgorithm = transportAlgorithm;
            this.digestMethod = digestMethod;
            this.mgfAlgorithm = mgfAlgorithm;
        }

        @Override
        public String getKeyTransportAlgorithm() {
            return transportAlgorithm;
        }

        @Override
        public String getDigestMethod() {
            return digestMethod;
        }

        @Override
        public String getMGFAlgorithm() {
            return mgfAlgorithm;
        }
    }

    /**
     * Is the default implementation of {@link IKeyAgreementInfo} containing the meta-data on how the symmetric key for
     * encryption is exchanged using the key agreement method.
     *
     * @since 7.0.0
     */
    public static class KeyAgreementInfo implements IKeyAgreementInfo {
		private final String keyAgreementMethod;
		private final String keyDerivationAlgorithm;
		private final String digestAlgorithm;
		private final Map<String, ?> kaParameters;
		private final Map<String, ?> kdfParameters;

		/**
		 * Creates a new <code>KeyAgreementInfo</code> instance for the given meta-data.
		 *
		 * @param keyAgreementMethod		the key agreement method
		 * @param kaParameters				the parameters for the key agreement
		 * @param keyDerivationAlgorithm	the key derivation algorithm
		 * @param digestAlgorithm			the digest algorithm used with the key derivation
		 * @param kdfParameters				the parameters for the key derivation
		 */
		public KeyAgreementInfo(final String keyAgreementMethod, final Map<String, ?> kaParameters,
								final String keyDerivationAlgorithm, final String digestAlgorithm,
								final Map<String, ?> kdfParameters) {
			this.keyAgreementMethod = keyAgreementMethod;
			this.kaParameters = kaParameters == null ? null : Collections.unmodifiableMap(kaParameters);
			this.keyDerivationAlgorithm = keyDerivationAlgorithm;
			this.digestAlgorithm = digestAlgorithm;
			this.kdfParameters = kdfParameters == null ? null : Collections.unmodifiableMap(kdfParameters);
		}
		@Override
		public String getKeyAgreementMethod() {
			return keyAgreementMethod;
		}

		@Override
		public IKeyDerivationInfo getKeyDerivationInfo() {
			return new IKeyDerivationInfo() {
				@Override
				public String getKeyDerivationAlgorithm() { return keyDerivationAlgorithm; }
				@Override
				public String getDigestAlgorithm() { return digestAlgorithm; }
				@Override
				public Map<String, ?> getParameters() { return kdfParameters; }
			};
		}

		@Override
		public Map<String, ?> getParameters() {
			return kaParameters;
		}
    }
}
