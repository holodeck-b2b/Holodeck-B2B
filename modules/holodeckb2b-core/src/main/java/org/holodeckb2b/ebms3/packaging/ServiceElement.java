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
import org.holodeckb2b.interfaces.general.IService;

/**
 * Is a helper class for handling the <code>Service</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.8 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ServiceElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Service");

    // The local name of the type attribute
    private static final String LN_ATTR_TYPE = "type";

    /**
     * Creates a <code>Service</code> element and adds it to the given <code>CollaborationInfo</code> element.
     *
     * @param ciElement     The <code>CollaborationInfo</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement ciElement, final IService data) {
        final OMFactory f = ciElement.getOMFactory();

        // Create the element
        final OMElement service = f.createOMElement(Q_ELEMENT_NAME, ciElement);

        // Fill it based on the given data
        service.setText(data.getName());

        // Set attributes if data is specified for it
        final String type = data.getType();
        if ( type != null && !type.isEmpty())
            service.addAttribute(LN_ATTR_TYPE, type, null);

        return service;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>Service</code> child element of the <code>
     * CollaborationInfo</code> element.
     *
     * @param ciElement     The parent <code>CollaborationInfo</code> element
     * @return              The {@link OMElement} object representing the <code>Service</code> element or,
     *                      <code>null</code> when there is <code>Service</code> element found as child of the given
     *                      element.
     */
    public static OMElement getElement(final OMElement ciElement) {
        return ciElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the information from the <code>Service</code> object and returns it in a new {@link
     * org.holodeckb2b.common.messagemodel.Service} entity object.
     *
     * @param svcElement    The <code>Service</code> element to read the info from
     * @return              A new {@link org.holodeckb2b.common.messagemodel.Service} object containing the service info
     *                      from the element
     */
    public static org.holodeckb2b.common.messagemodel.Service readElement(final OMElement svcElement) {
        if (svcElement == null)
            return null;

        // Read service name
        final String svcName = svcElement.getText();
        // Read the optional service type
        final String svcType = svcElement.getAttributeValue(new QName(LN_ATTR_TYPE));

        // Create and return the entity object
        return new org.holodeckb2b.common.messagemodel.Service(svcName, svcType);
    }
}
