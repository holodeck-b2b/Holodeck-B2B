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
import java.util.Collection;

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
     * Gets the X509 certificate that was used to create the signature of the message. This corresponds to the
     * certificate referenced in the <code>ds:Signature/ds:KeyInfo</code> element. Note that the certificate itself does
     * not need to be included in the message to be returned here. It can be retrieved from the public keystore based
     * on the reference included in the message.
     *
     * @return The certificate used for signing.
     */
    X509Certificate getSigningCertificate();

    /**
     * Gets algorithm that was used to create the signature of the message. The returned value is the algorithm
     * identifier as defined in the <i>XML Signature Syntax and Processing</i> specification and corresponds to the
     * <code>ds:Signature/ds:SignatureMethod</code> element.
     *
     * @return  The signature algorithm
     */
    String getSignatureAlgorithm();

    /**
     * Gets the information on the digests of the payloads included in the message. This corresponds to the information
     * in the <code>ds:Reference</code> elements.
     * <p>NOTE: It is assumed that each payload contained in the message is signed separately, i.e. has its own <code>
     * ds:SignedInfo/ds:Reference</code> element, so the <code>URI</code> attribute of the <code>ds:Reference</code>
     * element can be used to identify the payload.
     *
     * @return The information on the digests of the payload
     */
    Collection<IPayloadDigest> getPayloadDigests();
}
