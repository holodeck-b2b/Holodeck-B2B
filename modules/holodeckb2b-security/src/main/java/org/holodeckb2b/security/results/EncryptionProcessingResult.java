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
package org.holodeckb2b.security.results;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.security.IEncryptionProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.X509ReferenceType;

/**
 * Is the security provider's implementation of {@link IEncryptionProcessingResult} containing the result of processing
 * the encryption of a message (includes decryption as well).
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class EncryptionProcessingResult extends AbstractSecurityProcessingResult implements IEncryptionProcessingResult
{
    private final X509Certificate       certificate;
    private final X509ReferenceType     refMethod;
    private final KeyTransportInfo      keyInfo;
    private final String                algorithm;
    private final Collection<IPayload>  payloads;


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
        this.keyInfo = null;
        this.algorithm = null;
        this.payloads = null;
    }

    /**
     * Creates a new <code>EncryptionProcessingResult</code> instance to indicate that processing of the encryption
     * completed successfully.
     *
     * @param encryptionCert        Certificate used for encryption
     * @param certReferenceMethod   Method used to include/reference the certificate
     * @param ktAlgorithm           Algorithm to protect the symmetric key
     * @param ktMGF                 The mask generation function identifier
     * @param ktDigest              The digest algorithm used for symmetric key transport
     * @param algorithm             The encryption algorithm
     * @param payloads              The encrypted payloads
     */
    public EncryptionProcessingResult(final X509Certificate encryptionCert, final X509ReferenceType certReferenceMethod,
                                      final String ktAlgorithm, final String ktMGF, final String ktDigest,
                                      final String algorithm, final Collection<IPayload> payloads) {
        super(SecurityHeaderTarget.DEFAULT);
        this.certificate = encryptionCert;
        this.refMethod = certReferenceMethod;
        this.keyInfo = new KeyTransportInfo(ktAlgorithm, ktDigest, ktMGF);
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
    public IKeyTransportInfo getKeyTransportInfo() {
        return keyInfo;
    }

    @Override
    public String getEncryptionAlgorithm() {
        return algorithm;
    }

    @Override
    public Collection<IPayload> getEncryptedPayloads() {
        return payloads;
    }

    /**
     * Is the security provider's implementation of {@link IKeyTransportInfo} containing the meta-data on how the
     * symmetric key for encryption is exchanged.
     */
    public class KeyTransportInfo implements IKeyTransportInfo {

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
        KeyTransportInfo(String transportAlgorithm, String digestMethod, String mgfAlgorithm) {
            this.transportAlgorithm = transportAlgorithm;
            this.digestMethod = digestMethod;
            this.mgfAlgorithm = mgfAlgorithm;
        }

        @Override
        public String getKeyTransportAlgorithm() {
            return algorithm;
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
}
