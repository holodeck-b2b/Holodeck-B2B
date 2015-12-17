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
import org.holodeckb2b.ebms3.persistent.message.AgreementReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;

/**
 * Is a helper class for handling the ebMS AgreementRef element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.7 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class AgreementRef {
    
    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");
    
    // The local name of the type attribute
    private static final String LN_ATTR_TYPE = "type";
    
    // The local name of the pmode attribute
    private static final String LN_ATTR_PMODE = "pmode";
    
    /**
     * Creates a <code>AgreementRef</code> element and adds it to the given <code>CollaborationInfo</code> element. 
     * 
     * @param ciElement     The <code>CollaborationInfo</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(OMElement ciElement, IAgreementReference data) {
        OMFactory f = ciElement.getOMFactory();
        
        // Create the element
        OMElement agreementRef = f.createOMElement(Q_ELEMENT_NAME, ciElement);
        
        // Fill it based on the given data
        agreementRef.setText(data.getName());
        
        // Set attributes if data is specified for it
        String type = data.getType();
        if ( type != null && !type.isEmpty())
            agreementRef.addAttribute(LN_ATTR_TYPE, type, null);
        String pmode = data.getPModeId();
        if ( pmode != null && !pmode.isEmpty())
            agreementRef.addAttribute(LN_ATTR_PMODE, pmode, null);
        
        return agreementRef;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>AgreementRef</code> 
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
     * Reads the information from the <code>AgreementRef</code> object and returns it
     * in a new {@link AgreementReference} entity object.
     * <p><b>NOTE:</b> The entity object is not persisted by this method! It is
     * the responsibility of the caller to store it.
     * 
     * @param arElement             The <code>AgreementRef</code> element to read the
     *                               info from
     * @return                       A new {@link org.holodeckb2b.ebms3.persistent.general.AgreementReference} 
     *                               object containing the service info from the
     *                               element
     * @throws PackagingException    When the given element does not contain a valid
     *                               <code>AgreementRef</code> element.
     */
    public static AgreementReference readElement(OMElement arElement) throws PackagingException {
        if (arElement == null)
            return null;
        
        // Read agreement info, i.e. name and type of reference
        String agreement = arElement.getText();
        if (agreement == null || agreement.isEmpty()) 
            // AgreementRed must contain name of agreement
            throw new PackagingException("AgreementRef does not contain agreement reference");
        
        String type = arElement.getAttributeValue(new QName(LN_ATTR_TYPE));
        // Create entity object
        AgreementReference arData = new AgreementReference(agreement, type);
        // Read P-Mode id
        arData.setPModeId(arElement.getAttributeValue(new QName(LN_ATTR_PMODE)));
        
        return arData;
    }
}
