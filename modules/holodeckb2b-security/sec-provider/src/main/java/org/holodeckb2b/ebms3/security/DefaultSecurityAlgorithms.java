/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

import org.holodeckb2b.interfaces.security.X509ReferenceType;

/**
 * Defines constants representing the default security algorithms that the <i>default security provider</i> will use
 * when no explicit settings are configured in the P-Mode.
 * <p>
 * NOTE: Since version 7.0.0 there are two options to convey the symmetric encryption key to the receiver,
 * <i>key transport</i> and <i>key agreement</i>. The <i>key transport</i> method is used as default if the P-Mode does
 * not specify the method explicitly.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public final class DefaultSecurityAlgorithms {

    /**
     * The default signing algorithm is SHA-256
     */
    public static final String SIGNATURE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /**
     * The default encryption algorithm is AES128-GCM
     */
    public static final String ENCRYPTION = "http://www.w3.org/2009/xmlenc11#aes128-gcm";

    /**
     * The default algorithm for hash value calculations is SHA-256
     * <p>Note that the message digest algorithm is used both for signature and encryption (key transport)
     */
    public static final String DIGEST = "http://www.w3.org/2001/04/xmlenc#sha256";

    /**
     * The default method to reference the X509 certificate in the message is using the issuer and serial number
     */
    public static final X509ReferenceType   KEY_REFERENCE = X509ReferenceType.IssuerAndSerial;

    /**
     * The default algorithm for key transport when using encryption is RSA-OAEP
     */
    public static final String KEY_TRANSPORT = "http://www.w3.org/2009/xmlenc11#rsa-oaep";

    /**
     * The default MGF algorithm for key transport is MGF1 with SHA256
     */
    public static final String KEY_TRANSPORT_MGF = "http://www.w3.org/2009/xmlenc11#mgf1sha256";

    /**
     * The default algorithm for key agreement method when using encryption is ECDH-ES
     * @since 7.0.0
     */
    public static final String KEY_AGREEMENT = "http://www.w3.org/2009/xmlenc11#ECDH-ES";

    /**
     * The default algorithm for key enryption if the key agreement method is used with encryption is AES-KeyWrap 128
     * @since 7.0.0
     */
    public static final String KEY_ENCRYPTION = "http://www.w3.org/2001/04/xmlenc#kw-aes128";

    /**
     * The default algorithm for key derivation during key agreement when using encryption is ConcatKDF
     * @since 7.0.0
     */
    public static final String KEY_DERIVATION = "http://www.w3.org/2009/xmlenc11#ConcatKDF";

    /**
     * The default method to reference the receiver's certificate used in the key agreement method
     * @since 7.0.0
     */
    public static final X509ReferenceType CERT_REFERENCE = X509ReferenceType.BSTReference;

    /*
     * This class should not be instantiated
     */
    private DefaultSecurityAlgorithms() {};
}
