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

import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.messagemodel.IReceipt;

/**
 * Is a helper class for handling the ebMS Receipt signals in the ebMS SOAP header. 
 * <p>The Receipt signal may contain any kind of element as child elements. For packaging 
 * this means we do not know what the actual XML content will be so it has to be supplied 
 * when constructing the element. Similar when reading the element the XML content is returned.
 * Because this information is not exchanged with the business application it is not included
 * in the {@link IReceipt} interface. Therefor the internal {@link org.holodeckb2b.ebms3.persistent.message.Receipt} 
 * entity object is used as parameter for the {@link #createElement(org.apache.axiom.om.OMElement, org.holodeckb2b.ebms3.persistent.message.Receipt)} 
 * method.
 * <p>The Receipt signal message unit is specified in section 5.2.3.3 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Receipt {
    
    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(Constants.EBMS3_NS_URI, "Receipt", Constants.EBMS3_NS_PREFIX);
    
    /**
     * Creates a new <code>eb:SignalMessage</code> for an <i>Receipt</i> signal.
     * 
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param receipt       The information to include in the receipt signal
     * @return              The new element representing the receipt signal
     */
    public static OMElement createElement(OMElement messaging, org.holodeckb2b.ebms3.persistent.message.Receipt receipt) {
        // First create the SignalMessage element that is the placeholder for
        // the Receipt element containing the receipt info
        OMElement signalmessage = SignalMessage.createElement(messaging);
        
        // Create the generic MessageInfo element
        MessageInfo.createElement(signalmessage, receipt);
        
        // Create the Receipt element
        OMElement rcptElement = signalmessage.getOMFactory().createOMElement(Q_ELEMENT_NAME, signalmessage);
        
        // Add the child elements as given in Receipt object
        for(OMElement e : receipt.getContent()) 
            rcptElement.addChild(e);
        
        return signalmessage;        
    }
    
    /**
     * Reads the information from a <code>eb:SignalMessage</code> and its child elements that contain the Receipt signal 
     * message unit and stores it a {@link org.holodeckb2b.ebms3.persistent.message.Receipt} object. 
     * <p><b>NOTE:</b> The information is stored in an entity object, but this method will NOT persist the object.
     * 
     * @param sigElement    The parent <code>eb:SignalMessage</code> element that contains the <code>eb:Receipt</code> element 
     * @return              The {@link org.holodeckb2b.ebms3.persistent.message.Receipt} object containing the information
     *                      on the receipt
     * @throws PackagingException   When the given element does not conform to
     *                              ebMS specification and can therefore not be
     *                              read completely
     */
    public static org.holodeckb2b.ebms3.persistent.message.Receipt readElement(OMElement sigElement) throws PackagingException {
        // Create a new Receipt entity object to store the information in
        org.holodeckb2b.ebms3.persistent.message.Receipt rcptData = new org.holodeckb2b.ebms3.persistent.message.Receipt();
        
        // First read general information from the MessageInfo child 
        MessageInfo.readElement(MessageInfo.getElement(sigElement), rcptData);
        
        // Because the content of the Receipt is not predefined read and store all child elements of the Receipt element
        rcptData.setContent(sigElement.getFirstChildWithName(Q_ELEMENT_NAME).getChildElements());
        
        return rcptData;
    }
    
    /**
     * Gets an {@link Iterator} for all <code>eb:SignalMessage</code> elements 
     * from the given ebMS 3 Messaging header in the SOAP message that represent
     * <i>Receipt</i> signals.
     * 
     * @param messaging   The SOAP Header block that contains the ebMS header,
     *                    i.e. the <code>eb:Messaging</code> element
     * @return      An {@link Iterator} for all {@link OMElement}s representing a 
     *              <code>eb:SignalMessage</code> element that contains an 
     *              Error signal, i.e. has one or more <code>eb:Receipt</code> 
     *              child elements  
     */
    public static Iterator<OMElement> getElements(SOAPHeaderBlock messaging) {
        // Check all SignalMessage elements in the header
        Iterator<?> signals = org.holodeckb2b.ebms3.packaging.SignalMessage.getElements(messaging);
        
        ArrayList<OMElement>  receipts = new ArrayList<OMElement>();
        while(signals.hasNext()) {
            OMElement signal  = (OMElement) signals.next();
            // If this SignalMessage element has a Receipt child, 
            //   it is an Receipt signal and should be returned
            if (signal.getFirstChildWithName(Q_ELEMENT_NAME) != null)
                receipts.add(signal);
        }
        
        return receipts.iterator();
    }       
    
    
}
