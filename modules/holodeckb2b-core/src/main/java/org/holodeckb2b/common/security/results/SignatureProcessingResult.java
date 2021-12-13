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
import java.util.Collections;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;

/**
 * Is the security provider's implementation of {@link ISignatureProcessingResult} containing the result of processing
 * the signature in a message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class SignatureProcessingResult extends AbstractSecurityProcessingResult implements ISignatureProcessingResult {

    private final X509Certificate   certificate;
    private final IValidationResult trustCheck;
    private final X509ReferenceType refMethod;
    private final String            algorithm;
    private final ISignedPartMetadata                headerDigest;
    private final Map<IPayload, ISignedPartMetadata> payloadDigests;

    /**
     * Creates a new <code>SignatureProcessingResult</code> instance to indicate that there was a problem in processing
     * the signature part.
     *
     * @param failure   Exception indicating the problem that occurred
     */
    public SignatureProcessingResult(final SecurityProcessingException failure) {
        super(SecurityHeaderTarget.DEFAULT, failure);
        this.certificate = null;
        this.trustCheck = null;
        this.refMethod = null;
        this.algorithm = null;
        this.headerDigest = null;
        this.payloadDigests = null;
    }
    
    /**
     * Creates a new <code>SignatureProcessingResult</code> instance to indicate that processing of the signature part
     * completed successfully.
     *
     * @param signingCert           Certificate used to create the signature
     * @param certReferenceMethod   Method used to include/reference the certificate
     * @param algorithm             The signing algorithm
     * @param headerDigest          The digest meta-data for the ebMS header
     * @param payloadDigests        The payloads' digest meta-data
     */
    public SignatureProcessingResult(final X509Certificate signingCert, final X509ReferenceType certReferenceMethod,
							    	 final String algorithm, final ISignedPartMetadata headerDigest,
							    	 final Map<IPayload, ISignedPartMetadata> payloadDigests) {
    	this(signingCert, null, certReferenceMethod, algorithm, headerDigest, payloadDigests);
    }
    
    /**
     * Creates a new <code>SignatureProcessingResult</code> instance to indicate that processing of the signature part
     * completed successfully including a trust validation check.
     *
     * @param signingCert           Certificate used to create the signature
     * @param trustCheckResult		Results of the trust validation as reported by the <i>Certificate Manager</i>
     * @param certReferenceMethod   Method used to include/reference the certificate
     * @param algorithm             The signing algorithm
     * @param headerDigest          The digest meta-data for the ebMS header
     * @param payloadDigests        The payloads' digest meta-data
     * @since 5.0.0
     */
    public SignatureProcessingResult(final X509Certificate signingCert, final IValidationResult trustCheckResult, 
    								 final X509ReferenceType certReferenceMethod,
                                     final String algorithm, final ISignedPartMetadata headerDigest,
                                     final Map<IPayload, ISignedPartMetadata> payloadDigests) {
        super(SecurityHeaderTarget.DEFAULT);
        this.certificate = signingCert;
        this.trustCheck = trustCheckResult;
        this.refMethod = certReferenceMethod;
        this.algorithm = algorithm;
        this.headerDigest = headerDigest;
        this.payloadDigests = !Utils.isNullOrEmpty(payloadDigests) ? Collections.unmodifiableMap(payloadDigests) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate getSigningCertificate() {
        return certificate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509ReferenceType getCertificateReferenceType() {
        return refMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSignatureAlgorithm() {
        return algorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISignedPartMetadata getHeaderDigest() {
        return headerDigest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<IPayload, ISignedPartMetadata> getPayloadDigests() {
        return payloadDigests;
    }

	@Override
	public IValidationResult getTrustValidation() {		
		return trustCheck;
	}
}
