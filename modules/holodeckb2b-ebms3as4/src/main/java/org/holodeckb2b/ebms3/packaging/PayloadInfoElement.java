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
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is a helper class for handling the ebMS <code>PayloadInfo</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.12 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PayloadInfoElement {

    /**
     * The fully qualified name of the element as an {@see QName}
     */
    public static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");

    /**
     * Creates a <code>PayloadInfo</code> element and adds it to the given <code>UserMessage</code> element.
     * <p>NOTE: This method should only be used when there are payloads associated with the processed message (which
     * is to be expected in the case of a user message).
     *
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param payloads      The information on the payloads of the user message to include in the element
     * @return  The new element if there is payload data available, null when no payload data is available
     */
    public static OMElement createElement(final OMElement umElement, final Collection<? extends IPayload> payloads) {
        // Check for availability of payloads before doing any processing
        if (Utils.isNullOrEmpty(payloads))
            return null;

        // Create the element
        final OMFactory f = umElement.getOMFactory();
        final OMElement plInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);

        // Fill it based on the given data
        for(final IPayload p : payloads)
            PartInfoElement.createElement(plInfo, p);

        return plInfo;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>PayloadInfo</code>  child element of the <code>
     * UserMessage</code> element.
     *
     * @param  umElement    The parent <code>UserMessage</code> element
     * @return              The {@link OMElement} object representing the <code>PayloadInfo</code> element or,<br>
     *                      <code>null</code> when the requested element is not found as child of the given element.
     */
    public static OMElement getElement(final OMElement umElement) {
        return umElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the meta data on the payloads from the <code>PartInfo</code> child elements and returns it as a collection
     * of {@link Payload} objects.
     * <p>Note that the XML schema of the ebMS header does not allow for an empty <code>PayloadInfo</code> element but
     * the specifications text does. This method is able to handle such an empty element, which will result in an
     * empty collection of payload meta-data.
     *
     * @param   piElement   The <code>PayloadInfo</code> element that contains the <code>PartInfo</code> elements to
     *                      read the meta-data from
     * @return              A collection of {@link Payload} objects containing the meta-data on the payloads
     */
    public static Collection<IPayload> readElement(final OMElement piElement){
        if (piElement == null)
            return null;

        // Create new collection
        final ArrayList<IPayload> payloads = new ArrayList<>();
        // Get all child elements containing the properties
        final Iterator<?> it = PartInfoElement.getElements(piElement);
        // Read each property element and add it info to the collection
        while (it.hasNext())
            payloads.add(PartInfoElement.readElement((OMElement) it.next()));

        return payloads;
    }
}
