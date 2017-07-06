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
import javax.xml.crypto.dsig.Transform;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Represents a digest that was calculated for a payload of a <i>User Message</i>. It contains the information that is
 * available in the <code>ds:SignedInfo/ds:Reference</code> elements in the Signature of the message, which beside the
 * digest itself is also the digest and transform algorithms.
 * <p>It is assumed that each payload contained in the message is signed separately, i.e. has its own <code>
 * ds:SignedInfo/ds:Reference</code> element, so the <code>URI</code> attribute of the <code>ds:Reference</code> element
 * can be used to identify the payload.<br>
 * Note that this URI may be different from the URI that is assigned to the payload ({@link IPayload#getPayloadURI()})
 * when it is included in the SOAP Body and the SOAP Body is signed as a whole.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 */
public interface IPayloadDigest {

    /**
     * Gets the reference to the payload this digest applies to. This is the value of the <code>URI</code> attribute of
     * the <code>ds:Reference</code> element in the signature.
     *
     * @return String containing the URI that point to the payload in the MIME package.
     */
    String getURI();

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
     * Gets the list of transformations that were applied to the payload content before the digest was calculated. The
     * <i>Web Services Security: SOAP Message Security</i> specification defines how transformation should be used when
     * signing SOAP messages. The returned list of transforms corresponds to <code>ds:Transform</code> descendants of
     * the <code>ds:Reference</code> element.
     *
     * @return List of the transforms applied to the payload before calculating the digest value.
     * @since HB2B_NEXT_VERSION
     */
    List<Transform> getTransforms();
}
