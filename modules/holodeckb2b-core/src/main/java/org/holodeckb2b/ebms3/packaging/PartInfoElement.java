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
package org.holodeckb2b.ebms3.packaging;

import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is a helper class for handling the ebMS <code>PartInfo</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.13 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartInfoElement {

    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");

    /**
     * The local name of the href attribute
     */
    private static final String HREF_ATTR = "href";

    /**
     * Creates a <code>PartInfo</code> element and adds it to the given <code>PayloadInfo</code> element.
     *
     * @param plElement     The <code>PayloadInfo</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement plElement, final IPayload data) {
        final OMFactory f = plElement.getOMFactory();

        // Create the element
        final OMElement piElement = f.createOMElement(Q_ELEMENT_NAME, plElement);

        // Fill it based on the given data

        // href attribute
        String href = data.getPayloadURI();
        if (href != null && !href.isEmpty()) {
            // Add prefixes for different reference
            if (IPayload.Containment.ATTACHMENT == data.getContainment())
                href = "cid:" + href;
            else if (IPayload.Containment.EXTERNAL != data.getContainment())
                href = "#" + href;

            piElement.addAttribute(HREF_ATTR, href, null);
        }

        // Create the Schema element (if schema reference is provided)
        final ISchemaReference schemaRef = data.getSchemaReference();
        if (schemaRef != null)
            SchemaElement.createElement(piElement, schemaRef);
        // Create the Description element (if a description is provided)
        final IDescription descr = data.getDescription();
        if (descr != null && !Utils.isNullOrEmpty(descr.getText()))
            DescriptionElement.createElement(piElement, descr);
        // Create the PartProperties element (if there are message properties)
        final Collection<IProperty> partProps = data.getProperties();
        if (!Utils.isNullOrEmpty(partProps))
            PartPropertiesElement.createElement(piElement, partProps);

        return piElement;
    }

    /**
     * Gets an {@link Iterator} for all <code>PartInfo</code> children of the given parent <code>PayloadInfo</code>
     * element.
     *
     * @param   plElement   The parent <code>PayloadInfo</code> element
     * @return              An {@link Iterator} for all {@link OMElement}s representing a <code>PartInfo</code>
     *                      child element of given element, or<br>
     *                      <code>null</code> when no such element is not found as child of the given element.
     */
    public static Iterator<OMElement> getElements(final OMElement plElement) {
        return plElement.getChildrenWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the meta data about one payload from the <code>PartInfo</code> element and returns it as a
     * {@link org.holodeckb2b.common.messagemodel.Payload} object.
     *
     * @param   piElement   The <code>PartInfo</code> element to read the payload meta-data from
     * @return              A new {@link org.holodeckb2b.common.messagemodel.Payload} object containing the
     *                      meta-data about the payload
     */
    public static org.holodeckb2b.common.messagemodel.Payload readElement(final OMElement piElement) {
        if (piElement == null)
            return null;

        // Create the entity object
        final org.holodeckb2b.common.messagemodel.Payload plData = new org.holodeckb2b.common.messagemodel.Payload();

        // Read href attribute
        String href = piElement.getAttributeValue(new QName(HREF_ATTR));
        if (Utils.isNullOrEmpty(href))
            plData.setContainment(IPayload.Containment.BODY);
        else {
            if (href.startsWith("#")) {
                plData.setContainment(IPayload.Containment.BODY);
                href = href.substring(1);
            } else if (href.startsWith("cid:")) {
                plData.setContainment(IPayload.Containment.ATTACHMENT);
                href = href.substring(4);
            } else
                plData.setContainment(IPayload.Containment.EXTERNAL);

            plData.setPayloadURI(href);
        }

        // Read and process Schema element (optional)
        OMElement schema = SchemaElement.getElement(piElement);
        if(schema != null)
            plData.setSchemaReference(SchemaElement.readElement(schema));
        // Read and process Description element (optional)
        OMElement description = DescriptionElement.getElement(piElement);
        if(description != null)
            plData.setDescription(DescriptionElement.readElement(description));
        // Read and process the PartProperties element (optional)
        OMElement partProperties = PartPropertiesElement.getElement(piElement);
        if(partProperties != null)
            plData.setProperties(PartPropertiesElement.readElement(partProperties));

        return plData;
    }
}
