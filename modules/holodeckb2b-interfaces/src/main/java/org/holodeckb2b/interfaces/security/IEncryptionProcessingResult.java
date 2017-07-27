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
 * Represents the results of processing of the encryption part of the WS-Security header. It provides information on the
 * certificate used to encrypt the symmetric key and the algorithms used to encrypt the symmetric key and the payload
 * data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface IEncryptionProcessingResult extends ISecurityProcessingResult {

    /**
     * Gets the X509 certificate used for encryption of the message. This corresponds to the certificate referenced in
     * the <code>xenc:EncryptedKey/ds:KeyInfo</code> element. Note that the certificate itself does not need to be
     * included in the message to be returned here, it can be retrieved from the private keys managed by the security
     * provider based on the reference included in the message.
     *
     * @return The certificate used for encryption.
     */
    X509Certificate getEncryptionCertificate();

    /**
     * Gets the type of security token reference used to point to the certificate that includes the public key that was
     * used to encrypt the message.
     *
     * @return The token reference type used
     */
    X509ReferenceType getCertificateReferenceType();

    /**
     * Gets the <i>key transport</i> meta-data on how the <i>symmetric key</i> is included in the message.
     *
     * @return The key transport meta-data
     */
    IKeyTransportInfo   getKeyTransportInfo();

    /**
     * Gets the references to the encrypted payloads. These correspond to the URIs as specified in the <code>href</code>
     * attribute of the <code>eb:PartInfo</code> element of the payload. Because the encryption may replace the complete
     * payload and assign new identifiers there may be no direct relation with references in the security header.
     *
     * @return  List of references to the encrypted payloads
     */
    Collection<String>  getEncryptedPayloadURIs();

    /**
     * Provides access to the meta-data on how the <i>symmetric key</i> is included in the WS-Security header. This
     * corresponds to the <code>xenc:EncryptedKey/xenc:EncryptionMethod</code> element in the header. Note that this
     * only includes information about the protection of symmetric key, not the key itself.
     * <p>This interface is based on the currently specified key transport algorithms in the <i>XML Encryption Syntax
     * and Processing Version 1.1</i> specification.
     *
     * @author Sander Fieten (sander at holodeck-b2b.org)
     * @since HB2B_NEXT_VERSION
     */
    public interface IKeyTransportInfo {

        /**
         * Gets the key transport algorithm that was used to protect the symmetric key that was used for the actual
         * encryption of the payloads.
         * <p>The allowed key transport algorithms and their identifiers are defined in the <i>XML Encryption Syntax
         * and Processing Version 1.1</i> specification.
         *
         * @return The key transport algorithm
         */
        String getKeyTransportAlgorithm();

        /**
         * Gets the digest algorithm that was used for the key transport of the symmetric key when the <i>RSA-OAEP</i>
         * algorithm is used.
         * <p>The allowed algorithms and their identifiers are defined in the <i>XML Encryption Syntax and Processing
         * Version 1.1</i> specification.
         *
         * @return The digest algorithm used in key transport
         */
        String getDigestMethod();

        /**
         * Gets the mask generation function algorithm for the key transport of the symmetric key when the <i>
         * RSA-OAEP</i> algorithm is used.
         * <p>The allowed algorithms and their identifiers are defined in the <i>XML Encryption Syntax and Processing
         * Version 1.1</i> specification.
         *
         * @return  The mask generation function used in key transport
         */
        String getMGFAlgorithm();
    }
}
