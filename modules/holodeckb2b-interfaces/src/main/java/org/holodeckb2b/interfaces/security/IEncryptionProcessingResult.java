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
import java.util.Map;

import org.holodeckb2b.interfaces.storage.IPayloadEntity;

/**
 * Represents the results of processing of the encryption part of message level security. It provides information on the
 * certificate used to encrypt the symmetric key and the algorithms used to encrypt the symmetric key and the payload
 * data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
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
     * Gets algorithm that was used to encrypt the message. The returned value is the algorithm identifier as defined by
     * the message protocol's used security specification. For ebMS3/AS4 these are defined in the in the <i
     * >XML Encryption Syntax and Processing</i> specification and corresponds to the <code>
     * xenc:EncryptedData/xenc:EncryptionMethod</code> elements. Although each encrypted part could use its own specific
     * encryption algorithm it is assumed that one is used for all parts of the message.
     *
     * @return  The encryption algorithm
     */
    String getEncryptionAlgorithm();

    /**
     * Gets the meta-data on how the <i>symmetric key</i> is exchanged between the communication partners.
     * Note that this information may not (all) be available for different messaging protocols.
     *
     * @return The key exchange meta-data
     */
    IKeyExchangeInfo   getKeyExchangeInfo();

    /**
     * Gets the encrypted payloads.
     *
     * @return  List of encrypted payloads
     */
    Collection<IPayloadEntity>  getEncryptedPayloads();

    /**
     * Abstract base interface to provide information on the method that was used to exchange the symmetric enryption
     * key between the communication partners. This can either done through embedding the key in the message using a
     * <i>key transport</i> mechanism or by encrypting the key using another key derived from the partner's
     * certificates through a <i>key agreement method</i>.
     */
    public abstract interface IKeyExchangeInfo {};

    /**
     * Provides access to the meta-data on the <i>symmetric key</i> was exchanged between the communication partners.
     * For WS-Security this corresponds to the <code>xenc:EncryptedKey/xenc:EncryptionMethod</code> element in the
     * header. Note that this only includes information about the protection of symmetric key, not the key itself.
     * <p>This interface is based on the currently specified key transport algorithms in the <i>XML Encryption Syntax
     * and Processing Version 1.1</i> specification.<br>
     *
     * @author Sander Fieten (sander at holodeck-b2b.org)
     * @since 4.0.0
     */
    public interface IKeyTransportInfo extends IKeyExchangeInfo {

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

    /**
     * Provides access to the meta-data on the key agreement method used to exchange the <i>symmetric key</i> between
     * the communication partners.
	 * <p>This interface is based on the currently specified key transport algorithms in the <i>XML Encryption Syntax
     * and Processing Version 1.1</i> specification.<br>
     *
     * @author Sander Fieten (sander at holodeck-b2b.org)
     * @since 7.0.0
     */
    public interface IKeyAgreementInfo extends IKeyExchangeInfo {
    	/**
    	 * Gets the key agreement method that was used to derive the key that was used to encrypt the symmetric key for
    	 * the actual encryption of the payloads.
    	 *
    	 * @return	the key agreement method
    	 */
    	String getKeyAgreementMethod();

        /**
         * Gets the settings of the key derivation method to be used within this key agreement.
         *
         * @return  the key derivation method settings
         */
    	IKeyDerivationInfo getKeyDerivationInfo();

    	/**
         * Gets the additional parameters of the key agreement method. Depending on the chosen key agreement method
         * there may be other parameters needed in addition to digest algorithm.
         *
         * @return Map containing the additional parameters
         */
    	Map<String, ?> getParameters();
    }

    /**
     * Provides access to the meta-data on the key agreement method used to exchange the <i>symmetric key</i> between
     * the communication partners.
     *
     * @author Sander Fieten (sander at holodeck-b2b.org)
     * @since 7.0.0
     */
    public interface IKeyDerivationInfo {
    	/**
    	 * Gets the key derivation algorithm.
    	 *
    	 * @return  URI of the key derivation algorithm to be used as defined in XMLENC-core1
		 */
    	String getKeyDerivationAlgorithm();

        /**
         * Gets the digest algorithm to be used for key agreement.
         *
         * @return  URI the digest algorithm to be used with the key derivation algorithm
         */
    	String getDigestAlgorithm();

        /**
         * Gets the additional parameters of the key derivation method. Depending on the chosen derivation algorithm
         * there may be other parameters needed in addition to digest algorithm.
         *
         * @return Map containing the additional parameters
         */
    	Map<String, ?> getParameters();
    }
}
