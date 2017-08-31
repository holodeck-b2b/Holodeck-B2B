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

import java.util.List;
import org.apache.axiom.om.OMElement;

/**
 * Is an interface that represents a digest that was calculated for a part of a message, e.g. the ebMS header or a
 * payload of a <i>User Message</i>.
 * <p>It contains the information that is available in the <code>ds:SignedInfo/ds:Reference</code> elements in the
 * Signature of the message, which beside the digest itself is also the digest and transform algorithms.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface ISignedPartMetadata {

    /**
     * Gets the base64 encoded digest value that was calculated for the payload. This is the value from the <code>
     * ds:DigestValue</code> child element of the <code>ds:Reference</code> element in the signature.
     *
     * @return String containing the base64 encoded digest value
     */
    String getDigestValue();

    /**
     * Gets the identifier of the algorithm that was used to calculate the digest. The identifiers are defined in the
     * <i>XML Signature Syntax and Processing</i> specification. This is the value from the <code>Algorithm</code>
     * attribute in child element <code>ds:DigestMethod</code> of <code>ds:Reference</code> in the signature.
     *
     * @return String containing the algorithm that was used to calculate the digest value.
     */
    String getDigestAlgorithm();

    /**
     * Gets a list of meta-data about the transformations that were applied to the content before the digest was
     * calculated. The <i>Web Services Security: SOAP Message Security</i> specification defines how transformation
     * should be used when signing SOAP messages. The returned list of transforms corresponds to <code>
     * ds:Transform</code> descendants of the <code>ds:Reference</code> element.
     *
     * @return List of the transforms applied to the payload before calculating the digest value.
     * @since HB2B_NEXT_VERSION
     */
    List<ITransformMetadata> getTransforms();

    /**
     * Is an interface that represents a transformation that was applied to the source data before a digest was
     * calculated as specified in the <i>XML Signature Syntax and Processing Version 1.1</i> specification.
     * <p>The information consists of the transformation algorithm used and optional parameters to it. Because the
     * parameters are not defined in the specification and can be any element there are included as is.
     */
    public interface ITransformMetadata {

        /**
         * Gets the identifier of the algorithm that was used to transform the part before the digest value was
         * calculated.
         *
         * @return  String containing the algorithm that was used to transform the part
         */
        String getTransformAlgorithm();

        /**
         * Gets the transformation parameters as contained in the security header, i.e. as child elements of the <code>
         * ds:Transform</code> element.
         *
         * @return Collection of XML elements representing the parameters of the transformation
         */
        List<OMElement>   getTransformParameters();
    }
}
