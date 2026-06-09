/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.pmode;

import java.util.Collections;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;

/**
 * Defines the configuration for encrypting and decrypting the ebMS message depending on the direction (outgoing or
 * incoming) of the message.
 * <p>The settings defined by the interface correspond with the P-Mode parameter group
 * <b>PMode[1].Security.X509.Encryption</b>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public interface IEncryptionConfiguration {

    /**
     * Gets the <i>alias</i> that the X509 certificate/key pair which is used for encryption/decryption of the messages
     * is registered under with the installed <i>Certificate Manager</i>.
     *
     * @return  The alias that identifies the certificate to use for the encryption.
     * @deprecated {@link #getDecryptionKeypairs()} and {@link #getEncryptionCertificate()} should be implemented and
     *  			used to provide/get the keypair(s)/certificate for decryption/encryption of messages.
     */
	@Deprecated(since = "9.0.0", forRemoval = true)
    default String getKeystoreAlias() {
		throw new UnsupportedOperationException();
	}

    /**
     * Gets the password to access the private key hold by the key pair. Only applies to configurations that are used to
     * decrypt messages.
     *
     * @return  The password to get access to the private key
     * @deprecated {@link #getDecryptionKeypairs()} should be implemented and used instead
     */
    @Deprecated(since = "9.0.0", forRemoval = true)
    default String getCertificatePassword() {
    	throw new UnsupportedOperationException();
    }

    /**
     * Gets the list of <i>aliases</i> and associated <i>passwords</i> under which the key pair(s) that can used for
     * decryption of the messages are registered with the installed <i>Certificate Manager</i>.
     *
     * @return  A map of alias and password combinations that identify the key pairs to use for decryption.
     * @since 9.0.0	for backwards compatibility a default implementation is provided that will return the
	 * 				single alias/password combination provided by the old methods.
     */
    default Map<String, String> getDecryptionKeypairs() {
    	return !Utils.isNullOrEmpty(getKeystoreAlias()) ? Map.of(getKeystoreAlias(), getCertificatePassword()) :
    			Collections.emptyMap();
	}

    /**
     * Gets the alias of the <i>partner certificate</i> that should be used for the encryption of the message.
     *
     * @return  The alias that identifies the certificate to use for the encryption
     * @since 9.0.0 for backwards compatibility a default implementation is provided that will return the alias provided
     * 				by the old method.
     */
    default String getEncryptionCertificate() {
    	return getKeystoreAlias();
    }

    /**
     * Gets the symmetric encryption algorithm (to be) used for the encryption of the message.
     * <p>If not specified Holodeck B2B will use the <i>AES128</i> algorithm as default. Note that there are doubts
     * about the strength of this algorithm for use with XML encryption
     * [<a href="http://www.nds.ruhr-uni-bochum.de/research/publications/breaking-xml-encryption">XMLENC-CBC-ATTACK</a>]
     * and it is RECOMMENDED to use a stronger algorithm. These however are only supported in XMLenc version 1.1 which
     * is currently not specified in ebMS v3 and AS4 (see <a href="https://issues.oasis-open.org/browse/EBXMLMSG-40">
     * issue #40 in issuetracker</a> of the OASIS ebMS TC.
     *
     * @return The symmetric encryption algorithm to be used, or<br>
     *         <code>null</code> when not specified.
     */
    String getAlgorithm();

    /**
     * Gets the settings to create the <code>xenc:EncryptedKey</code> element in the WS-Security header of the message
     * in case the symmetric encryption key must be packaged in the message using the <i>key transport</i> method.
     * <p>NOTE 1: Either these settings or the settings for <i>key agreement</i> should be specified, but not both.<br/>
     * NOTE 2: Specification of these parameters is optional. In case nothing is specified, the installed <i>security
     * provider</i> will use default settings.
     *
     * @return  An {@link IKeyTransport} object containing the key transport parameters, or<br>
     *          <code>null</code> if not specified
     */
    IKeyTransport getKeyTransport();

    /**
     * Gets the settings to create the <code>xenc:EncryptedKey</code> element in the WS-Security header of the message
     * in case the symmetric encryption key must should be derieved from the certificates included in the message using
     * a <i>key agreement</i> method.
     * <p>NOTE 1: Either these settings or the settings for <i>key transport</i> should be specified, but not both.<br/>
     * NOTE 2: Specification of these parameters is optional. In case nothing is specified, the installed <i>security
     * provider</i> will use default settings.
     *
     * @return  An {@link IKeyAgreement} object containing the key agreement parameters, or<br>
     *          <code>null</code> if not specified
     * @since 7.0.0
     */
    IKeyAgreement getKeyAgreement();
}
