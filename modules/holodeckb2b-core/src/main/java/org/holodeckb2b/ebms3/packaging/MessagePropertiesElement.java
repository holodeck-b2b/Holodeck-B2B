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
 * Is a helper class for handling the <code>MessageProperties</code> element in the ebMS SOAP header block.
 * <p>This element is specified in section 5.2.2.11 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MessagePropertiesElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "MessageProperties");

    /**
     * Creates a <code>MessageProperties</code> element and adds it to the given <code>UserMessage</code> element.
     * <p>NOTE: This method should only be used when there are message properties available for the processed User
     * Message as the XML schema definition requires the element to have at least one property (although the spec states
     * there can be none).
     *
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param properties    The message properties to include in the element
     * @return  The new element if there where message properties available, null when no properties were available
     */
    public static OMElement createElement(final OMElement umElement, final Collection<IProperty> properties) {
        // Check for availability of properties before doing any processing
        if (Utils.isNullOrEmpty(properties))
            return null;

        final OMFactory f = umElement.getOMFactory();
        // Create the element
        final OMElement msgProps = f.createOMElement(Q_ELEMENT_NAME, umElement);

        // Fill it based on the given data
        for(final IProperty p : properties)
            PropertyElement.createElement(msgProps, p);

        return msgProps;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>MessageProperties</code> child element of the <code>
     * UserMessage</code> element.
     *
     * @param   umElement   The parent <code>UserMessage</code> element
     * @return              The {@link OMElement} object representing the <code>MessageProperties</code> element or,<br>
     *                      <code>null</code> when the requested element is not found as child of the given element.
     */
    public static OMElement getElement(final OMElement umElement) {
        return umElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the set of properties from the <code>MessageProperties</code> element and returns them as a collection of
     * {@link org.holodeckb2b.common.messagemodel.Property} entity objects.
     *
     * @param   mpElement   The <code>MessageProperties</code> element to read the properties from
     * @return              A new collection of objects implementing {@link IProperty}
     */
    public static Collection<IProperty> readElement(final OMElement mpElement) {
        if (mpElement == null)
            return null;

        // Create new collection
        final ArrayList<IProperty> props = new ArrayList<>();
        // Get all child elements containing the properties
        final Iterator<OMElement> it = PropertyElement.getElements(mpElement);
        // Read each property element and add it info to the collection
        while (it.hasNext())
            props.add(PropertyElement.readElement((OMElement) it.next()));

        return props;
    }
}
