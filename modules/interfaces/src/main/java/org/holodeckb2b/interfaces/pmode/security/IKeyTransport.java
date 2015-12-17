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
package org.holodeckb2b.interfaces.pmode.security;

/*
 * #%L
 * Holodeck B2B - Interfaces
 * %%
 * Copyright (C) 2015 The Holodeck B2B Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

/**
 * Defines the <i>KeyTransport</i> parameters within the encryption configuration of a P-Mode. This parameters are used
 * to construct the <code>xenc:EncryptedKey</code> element in the WS-Security header of the message.
 * <p>See <a href="http://www.w3.org/TR/xmlenc-core1/#sec-Alg-KeyTransport">section 5.5 in XML Encryption Syntax and 
 * Processing Version 1.1</a> for more information about the algorithms for key transport.
 * <p>As noted in <a href="https://issues.oasis-open.org/browse/EBXMLMSG-45">issue 45 of the OASIS TC issue tracker</a>
 * there are no P-Mode parameters defined in the ebMS specification for configuration of key transport.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IKeyTransport {
    
    /**
     * Gets the key transport algorithm.
     * <p>If <i>RSA-OAEP</i> is used as key transport algorithm then also a MGF algorithm must be specified, i.e. 
     * {@link #getMGFAlgorithm()} MUST NOT return <code>null</code>.
     * <p>The default value is <i>RSA-OAEP (including MGF1 with SHA1)</i>.
     * 
     * @return  String containing the id of the key transport algorithm to be used as defined in XMLENC-core1, or<br>
     *          <code>null</code> if not specified
     */
    public String getAlgorithm();
    
    /**
     * Gets the mask generation function (MGF) algorithm to use in case <i>rsa-oaep</i> is used as key transport 
     * algorithm.
     * 
     * @return  String containing the id the MGF algorithm to be used as defined in XMLENC-core1, or<br>
     *          <code>null</code> if not specified
     */
    public String getMGFAlgorithm();
    
    /**
     * Gets the message digest algorithm for key transport. 
     * <p>The default value is <i>SHA256</i>.
     * 
     * @return  String containing the id the digest algorithm to be used as defined in XMLENC-core1, or<br>
     *          <code>null</code> if not specified
     */
    public String getDigestAlgorithm();

    /**
     * Gets the method of referencing the certificate used for encryption. Section 3.2 of the WS-Security X.509 
     * Certificate Token Profile Version 1.1.1 specification defines the three options that are allowed for referencing 
     * the certificate.
     * <p>NOTE 1: This setting only applies when encrypting the message, i.e. for outgoing messages. When the incoming 
     * message is decrypted the reference included in the message will be used to look up the certificate regardless 
     * of the used reference method. 
     * <p>NOTE 2: When not specified in the P-Mode Holodeck B2B will reference the certificate using its issuer and 
     * serial number (as specified in section 3.2.3 of WS-Sec X.509 CTP).
     * 
     * @return  The method to be used for referencing the certificate, or<br>
     *          <code>null</code> if no method is specified
     */
    public X509ReferenceType getKeyReferenceMethod();
}
