/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.interfaces.security.IPayloadDigest;

/**
 * Implements {@link IPayloadDigest} and contains information about the digest that was calculated for a payload of a
 * User Message. The information on the digest must be supplied when the object is created and can not be modified
 * afterwards.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public class PayloadDigest implements IPayloadDigest {

    private static final String DSIG_NAMESPACE_URI = "http://www.w3.org/2000/09/xmldsig#";

    private final String  uri;
    private final String  value;
    private final String  algorithm;

    /**
     * Creates a new <code>PayloadDigest</code> object with the provided information on the digest.
     *
     * @param uri               The reference to the payload this digest applies to as an URI
     * @param digestValue       The digest value that was calculated for this payload
     * @param digestAlgorithm   The algorithm used to calculate the digest value
     */
    public PayloadDigest(final String uri, final String digestValue, final String digestAlgorithm) {
        this.uri = uri;
        this.value = digestValue;
        this.algorithm = digestAlgorithm;
    }

    /**
     * Creates a new <code>PayloadDigest</code> object with the information on the digest provided in the <code>
     * ds:Reference</code> element.
     *
     * @param reference     The {@link OMElement} object representing the <code>ds:Reference</code> element
     */
    public PayloadDigest(final OMElement reference) {
        // Only a ds:Reference element canbe used to create a PayloadDigest object
        if(!"Reference".equals(reference.getLocalName())
           || !DSIG_NAMESPACE_URI.equals(reference.getNamespaceURI()))
            throw new IllegalArgumentException("Argument must be a ds:Reference element!");

        this.uri = reference.getAttributeValue(new QName("URI"));
        try {
            this.value = reference.getFirstChildWithName(new QName(DSIG_NAMESPACE_URI, "DigestValue")).getText();
            this.algorithm = reference.getFirstChildWithName(new QName(DSIG_NAMESPACE_URI, "DigestMethod"))
                                      .getAttributeValue(new QName("Algorithm"));
        } catch (final NullPointerException npe) {
            // A NPE indicates that a required element or attribute was not available => illegal argumen
            throw new IllegalArgumentException("The passed ds:Reference element is invalid!");
        }
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public String getDigestValue() {
        return value;
    }

    @Override
    public String getDigestAlgorithm() {
        return algorithm;
    }
}
