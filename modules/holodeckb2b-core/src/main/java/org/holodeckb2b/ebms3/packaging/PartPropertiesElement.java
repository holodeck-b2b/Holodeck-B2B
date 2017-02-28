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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Is a helper class for handling the <code>PartProperties</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.13 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartPropertiesElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PartProperties");

    /**
     * Creates a <code>PartProperties</code> element and adds it to the given <code>PartInfo</code> element.
     * <p>NOTE: This method should only be used when there are part properties available for the processed payload as
     * the XML schema definition requires the element to have at least one property (although the spec states there can
     * be none).
     *
     * @param piElement     The <code>PartInfo</code> element this element should be added to
     * @param properties    The collection of part properties to include in the element
     * @return              The new element if there where part properties available, or<br>
     *                      <code>null</code> when no properties were available
     */
    public static OMElement createElement(final OMElement piElement, final Collection<IProperty> properties) {
        // Check for availability of properties before doing any processing
        if (Utils.isNullOrEmpty(properties))
            return null;

        final OMFactory f = piElement.getOMFactory();

        // Create the element
        final OMElement partProps = f.createOMElement(Q_ELEMENT_NAME, piElement);

        // Fill it based on the given data
        for(final IProperty p : properties)
            PropertyElement.createElement(partProps, p);

        return partProps;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>PartProperties</code> child element of the
     * <code>PartInfo</code> element.
     *
     * @param   piElement   The parent <code>PartInfo</code> element
     * @return              The {@link OMElement} object representing the <code>PartProperties</code> element or,<br>
     *                      <code>null</code> when the requested element is not found as child of the given element.
     */
    public static OMElement getElement(final OMElement piElement) {
        return piElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the set of properties from the <code>PartProperties</code> element and returns them as a collection of
     * {@link org.holodeckb2b.common.messagemodel.Property} objects.
     *
     * @param   ppElement   The <code>PartProperties</code> element to read the properties from
     * @return              A new collection of objects implementing {@link IProperty}
     */
    public static Collection<IProperty> readElement(final OMElement ppElement) {
        if (ppElement == null)
            return null;
        
        // Create new collection
        final ArrayList<IProperty> props = new ArrayList<>();
        // Get all child elements containing the properties
        final Iterator<OMElement> it = PropertyElement.getElements(ppElement);
        // Read each property element and add it info to the collection
        while (it.hasNext())
            props.add(PropertyElement.readElement((OMElement) it.next()));

        return props;
    }

}
