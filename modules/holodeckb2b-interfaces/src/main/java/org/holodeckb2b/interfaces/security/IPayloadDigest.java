/*
 * Copyright (C) 2016 The Holodeck B2B Team.
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

/**
 * Represents a digest that was calculated for a payload of a <i>User Message</i>. It contains the information that is
 * available in the <code>ds:Reference</code> elements in the Signature of the message, which beside the digest itself
 * is also the digest and transform algorithms.<br>
 * The reference to the payload is provided by the URI that identifies the payload in the MIME package. As a result
 * objects of this type need to be related to the actual payload through the context in which they are used.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public interface IPayloadDigest {

    /**
     * Gets the reference to the payload this digest applies to. This is the value of the <code>URI</code> attribute of
     * the <code>ds:Reference</code> element in the signature.
     * 
     * @return String containing the URI that point to the payload in the MIME package. 
     */
    public String getURI();
    
    /**
     * Gets the base64 encoded digest value that was calculated for the payload. This is the value from the <code>
     * ds:DigestValue</code> child element of the <code>ds:Reference</code> element in the signature.
     * 
     * @return String containing the base64 encoded digest value
     */
    public String getDigestValue();
    
    /**
     * Gets the identifier of the algorithm that was used to calculate the digest. The identifiers are defined in the 
     * <i>XML Signature Syntax and Processing</i> specification. This is the value from the <code>Algorithm</code> 
     * attribute in child element <code>ds:DigestMethod</code> of <code>ds:Reference</code> in the signature.
     * 
     * @return String containing the algorithm that was used to calculate the digest value.
     */
    public String getDigestAlgorithm();    
}
