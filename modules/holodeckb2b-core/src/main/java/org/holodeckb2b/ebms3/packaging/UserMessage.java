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

import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.messagemodel.IUserMessage;

/**
 * Is a helper class for handling the ebMS UserMessage element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class UserMessage {
    
    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(Constants.EBMS3_NS_URI, "UserMessage", Constants.EBMS3_NS_PREFIX);
    
    /**
     * The local name of the mpc attribute
     */
    private static final String MPC_ATTR = "mpc";
    
    /**
     * Creates a <code>UserMessage</code> element and adds it to the given <code>Messaging</code> element. 
     * 
     * @param messaging     The <code>Messaging</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(OMElement messaging, IUserMessage data) {
        OMFactory f = messaging.getOMFactory();
        
        // Create the element
        OMElement usermessage = f.createOMElement(Q_ELEMENT_NAME, messaging);
        
        // Fill it based on the given data
        
        // MPC attribute only set when not default
        String mpc = data.getMPC();
        if (mpc != null && !mpc.equals(Constants.DEFAULT_MPC))
            usermessage.addAttribute(MPC_ATTR, mpc, null);
        
        // Create the MessageInfo element
        MessageInfo.createElement(usermessage, data);
        // Create the PartyInfo element
        PartyInfo.createElement(usermessage, data);
        // Create the CollaborationInfo element
        CollaborationInfo.createElement(usermessage, data.getCollaborationInfo());
        // Create the MessageProperties element (if there are message properties)
        Collection<IProperty> msgProps = data.getMessageProperties();
        if (msgProps != null && msgProps.size() > 0)
            MessageProperties.createElement(usermessage, msgProps);
        
        // Create the eb:PayloadInfo element (if there are payloads)
        PayloadInfo.createElement(usermessage, data.getPayloads());
        
        return usermessage;
    }
    
    /**
     * Gets an {@link Iterator} for the <code>eb:UserMessage</code> elements 
     * from the given ebMS 3 Messaging header in the SOAP message.
     * 
     * @param messaging   The SOAP Header block that contains the ebMS header,
     *                    i.e. the <code>eb:Messaging</code> element
     * @return      An {@link Iterator} for all {@link OMElement}s representing a 
     *              <code>eb:UserMessage</code> element in the given header
     */
    public static Iterator<?> getElements(OMElement messaging) {
        return messaging.getChildrenWithName(Q_ELEMENT_NAME);
    }
    
    /**
     * Reads the meta data of a User Message message unit from the <code>eb:UserMessage</code> 
     * element and return it as a {@link org.holodeckb2b.ebms3.persistent.message.UserMessage}
     * entity object.
     * <p><b>NOTE 1:</b> The entity object is not persisted by this method! It 
     * is the responsibility of the caller to store it.
     * <p><b>NOTE 2:</b> 
     * 
     * @param   umElement           The <code>UserMessage</code> element that contains
     *                              the meta data to read
     * @return                      A new {@link org.holodeckb2b.ebms3.persistent.message.UserMessage} 
     *                              object 
     * @throws PackagingException   When the given element is not a valid
     *                              <code>UserMessage</code> element.
     */
    public static org.holodeckb2b.ebms3.persistent.message.UserMessage readElement(OMElement umElement) throws PackagingException {
        // Create a new PullRequest entity object to store the information in
        org.holodeckb2b.ebms3.persistent.message.UserMessage umData = new org.holodeckb2b.ebms3.persistent.message.UserMessage();
        
        // The PullRequest itself only contains the [optional] mpc attribute
        String  mpc = umElement.getAttributeValue(new QName(MPC_ATTR));
        
        // If there was no mpc attribute or it was empty (which formally is 
        // illegal because the mpc should be a valid URI) it is set to the default MPC
        if (mpc == null || mpc.isEmpty())
            mpc = Constants.DEFAULT_MPC;
        umData.setMPC(mpc);

        // Get the MessageInfo element
        OMElement child = MessageInfo.getElement(umElement);
        if (child == null)
            // The MessageInfo element is required, so this is an invalid message. Raise exception
            throw new PackagingException("Missing MessageInfo element in UserMessage");
        // Read the MessageInfo element
        MessageInfo.readElement(child, umData);
        
        // Get the PartyInfo element
        child = PartyInfo.getElement(umElement);
        if (child == null)
            // The PartyInfo element is required, so this is an invalid message. Raise exception
            throw new PackagingException("Missing PartyInfo element in UserMessage");
        // Read the PartyInfo element
        PartyInfo.readElement(child, umData);
        
        // Get the CollaborationInfo element
        child = CollaborationInfo.getElement(umElement);
        if (child == null)
            // The CollaborationInfo element is required, so this is an invalid message. Raise exception
            throw new PackagingException("Missing CollaborationInfo element in UserMessage");
        // Read the CollaborationInfo element
        umData.setCollaborationInfo(CollaborationInfo.readElement(child));
        
        // Get the MessageProperties element and process it when available
        child = MessageProperties.getElement(umElement);
        if (child != null)
            umData.setMessageProperties(MessageProperties.readElement(child));
        
        // Get the PayloadInfo element and process it when available
        child = PayloadInfo.getElement(umElement);
        if (child != null)
            umData.setPayloads(PayloadInfo.readElement(child));
        
        return umData;
    }
    
}
