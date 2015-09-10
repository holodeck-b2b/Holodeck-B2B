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
import org.holodeckb2b.common.messagemodel.IMessageUnit;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;

/**
 * Is a helper class for handling the ebMS MessageInfo element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.1 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageInfo {
    
    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(Constants.EBMS3_NS_URI, "MessageInfo");
    
    /**
     * The fully qualified name of the Timestamp element as an {@see QName}
     */
    private static final QName  Q_TIMESTAMP = new QName(Constants.EBMS3_NS_URI, "Timestamp");
    
    /**
     * The fully qualified name of the MessageId element as an {@see QName}
     */
    private static final QName  Q_MESSAGEID = new QName(Constants.EBMS3_NS_URI, "MessageId");
    
    /**
     * The fully qualified name of the RefToMessageId element as an {@see QName}
     */
    private static final QName  Q_REFTO_MESSAGEID = new QName(Constants.EBMS3_NS_URI, "RefToMessageId");
    
    /**
     * Creates a <code>eb:MessageInfo</code> element for the given {@see IMessageUnit} 
     * object. This element is added as a child to the element representing the
     * message unit.
     * 
     * @param muElement     The element representing the given message unit
     * @param data          The message unit information as an {@see IMessageUnit}
     * @return              The newly created <code>eb:MessageInfo</code> element.
     */
    public static OMElement createElement(OMElement muElement, IMessageUnit data) {
        OMFactory f = muElement.getOMFactory();
        
        // Create the element
        OMElement msginfo = f.createOMElement(Q_ELEMENT_NAME, muElement);
        
        // Add content
        OMElement timestamp = f.createOMElement(Q_TIMESTAMP, msginfo);
        timestamp.setText(Utils.toXMLDateTime(data.getTimestamp()));
        
        OMElement messageId = f.createOMElement(Q_MESSAGEID, msginfo);
        messageId.setText(data.getMessageId());
        
        String refMsgId = data.getRefToMessageId();
        if (refMsgId != null && !refMsgId.isEmpty()) {
            OMElement  refToMsgId = f.createOMElement(Q_REFTO_MESSAGEID, msginfo);
            refToMsgId.setText(refMsgId);
        }

        return msginfo;
    } 

    /**
     * Gets the {@link OMElement} object that represent the <code>MessageInfo</code> 
     * child element of the <code>UserMessage</code> or <code>SignalMessage</code> 
     * element.
     * 
     * @param piElement     The parent element (either <code>UserMessage</code> or <code>SignalMessage</code>)
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(OMElement muElement) {
        return muElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }
    
    /**
     * Reads the information from the <code>eb:MessageInfo</code> element and
     * stores it in the given {@see MessageUnit} object. 
     * 
     * @param miElement     The <code>MessageInfo</code> element to read the
     *                      information from
     * @param mu            The {@link MessageUnit} where information should be
     *                      stored in
     * @throws PackagingException   When the given element does not conform to
     *                              ebMS specification and can therefore not be
     *                              read completely
     */
    public static void readElement(OMElement miElement, MessageUnit mu) throws PackagingException {
        try {
            mu.setTimestamp(Utils.fromXMLDateTime(miElement.getFirstChildWithName(Q_TIMESTAMP).getText()));
        } catch (Exception e) {
            throw new PackagingException("Required element " + Q_TIMESTAMP.getLocalPart() 
                                                                    + " is missing or has invalid value");
        }  
        
        OMElement messageId = miElement.getFirstChildWithName(Q_MESSAGEID);
            
        if (messageId == null || messageId.getText().isEmpty())
            throw new PackagingException("Required element " + Q_TIMESTAMP.getLocalPart() 
                                                                    + " is missing or has invalid value");
        mu.setMessageId(messageId.getText());
        
        OMElement  refToMsgId = miElement.getFirstChildWithName(Q_REFTO_MESSAGEID);
        if (refToMsgId != null && !refToMsgId.getText().isEmpty())
            mu.setRefToMessageId(refToMsgId.getText());
    }
}
