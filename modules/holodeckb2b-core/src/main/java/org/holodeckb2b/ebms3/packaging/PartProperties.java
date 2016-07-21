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
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Is a helper class for handling the ebMS PartProperties element in the ebMS SOAP
 * header.
 * <p>This element is specified in section 5.2.2.13 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartProperties {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PartProperties");

    /**
     * Creates a <code>PartProperties</code> element and adds it to the given <code>PartInfo</code> element.
     * <p>NOTE: This method should only be used when there are part properties available for the processed payload.
     *
     * @param piElement     The <code>PartInfo</code> element this element should be added to
     * @param properties    The message properties to include in the element
     * @return  The new element if there where message properties available, null when no properties were available
     */
    public static OMElement createElement(final OMElement piElement, final Collection<IProperty> properties) {
        // Check for availability of properties before doing any processing
        if (properties == null || properties.size() == 0)
            return null;

        final OMFactory f = piElement.getOMFactory();

        // Create the element
        final OMElement msgProps = f.createOMElement(Q_ELEMENT_NAME, piElement);

        // Fill it based on the given data
        for(final IProperty p : properties)
            Property.createElement(msgProps, p);

        return msgProps;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>PartProperties</code>
     * child element of the <code>PartInfo</code> element.
     *
     * @param piElement     The parent <code>PartInfo</code> element
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(final OMElement piElement) {
        return piElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the set of properties from the <code>PartProperties</code> element
     * and returns them as a collection of {@link org.holodeckb2b.ebms3.persistency.entities.Property}
     * entity objects.
     * <p><b>NOTE:</b> The entity objects in the collection are not persisted by
     * this method! It is the responsibility of the caller to store it.
     *
     * @param ppElement             The <code>PartProperties</code> element to read the
     *                              properties from
     * @return                      A new collection of {@link org.holodeckb2b.ebms3.persistency.entities.Property}
     *                              objects
     * @throws PackagingException   When the given element is not a valid
     *                              <code>MessageProperties</code> element.
     */
    public static Collection<org.holodeckb2b.ebms3.persistency.entities.Property> readElement(final OMElement ppElement) throws PackagingException {
        if (ppElement == null)
            return null;

        // Create new collection
        final ArrayList<org.holodeckb2b.ebms3.persistency.entities.Property> props = new ArrayList<>();
        // Get all child elements containing the properties
        final Iterator<?> it = ppElement.getChildrenWithName(Property.Q_ELEMENT_NAME);
        // Read each property element and add it info to the collection
        while (it.hasNext())
            props.add(Property.readElement((OMElement) it.next()));

        // The collection must contain at least 1 property
        if (props.isEmpty())
            throw new PackagingException("PartProperties does not contain properties");

        return props;
    }

}
