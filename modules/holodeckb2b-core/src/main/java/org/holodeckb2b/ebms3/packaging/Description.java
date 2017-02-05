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

import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IDescription;

/**
 * Is a helper class for handling the ebMS Description element in the ebMS SOAP header.
 * <p>NOTE: This element is currently only specified in section 6.2.8 of the ebMS 3 Core specification as
 * a child of the <code>Error</code> element in a Signal message unit. The schema of the Core spec
 * however defines this element also as child of the PartInfo element, part of a user message
 * message unit. So this element can be reused there as well, although it is not described in section
 * 5.2.2.13 of the spec.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Description {

    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Description");

    // The local name of the lang attribute
    private static final String LN_ATTR_LANG = "lang";

    // The lang attribute is defined in the standard XML namespace
    private static final String NS_PREFIX_ATTR_LANG = "xml";
    private static final String NS_URI_ATTR_LANG = "http://www.w3.org/XML/1998/namespace";

    /**
     * Creates a <code>Description</code> element and adds it to the given parent element.
     * <p>NOTE: This method should only be called when there is text to add to the description.
     * When calling this method when no text is available the result will be an empty and
     * useless Description element!
     *
     * @param parent     The element this element should be added to as a child
     * @param data       The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement parent, final IDescription data) {
        final OMFactory f = parent.getOMFactory();

        // Create the element
        final OMElement description = f.createOMElement(Q_ELEMENT_NAME, parent);

        // Fill it based on the given data
        final String content = data.getText();
        if (content != null && !content.isEmpty())
            description.setText(content);

        // Set attributes if data is specified for it
        final String lang = data.getLanguage();
        if ( lang != null && !lang.isEmpty())
            description.addAttribute(LN_ATTR_LANG, lang, f.createOMNamespace(NS_URI_ATTR_LANG, NS_PREFIX_ATTR_LANG));

        return description;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>Description</code>
     * child element of the given element.
     *
     * @param parent     The parent element
     * @return           The {@link OMElement} object representing the requested element
     *                   or <code>null</code> when the requested element is not found as
     *                   child of the given element.
     */
    public static OMElement getElement(final OMElement parent) {
        return parent.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the information from a <code>eb:Description</code> element and returns it in a
     * {@see org.holodeckb2b.ebms3.persistent.general.Description} object.
     * <p><b>NOTE</b> Although the returned object is an entity object it is not stored in the database by this method.
     *
     * @param descrElement  The element that contains the description to read
     * @return              A {@link org.holodeckb2b.ebms3.persistent.general.Description} object containing the
     *                      information from the specified <code>Description</code> element
     */
    public static org.holodeckb2b.ebms3.persistency.entities.Description readElement(final OMElement descrElement) {
        // Check if there was a Description element to read
        if (descrElement == null)
            return null;

        // Create new entity object
        final org.holodeckb2b.ebms3.persistency.entities.Description  descrData =
                                                        new org.holodeckb2b.ebms3.persistency.entities.Description();

        // Read description content
        descrData.setText(descrElement.getText());
        // Read language attribute
        descrData.setLanguage(descrElement.getAttributeValue(new QName(NS_URI_ATTR_LANG, LN_ATTR_LANG)));

        return descrData;
    }
}
