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

import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Is a helper class for handling the <code>Property</code> elements that occur in the ebMS SOAP header. This element is
 * used as a child of both <code>MessageProperties</code> and <code>PartProperties</code> specified in sections 5.2.2.11
 * and 5.2.2.13 respectively of the ebMS 3 Core specification.
 * <p><b>NOTE:</b> The current version of the ebMS spec does define the type attribute, but the schema does not. So
 * adding this to the XML document makes it invalid. This problem is known to the  OASIS ebXML Messaging TC under <a
 * href="https://tools.oasis-open.org/issues/browse/EBXMLMSG-2">issue number 2</a>. As noted there the schema will be
 * changed to include this attribute. Until the issue is fixed Holodeck B2B will not add the type.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Property {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Property");

    // The local name of the name attribute
    private static final String LN_ATTR_NAME = "name";

    // The local name of the type attribute
    private static final String LN_ATTR_TYPE = "type";

    /**
     * Creates a <code>Property</code> element and adds it to the given element.
     *
     * @param parent     The element this element should be added to
     * @param data       The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement parent, final IProperty data) {
        final OMFactory f = parent.getOMFactory();

        // Create the element
        final OMElement property = f.createOMElement(Q_ELEMENT_NAME, parent);

        // Fill it based on the given data
        property.setText(data.getValue());

        // Set attributes if data is specified for it
        property.addAttribute(LN_ATTR_NAME, data.getName(), null); // name attribute is required!

        // @todo: When type attribute problem (spec problem, see above) is finally resolved, this comment can be removed
        //String type = data.getType();
        //if ( type != null && !type.isEmpty())
        //    property.addAttribute(LN_ATTR_TYPE, type, null);

        return property;
    }

    /**
     * Gets an {@link Iterator} for all <code>Property</code> child elements of the given parent element.
     *
     * @param   parent   The parent element, i.e. either a <code>MessageProperties</code> or <code>
     *                      PartProperties</code> element
     * @return              An {@link Iterator} for all {@link OMElement}s representing a <code>Property</code> child
     *                      of the given parent element
     */
    public static Iterator<OMElement> getElements(final OMElement parent) {
        return parent.getChildrenWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the information from the <code>Property</code> object and returns it in a new {@link
     * org.holodeckb2b.common.messagemodel.Property} object.
     *
     * @param   propElement     The <code>Property</code> element to read the info from
     * @return                  A new {@link org.holodeckb2b.ebms3.persistency.entities.Property} object containing the
     *                          service info from the element
     */
    public static org.holodeckb2b.common.messagemodel.Property readElement(final OMElement propElement) {
        if (propElement == null)
            return null;

        // Read property name and value
        final String name = propElement.getAttributeValue(new QName(LN_ATTR_NAME));
        final String value = propElement.getText();

        //@todo: Uncomment when spec is changed and type is allowed
//      // Read type attribute
//        String type = propElement.getAttributeValue(new QName(LN_ATTR_TYPE));

        // Create and return the entity object
        return new org.holodeckb2b.common.messagemodel.Property(name, value);
    }
}
