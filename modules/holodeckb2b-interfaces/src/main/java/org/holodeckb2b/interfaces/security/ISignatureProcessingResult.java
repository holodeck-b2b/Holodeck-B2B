/*
 * Copyright (C) 2017 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.security;

import java.security.cert.X509Certificate;
import java.util.Map;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Represents the results of processing of the signature part of the WS-Security header. It provides information on the
 * certificate used to sign the message, the signature algorithm and the calculated digests for the payloads in the
 * message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface ISignatureProcessingResult extends ISecurityProcessingResult {

    /**
     * Gets the X509 certificate used to create the signature of the message. This corresponds to the certificate
     * referenced in the <code>ds:Signature/ds:KeyInfo</code> element. Note that the certificate itself does not need to
     * be included in the message to be returned here, it can be retrieved from the private keys managed by the security
     * provider based on the reference included in the message.
     *
     * @return The certificate used for signing.
     */
    X509Certificate getSigningCertificate();

    /**
     * Gets the type of security token reference used to point to the certificate that includes the public key that
     * corresponds to the private key used to sign the message.
     *
     * @return The token reference type used
     */
    X509ReferenceType getCertificateReferenceType();

    /**
     * Gets algorithm that was used to create the signature of the message. The returned value is the algorithm
     * identifier as defined in the <i>XML Signature Syntax and Processing</i> specification and corresponds to the
     * <code>ds:Signature/ds:SignatureMethod</code> element.
     *
     * @return  The signature algorithm
     */
    String getSignatureAlgorithm();

    /**
     * Gets the information on the digest calculated for the ebMS SOAP header. This corresponds to the information
     * in the <code>ds:Reference</code> element referencing the ebMS header.
     * <p>NOTE: As the ebMS header is signed as a whole this digest covers the ebMS meta-data of all message units
     * included in the messages.
     *
     * @return The information on the digest of the ebMS message header
     */
    ISignedPartMetadata getHeaderDigest();

    /**
     * Gets the information on the digests of the payloads included in the message. This corresponds to the information
     * in the <code>ds:Reference</code> elements.
     * <p>NOTE: It is assumed that each payload contained in the message is signed separately, i.e. has its own <code>
     * ds:SignedInfo/ds:Reference</code> element, so the <code>URI</code> attribute of the <code>ds:Reference</code>
     * element can be used to identify the payload. When multiple XML payloads are included in the SOAP body it may not
     * be possible to identify the individual payloads as the SOAP Body is signed as a whole.
     *
     * @return The information on the digests of the payloads
     */
    Map<IPayload, ISignedPartMetadata> getPayloadDigests();
}
