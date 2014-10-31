/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;

/**
 * Is a helper class for handling the ebMS CollaborationInfo element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.6 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CollaborationInfo {
    
    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(Constants.EBMS3_NS_URI, "CollaborationInfo");

    /**
     * The fully qualified name of the Action element as an {@link QName}
     */
    private static final QName  Q_ACTION = new QName(Constants.EBMS3_NS_URI, "Action");

    /**
     * The fully qualified name of the element ConversationId as an {@link QName}
     */
    private static final QName  Q_CONVERSATIONID = new QName(Constants.EBMS3_NS_URI, "ConversationId");

    /**
     * Creates a <code>CollaborationInfo</code> element and adds it to the given <code>UserMessage</code> element. 
     * 
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(OMElement umElement, ICollaborationInfo data) {
        OMFactory f = umElement.getOMFactory();
        
        // Create the element
        OMElement collabInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);
        
        // Fill it based on the given data
        if (data.getAgreement() != null) 
            AgreementRef.createElement(collabInfo, data.getAgreement());
        
        Service.createElement(collabInfo, data.getService());
        
        // Elements Action and ConversationId are so simple that they are created here
        OMElement action = f.createOMElement(Q_ACTION, collabInfo);
        action.setText(data.getAction());
        
        OMElement convId = f.createOMElement(Q_CONVERSATIONID, collabInfo);
        convId.setText(data.getConversationId());
        
        return collabInfo;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>CollaborationInfo</code> 
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
     * Reads the business transaction information of the UserMessage message unit 
     * contained in the <code>CollaborationInfo</code> element and returns it in 
     * a new {@link org.holodeckb2b.ebms3.persistent.message.CollaborationInfo} 
     * object.
     * <p><b>NOTE:</b> This method does NOT persist the entity object! It is the
     * responsibility of the caller to save it.
     * 
     * @param ciElement             The <code>CollaborationInfo</code> element that contains the
     *                              info about this User Message message unit
     * @return                      The {@link org.holodeckb2b.ebms3.persistent.message.CollaborationInfo} object
     *                              the information is returned in
     * @throws PackagingException   When the given element does not contain a valid
     *                              <code>CollaborationInfo</code> element.
     */
    public static org.holodeckb2b.ebms3.persistent.message.CollaborationInfo readElement(OMElement ciElement) throws PackagingException {
        // There must be a CollaborationInfo element 
        if (ciElement == null)
            return null;
        
        // Create new entity object
        org.holodeckb2b.ebms3.persistent.message.CollaborationInfo ciData = new org.holodeckb2b.ebms3.persistent.message.CollaborationInfo();
        
        // Start with reading the required elements: Service, Action and ConversationId
        OMElement child = Service.getElement(ciElement);
        if (child == null)
            // Service element is required, so raise exception about invalid message
            throw new PackagingException("Service element is missing from CollaborationInfo");
        
        // Read the Service element and store info in entity object
        ciData.setService(Service.readElement(child));
        
        // Action child element
        child = ciElement.getFirstChildWithName(Q_ACTION);
        if (child == null)
            throw new PackagingException("Action element is missing from CollaborationInfo");
        
        String action = child.getText();
        if (action == null && action.isEmpty())
            throw new PackagingException("Action element is empty");
        
        ciData.setAction(action);
        
        // ConversationId child element
        child = ciElement.getFirstChildWithName(Q_CONVERSATIONID);
        if (child == null)
            throw new PackagingException("ConversationId element is missing from CollaborationInfo");
        
        String convId = child.getText();
        if (convId == null && convId.isEmpty())
            throw new PackagingException("ConversationId element is empty");
        
        ciData.setConversationId(convId);
        
        // Get and read optional AgreementRef child element
        child = AgreementRef.getElement(ciElement);
        if (child != null)
            ciData.setAgreement(AgreementRef.readElement(child));
        
        return ciData;
    }
}
