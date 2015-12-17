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
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is a helper class for handling the ebMS PayloadInfo element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.12 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PayloadInfo {
    
    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");

    /**
     * Creates a <code>PayloadInfo</code> element and adds it to the given <code>UserMessage</code> element. 
     * <p>NOTE: This method should only be used when there are payloads associated with the processed message (which
     * is to be expected in the case of a user message).
     * 
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param payloads      The information on the payloads of the user message to include in the element
     * @return  The new element if there where message properties available, null when no properties were available
     */
    public static OMElement createElement(OMElement umElement, Collection<IPayload> payloads) {
        // Check for availability of payloads before doing any processing
        if (payloads == null || payloads.isEmpty())
            return null;
        
        // Create the element
        OMFactory f = umElement.getOMFactory();
        OMElement plInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);
        
        // Fill it based on the given data
        for(IPayload p : payloads)
            PartInfo.createElement(plInfo, p);
        
        return plInfo;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>PayloadInfo</code> 
     * child element of the <code>UserMessage</code> element.
     * 
     * @param ciElement     The parent <code>UserMessage</code> element 
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(OMElement umElement) {
        return umElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }  
    
    /**
     * Reads the meta data on the payloads from the <code>PartInfo</code> child
     * elements and returns them as a collection of {@link org.holodeckb2b.ebms3.persistent.message.Payload}
     * entity objects.
     * <p><b>NOTE:</b> The entity objects in the collection are not persisted by 
     * this method! It is the responsibility of the caller to store it.
     * 
     * @param piElement             The <code>PayloadInfo</code> element that contains
     *                              the <code>PartInfo</code> element to read the 
     *                              meta data from
     * @return                      A new collection of {@link org.holodeckb2b.ebms3.persistent.message.Payload} 
     *                              objects 
     * @throws PackagingException   When the given element is not a valid
     *                              <code>PayloadInfo</code> element.
     */
    public static Collection<org.holodeckb2b.ebms3.persistent.message.Payload> readElement(OMElement piElement) throws PackagingException {
        if (piElement == null)
            return null;
        
        // Create new collection
        ArrayList<org.holodeckb2b.ebms3.persistent.message.Payload> payloads = new ArrayList<org.holodeckb2b.ebms3.persistent.message.Payload>();
        // Get all child elements containing the properties
        Iterator<?> it = piElement.getChildrenWithName(PartInfo.Q_ELEMENT_NAME);
        // Read each property element and add it info to the collection
        while (it.hasNext())
            payloads.add(PartInfo.readElement((OMElement) it.next()));
        
        // The collection must contain at least 1 payload
        if (payloads.isEmpty())
            throw new PackagingException("PayloadInfo does not contain PartInfo elements");
        
        return payloads;
    }    
}
