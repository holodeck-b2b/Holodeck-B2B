/**
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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

import java.util.Map;

import org.holodeckb2b.interfaces.security.X509ReferenceType;


/**
 * Defines the <i>KeyAgreement</i> parameters within the encryption configuration of a P-Mode. These parameters are used
 * to construct the <code>xenc:EncryptedKey</code> element in the WS-Security header of the message.
 * <p>See <a href="http://www.w3.org/TR/xmlenc-core1/#sec-Alg-KeyAgreement">section 5.6 in XML Encryption Syntax and
 * Processing Version 1.1</a> for more information about the algorithms for key agreement.
 * <p>As noted in <a href="https://issues.oasis-open.org/browse/EBXMLMSG-45">issue 45 of the OASIS TC issue tracker</a>
 * there are no P-Mode parameters defined in the ebMS specification for configuration of key transport nor agreement.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public interface IKeyAgreement {

    /**
     * Gets the key encryption algorithm.
     *
     * @return  String containing the id of the key encryption algorithm to be used as defined in XMLENC-core1
     */
    String getKeyEncryptionAlgorithm();

    /**
     * Gets the key agreement method.
     *
     * @return  String containing the id of the key agreement algorithm to be used as defined in XMLENC-core1
     */
    String getAgreementMethod();

    /**
     * Gets the settings of the key derivation method to be used within this key agreement.
     *
     * @return  the key derivation method settings
     */
    IKeyDerivationMethod getKeyDerivationMethod();

    /**
     * Gets the method of referencing or including the receiver's certificate within this key agreement.
     * <p>
     * NOTE: The receiver's certificate (reference) is included in the <code>xenc:RecipientKeyInfo</code> element which
     * is of the same type as the <code>ds:KeyInfo</code> element. But since the use of the <i>key agreement</i> method
     * for exchanging the symmetric key is not specified in the WS-Security specification it is not clear how the
     * receiver's certificate should be included or referenced in the <code>xenc:RecipientKeyInfo</code> element.<br/>
     * For now we use the same options as specified in section 3.2 of the <a href="http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-x509TokenProfile-v1.1.1-os.html#_Toc307416641">
     * WS-Security X.509 Certificate Token Profile Version 1.1.1 specification<ol>
     * <li>Subject Key Identifier</li>
     * <li>Issuer and Serial number</li>
     * <li>Inclusion of the complete certificate</li>
     * </ol>
     * Based on the XMLenc specification these can be included using the <code>ds:X509Data</code> element as child of
     * the <code>xens:RecipientKeyInfo</code> element. If however the methodology of the WS-Security specification would
     * be followed the <code>wss:SecurityTokenReference</code> element should be used instead. Which element is included
     * is left to the <i>Security Provider</i> implementation. The complete certificate can be either directly included
     * in a <code>xs:X509Certificate</code> child of <code>ds:X509Data</code> or in a separate <code>
     * wss:BinarySecurityToken</code> WS-Security header element that is referenced from <code>wss:SecurityTokenReference
     * </code>. For both options the {@link X509ReferenceType#BSTReference} value should be used.
     *
     * @return  The method to be used for referencing the receiver's certificate
     */
    X509ReferenceType getCertReferenceMethod();

    /**
     * Gets the additional parameters of the key derivation method. Depending on the chosen derivation algorithm there
     * may be other parameters needed in addition to digest algorithm.
     *
     * @return Map containing the additional parameters
     */
    Map<String, ?> getParameters();
}
