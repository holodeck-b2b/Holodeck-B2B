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
 * Is a helper class for handling the ebMS Service element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.8 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Service {
    
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
    public static OMElement createElement(OMElement ciElement, IService data) {
        OMFactory f = ciElement.getOMFactory();
        
        // Create the element
        OMElement service = f.createOMElement(Q_ELEMENT_NAME, ciElement);
        
        // Fill it based on the given data
        service.setText(data.getName());
        
        // Set attributes if data is specified for it
        String type = data.getType();
        if ( type != null && !type.isEmpty())
            service.addAttribute(LN_ATTR_TYPE, type, null);
        
        return service;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>Service</code> 
     * child element of the <code>CollaborationInfo</code> element.
     * 
     * @param ciElement     The parent <code>CollaborationInfo</code> element 
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(OMElement ciElement) {
        return ciElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }    
    
    /**
     * Reads the information from the <code>Service</code> object and returns it
     * in a new {@link org.holodeckb2b.ebms3.persistency.entities.Service} entity
     * object.
     * <p><b>NOTE:</b> The entity object is not persisted by this method! It is
     * the responsibility of the caller to store it.
     * 
     * @param svcElement             The <code>Service</code> element to read the
     *                               info from
     * @return                       A new {@link org.holodeckb2b.ebms3.persistency.entities.Service} 
     *                               object containing the service info from the
     *                               element
     * @throws PackagingException    When the given element does not contain a valid
     *                               <code>Service</code> element.
     */
    public static org.holodeckb2b.ebms3.persistency.entities.Service readElement(OMElement svcElement) throws PackagingException {
        if (svcElement == null)
            return null;
        
        // Read service name
        String svcName = svcElement.getText();
        
        if (svcName == null || svcName.isEmpty())
            // Service name is required!
            throw new PackagingException("Service name is missing from Service element");
        
        // Create the entity object
        org.holodeckb2b.ebms3.persistency.entities.Service svcData = new org.holodeckb2b.ebms3.persistency.entities.Service(svcName);
        
        // Read the optional service type
        svcData.setType(svcElement.getAttributeValue(new QName(LN_ATTR_TYPE)));
        
        return svcData;
    }
}
