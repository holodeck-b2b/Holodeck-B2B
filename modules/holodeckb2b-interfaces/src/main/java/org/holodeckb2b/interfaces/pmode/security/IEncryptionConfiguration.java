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
package org.holodeckb2b.interfaces.pmode.security;

/**
 * Defines the configuration for encrypting and decrypting the ebMS message depending on the direction (outgoing or
 * incoming) of the message.
 * <p>The settings defined by the interface correspond with the P-Mode parameter group
 * <b>PMode[1].Security.X509.Encryption</b>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public interface IEncryptionConfiguration {

    /**
     * Gets the Java keystore <i>alias</i> that identifies the X509 certificate that should be used for encryption /
     * decryption.
     * <p>The current implementation of Holodeck B2B uses Java keystores to store certificates. Two keystores are used
     * to storing private and public certificates and another for storing CA certificates (the trust store). Depending
     * what this configuration applies to the certificate must exist in either the private (when decrypting incoming
     * messages) or public (when encrypting outgoing messages) keystore.
     *
     * @return  The alias that identifies the certificate to use for the encryption.
     */
    public String getKeystoreAlias();

    /**
     * Gets the password to access the private key hold by the certififcate. Only applies to configurations that are
     * used to decrypt messages.
     * <p>Current implementation of Holodeck B2B requires that result is the password in clear text. Future version may
     * change this to get better secured passwords.
     *
     * @return  The password to get access to the private key
     */
    public String getCertificatePassword();

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
    public String getAlgorithm();


    /**
     * Gets the key transport settings that determine how the <code>xenc:EncryptedKey</code> element in the WS-Security
     * header of the message must be created. These settings therefore apply only to the sending side of a message
     * exchange.
     * <p>Specification of these parameters is optional. Holodeck B2B will use the following defaults if not specified:
     * <ul><li>Key Transport Algorithm = RSA-OAEP (including MGF1 with SHA1)</li>
     * <li>Message Digest Algorithm = SHA256</li>
     * <li>Key Reference Method  = Issuer and Serial number</li>
     * </ul>
     *
     * @return  An {@link IKeyTransport} object containing the key transport parameters, or<br>
     *          <code>null</code> if not specified
     */
    public IKeyTransport getKeyTransport();

}
