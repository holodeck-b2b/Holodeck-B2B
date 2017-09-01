package org.holodeckb2b.security.results;

/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.security.SecurityConstants;
import org.holodeckb2b.security.util.Axis2XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implements {@link ISignedPartMetadata} and contains the meta-data about a digest that was calculated or included in
 * the message for a part of a message unit which can either the ebMS message header or a payload.
 * <p>The information on the digest must be supplied when the object is created and can not be modified afterwards.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SignedPartMetadata implements ISignedPartMetadata {

    private final String                    digest;
    private final String                    algorithm;
    private final List<ITransformMetadata>   transforms;

    /**
     * Creates a new <code>SignedPartMetadata</code> instance using the information from the provided <code>
     * ds:Reference</code> element.
     *
     * @param reference     The DOM representation of the <code>ds:Reference</code> element
     */
    public SignedPartMetadata(final Element reference) {
        // Only a ds:Reference element canbe used to create a PayloadDigest object
        if(!SecurityConstants.REFERENCE_ELEM.getLocalPart().equals(reference.getLocalName())
           || !SecurityConstants.REFERENCE_ELEM.getNamespaceURI().equals(reference.getNamespaceURI()))
            throw new IllegalArgumentException("Argument must be a ds:Reference element!");

        String digestValue = null;
        String digestAlgorithm = null;
        List<ITransformMetadata> digestTransforms = new ArrayList<>();
        NodeList children = reference.getChildNodes();
        for (int i = 0; i  < children.getLength(); i++) {
            Node child = children.item(i);
            switch (child.getLocalName()) {
                case "DigestValue" :
                    digestValue = child.getTextContent();
                    break;
                case "DigestMethod" :
                    digestAlgorithm = ((Element) child).getAttribute("Algorithm");
                    break;
                case "Transforms" :
                    NodeList transformElems = ((Element) child).getElementsByTagNameNS(
                                                                 SecurityConstants.DSIG_NAMESPACE_URI, "Transform");
                    for (int j = 0; j < transformElems.getLength(); j++)
                        digestTransforms.add(new TransformMetadata((Element) transformElems.item(j)));
            }
        }
        // Use collected info to initialize instance
        this.algorithm = digestAlgorithm;
        this.digest = digestValue;
        this.transforms = Collections.unmodifiableList(digestTransforms);
    }


    @Override
    public String getDigestValue() {
        return digest;
    }

    @Override
    public String getDigestAlgorithm() {
        return algorithm;
    }

    @Override
    public List<ITransformMetadata> getTransforms() {
        return transforms;
    }

    /**
     * Implements the {@link ITransformMetadata} interface and contains the meta-data about a transformation that was
     * applied before the digest value was calculated.
     */
    public class TransformMetadata implements ITransformMetadata {

        /**
         * The transformation algorithm
         */
        private final String algorithm;
        /**
         * The transformation parameters as XML elements
         */
        private final List<OMElement>   parameters;

        /**
         * Creates a new <code>TransformMetadata</code> instance using the information from the provided <code>
         * ds:Transform</code> element.
         *
         * @param transform     The DOM representation of the <code>ds:Transform</code> element
         */
        protected TransformMetadata(final Element transform) {
            this.algorithm = transform.getAttribute("Algorithm");

            List<OMElement> paramElems = new ArrayList<>();
            NodeList children = transform.getChildNodes();
            if (children != null && children.getLength() > 0) {
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element)
                        paramElems.add(Axis2XMLUtils.convertDOMElementToAxiom((Element) child));
                }
            }
            if (!Utils.isNullOrEmpty(paramElems))
                this.parameters = Collections.unmodifiableList(paramElems);
            else
                this.parameters = null;
        }

        @Override
        public String getTransformAlgorithm() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List<OMElement> getTransformParameters() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
