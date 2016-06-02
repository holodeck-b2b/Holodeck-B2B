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

import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.ebms3.persistency.entities.PartyId;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is a helper class for handling the ebMS PartyInfo element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.2 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartyInfo {
    
    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PartyInfo");
    
    /**
     * The <code>From</code> and <code>To</code> element are structurally equal,
     * both represent a trading partner involved in the exchange. 
     * <br>We therefore use an inner class that represents a general trading partner.
     */
    private static class TradingPartner {
    
        public static enum ElementName { FROM, TO }
    
        /**
         * The fully qualified name of the From element as an {@link QName}
         */
        private static final QName  Q_FROM_PARTY = new QName(EbMSConstants.EBMS3_NS_URI, "From");

        /**
         * The fully qualified name of the To element as an {@link QName}
         */
        private static final QName  Q_TO_PARTY = new QName(EbMSConstants.EBMS3_NS_URI, "To"); 
    
        /**
         * The fully qualified name of the PartyId element as an {@link QName}
         */
        private static final QName  Q_PARTYID = new QName(EbMSConstants.EBMS3_NS_URI, "PartyId");
        
        // The local name for the PartyId type attribute
        private static final String LN_PARTYID_TYPE = "type";
        
        /**
         * The fully qualified name of the Role element as an {@link QName}
         */
        private static final QName  Q_ROLE = new QName(EbMSConstants.EBMS3_NS_URI, "Role");
        
        /**
         * Creates a <code>From</code> or <code>To</code> element and includes it in the
         * given <code>PartyInfo</code> element. 
         * 
         * @param rootName      The name to use for the element, i.e. <i>From</i> or <i>To</i>
         * @param piElement     The <code>PartyInfo</code> element this element should be added to
         * @param data          The data to include in the element
         * @return  The new element
         */
        public static OMElement createElement(ElementName rootName, OMElement piElement, ITradingPartner data) {
            OMFactory f = piElement.getOMFactory();
            
            // Create the element
            OMElement tpInfo = f.createOMElement((rootName == ElementName.FROM ? Q_FROM_PARTY : Q_TO_PARTY), piElement);

            // Add content, starting with all party ids
            for(IPartyId pi : data.getPartyIds()) {
                OMElement partyId = f.createOMElement(Q_PARTYID, tpInfo);
                partyId.setText(pi.getId());
                String pidType = pi.getType();
                if (pidType != null && !pidType.isEmpty())
                    partyId.addAttribute(LN_PARTYID_TYPE, pidType, null);
            }
            
            // Create the Role element and ensure it has a value. 
            OMElement roleElem = f.createOMElement(Q_ROLE, tpInfo);
            String role = data.getRole();
            roleElem.setText((role != null && !role.isEmpty() ? role : EbMSConstants.DEFAULT_ROLE ));
            
            return tpInfo;
        }
        
        /**
         * Gets the {@link OMElement} object that represent the <code>To</code> or
         * <code>From</code> child element of the <code>eb:PartyInfo</code> element.
         * 
         * @param piElement     The parent <code>eb:PartyInfo</code> element.
         * @param elemName      Indicates whether the <code>To</code> or <code>From</code>
         *                      element should be retrieved
         * @return              The {@link OMElement} object representing the requested element
         *                      or <code>null</code> when the requested element is not found as
         *                      child of the given element.
         */
        public static OMElement getElement(OMElement piElement, ElementName elemName) {
            return piElement.getFirstChildWithName((elemName == ElementName.FROM ? Q_FROM_PARTY : Q_TO_PARTY));
        }
        
        
        /**
         * Reads the trading partner information from a <code>From</code> or 
         * <code>To</code> element and returns it as a {@link org.holodeckb2b.ebms3.persistency.entities.TradingPartner}
         * object.
         * <p><b>NOTE:</b> Although the information is returned in an entity object,
         * the object is not persisted. 
         * 
         * @param tpElement     The element to read the information from
         * @return              A {@link org.holodeckb2b.ebms3.persistency.entities.TradingPartner}
         *                      object containing the information from the element
         * @throws PackagingException   When the given element does not conform to
         *                              ebMS specification and can therefor not be
         *                              read completely
         */
        public static org.holodeckb2b.ebms3.persistency.entities.TradingPartner readElement(OMElement tpElement) throws PackagingException {
            if (tpElement == null)
                return null; // If there is no element content, there is no data
            
            // Create the entity object
            org.holodeckb2b.ebms3.persistency.entities.TradingPartner tpData = new org.holodeckb2b.ebms3.persistency.entities.TradingPartner();
            
            // Check for a Role element and use its value
            OMElement roleElement = tpElement.getFirstChildWithName(Q_ROLE);
            if (roleElement != null) {
                String role = roleElement.getText();
                tpData.setRole((role != null && !role.isEmpty() ? role : EbMSConstants.DEFAULT_ROLE ));
            } else
                tpData.setRole(EbMSConstants.DEFAULT_ROLE);
            
            // Read all PartyId elements and add info to entity
            Iterator<?> it = tpElement.getChildrenWithName(Q_PARTYID);
            while (it.hasNext()) {
                OMElement pidElem = (OMElement) it.next();
                String pid = pidElem.getText();
                String pidType = pidElem.getAttributeValue(new QName(LN_PARTYID_TYPE));
                // The PartyId must have a value
                if (pid == null || pid.isEmpty())
                    throw new PackagingException("PartyId is required but found empty");
                
                PartyId pidData = new PartyId();
                pidData.setId(pid); 
                
                if (pidType != null && !pidType.isEmpty())
                    pidData.setType(pidType);
                
                // Add PartyId to trading partner info
                tpData.addPartyId(pidData);
            }
            // There must be at least one PartyId for a trading partner
            if (tpData.getPartyIds().isEmpty())
                throw new PackagingException("No PartyId found for tradingpartner");
            
            return tpData;
        }
     }

    /**
     * Creates an ebMS 3 <code>PartyInfo</code> element and adds it to <code>UserMessage</code> element.
     * The created element includes the <code>From</code> and <code>To</code> elements that identify the 
     * sender and receiver of this message unit.
     * 
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param data          The data to include in the element
     * @return The new element
     */
    public static OMElement createElement(OMElement umElement, IUserMessage data) {
        OMFactory f = umElement.getOMFactory();
        
        // Create the element
        OMElement partyInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);
        
        // Add content, i.e. the from and to element
        TradingPartner.createElement(TradingPartner.ElementName.FROM, partyInfo, data.getSender());
        TradingPartner.createElement(TradingPartner.ElementName.TO, partyInfo, data.getReceiver());
        
        return partyInfo;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>PartyInfo</code> 
     * child element of the <code>UserMessage</code> element.
     * 
     * @param muElement     The parent <code>UserMessage</code> element
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(OMElement muElement) {
        return muElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }    
    
    /**
     * Reads the information on the sender and receiver of the UserMessage message
     * unit and stores it in the given {@link org.holodeckb2b.ebms3.persistency.entities.UserMessage} 
     * object.
     * <p><b>NOTE:</b> This method does NOT persist the entity object! It is the
     * responsibility of the caller to save changes.
     * 
     * @param piElement             The <code>PartyInfo</code> element that contains the
     *                              info about the sender and receiver of this User Message message unit
     * @param umData                The {@link org.holodeckb2b.ebms3.persistency.entities.UserMessage} object
     *                              to update
     * @throws PackagingException   When the given element does not contain a valid
     *                              <code>eb:PartyInfo</code> element.
     */
    public static void readElement(OMElement piElement, org.holodeckb2b.ebms3.persistency.entities.UserMessage umData) throws PackagingException {
        if (piElement == null)
            return;
        
        // Read the content, i.e. the from and to element
        org.holodeckb2b.ebms3.persistency.entities.TradingPartner tp = TradingPartner.readElement(TradingPartner.getElement(piElement, TradingPartner.ElementName.FROM));
        if (tp != null)
            umData.setSender(tp);
        else 
            // The From element must occur as child of PartyInfo
            throw new PackagingException("No From element found in PartyInfo element");
        
        tp = TradingPartner.readElement(TradingPartner.getElement(piElement, TradingPartner.ElementName.TO));
        if (tp != null)
            umData.setReceiver(tp);
        else
            // The To element must occur as child of PartyInfo
            throw new PackagingException("No To element found in PartyInfo element");        
    }
}
