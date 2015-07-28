/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.common.security;

/**
 * Defines the KeyTransport parameters within the encryption configuration of a P-Mode.
 * <p>See <a href="http://www.w3.org/TR/xmlenc-core1/#sec-Alg-KeyTransport">section 5.5 in XML Encryption Syntax and 
 * Processing Version 1.1</a> for more information about key transport.
 * <p>As noted in <a href="https://issues.oasis-open.org/browse/EBXMLMSG-45">issue 45 of the OASIS TC issue tracker</a>
 * there are no P-Mode parameters defined in the ebMS specification for configuration of key transport.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IKeyTransport {
    
    /**
     * Gets the key transport algorithm.
     * <p>If "http://www.w3.org/2009/xmlenc11#rsa-oaep"is used as key transport algorithm then also a MGF and digest
     * algorithm must be specified.
     * 
     * @return String containing the id of the key transport algorithm to be used as defined in XMLENC-core1
     */
    public String getAlgorithm();
    
    /**
     * Gets the mask generation function (MGF) algorithm to use in case <i>rsa-oaep</i> is used as key transport 
     * algorithm.
     * 
     * @return String containing the id the MGF algorithm to be used as defined in XMLENC-core1.
     */
    public String getMGFAlgorithm();
    
    /**
     * Gets the digest algorithm to use in case <i>rsa-oaep</i> is used as key transport algorithm.
     * 
     * @return String containing the id the digest algorithm to be used as defined in XMLENC-core1
     */
    public String getDigestAlgorithm();
    
}
