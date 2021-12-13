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
import org.holodeckb2b.common.messagemodel.PartyId;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is a helper class for handling the ebMS <code>PartyInfo</code> element and its <code>From</code> and <code>To</code>
 * child elements in the ebMS SOAP header.
 * <p>This elements are specified in sections 5.2.2.2 to 5.2.2.5 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PartyInfoElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    public static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PartyInfo");

    /**
     * Creates an ebMS 3 <code>PartyInfo</code> element and adds it to <code>UserMessage</code> element. The created
     * element includes the <code>From</code> and <code>To</code> elements that identify the sender and receiver of this
     * User Message.
     *
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param data          The data to include in the element
     * @return The new element
     */
    public static OMElement createElement(final OMElement umElement, final IUserMessage data) {
        final OMFactory f = umElement.getOMFactory();

        // Create the element
        final OMElement partyInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);

        // Add content, i.e. the from and to element
        TradingPartner.createElement(TradingPartner.ElementName.FROM, partyInfo, data.getSender());
        TradingPartner.createElement(TradingPartner.ElementName.TO, partyInfo, data.getReceiver());

        return partyInfo;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>PartyInfo</code> child element of the given <code>
     * UserMessage</code> element.
     *
     * @param muElement     The parent <code>UserMessage</code> element
     * @return              The {@link OMElement} object representing the <code>PartyInfo</code> element or,<br>
     *                      <code>null</code> when the requested element is not found as child of the given element.
     */
    public static OMElement getElement(final OMElement muElement) {
        return muElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the information on the sender and receiver of the User Message message unit and stores it in the given
     * {@link org.holodeckb2b.common.messagemodel.UserMessage} object.
     * <p><b>NOTE:</b> This method only reads the information that is available from the message. It does not perform
     * any validation of the message, this is done in the validation handlers.
     *
     * @param piElement             The <code>PartyInfo</code> element that contains the info about the sender and
     *                              receiver of this User Message message unit
     * @param umData                The {@link org.holodeckb2b.common.messagemodel.UserMessage} object to update
     */
    public static void readElement(final OMElement piElement,
                                   final org.holodeckb2b.common.messagemodel.UserMessage umData) {
        if (piElement == null)
            return;

        // Read the content, i.e. the From and To elements and store the info in the persistency object
        umData.setSender(TradingPartner.readElement(
                                        TradingPartner.getElement(piElement, TradingPartner.ElementName.FROM)));
        umData.setReceiver(TradingPartner.readElement(
                                        TradingPartner.getElement(piElement, TradingPartner.ElementName.TO)));
    }

    /**
     * The <code>From</code> and <code>To</code> element are structurally equal, both represent a trading partner
     * involved in the exchange. We therefore use an inner class that represents a general trading partner.
     */
    public static class TradingPartner {

        public static enum ElementName { FROM, TO }

        /**
         * The fully qualified name of the From element as an {@link QName}
         */
        protected static final QName  Q_FROM_PARTY = new QName(EbMSConstants.EBMS3_NS_URI, "From");

        /**
         * The fully qualified name of the To element as an {@link QName}
         */
        protected static final QName  Q_TO_PARTY = new QName(EbMSConstants.EBMS3_NS_URI, "To");

        /**
         * The fully qualified name of the PartyId element as an {@link QName}
         */
        protected static final QName  Q_PARTYID = new QName(EbMSConstants.EBMS3_NS_URI, "PartyId");

        // The local name for the PartyId type attribute
        public static final String LN_PARTYID_TYPE = "type";

        /**
         * The fully qualified name of the Role element as an {@link QName}
         */
        protected static final QName  Q_ROLE = new QName(EbMSConstants.EBMS3_NS_URI, "Role");

        /**
         * Creates a <code>From</code> or <code>To</code> element and includes it in the given <code>PartyInfo</code>
         * element.
         *
         * @param rootName      The name to use for the element, i.e. <i>From</i> or <i>To</i>
         * @param piElement     The <code>PartyInfo</code> element this element should be added to
         * @param data          The data to include in the element
         * @return  The new element
         */
        public static OMElement createElement(final ElementName rootName, final OMElement piElement,
                                              final ITradingPartner data) {
            final OMFactory f = piElement.getOMFactory();

            // Create the element
            final OMElement tpInfo = f.createOMElement(rootName == ElementName.FROM ? Q_FROM_PARTY : Q_TO_PARTY,
                                                       piElement);

            // Add content, starting with all party ids
            for(final IPartyId pi : data.getPartyIds()) {
                final OMElement partyId = f.createOMElement(Q_PARTYID, tpInfo);
                partyId.setText(pi.getId());
                final String pidType = pi.getType();
                if (pidType != null && !pidType.isEmpty())
                    partyId.addAttribute(LN_PARTYID_TYPE, pidType, null);
            }

            // Create the Role element and ensure it has a value.
            final OMElement roleElem = f.createOMElement(Q_ROLE, tpInfo);
            final String role = data.getRole();
            roleElem.setText((role != null && !role.isEmpty() ? role : EbMSConstants.DEFAULT_ROLE ));

            return tpInfo;
        }

        /**
         * Gets the {@link OMElement} object that represent the <code>To</code> or <code>From</code> child element of
         * the <code>eb:PartyInfo</code> element.
         *
         * @param piElement     The parent <code>eb:PartyInfo</code> element.
         * @param elemName      Indicates whether the <code>To</code> or <code>From</code> element should be retrieved
         * @return              The {@link OMElement} object representing the requested element or,<br>
         *                      <code>null</code> when the requested element is not found as child of the given element.
         */
        public static OMElement getElement(final OMElement piElement, final ElementName elemName) {
            return piElement.getFirstChildWithName((elemName == ElementName.FROM ? Q_FROM_PARTY : Q_TO_PARTY));
        }


        /**
         * Reads the trading partner information from a <code>From</code> or <code>To</code> element and returns it as
         * a {@link org.holodeckb2b.common.messagemodel.TradingPartner} object.
         *
         * @param tpElement     The element to read the information from
         * @return              A {@link org.holodeckb2b.common.messagemodel.TradingPartner} object containing
         *                      the information from the element
         */
        public static org.holodeckb2b.common.messagemodel.TradingPartner readElement(final OMElement tpElement) {
            if (tpElement == null)
                return null; // If there is no element content, there is no data

            // Create the entity object
            final org.holodeckb2b.common.messagemodel.TradingPartner tpData =
                                                        new org.holodeckb2b.common.messagemodel.TradingPartner();

            // Check for a Role element and use its value
            final OMElement roleElement = tpElement.getFirstChildWithName(Q_ROLE);
            if (roleElement != null && !Utils.isNullOrEmpty(roleElement.getText()))
                tpData.setRole(roleElement.getText());

            // Read all PartyId elements and add info to entity
            final Iterator<?> it = tpElement.getChildrenWithName(Q_PARTYID);
            while (it.hasNext()) {
                final OMElement pidElem = (OMElement) it.next();
                // Add PartyId to trading partner info
                tpData.addPartyId(new PartyId(pidElem.getText(),pidElem.getAttributeValue(new QName(LN_PARTYID_TYPE))));
            }

            return tpData;
        }
     }
}
