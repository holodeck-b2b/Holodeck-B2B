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
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;

/**
 * Is a helper class for handling the <code>CollaborationInfo</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.6 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CollaborationInfoElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "CollaborationInfo");

    /**
     * The fully qualified name of the Action element as an {@link QName}
     */
    private static final QName  Q_ACTION = new QName(EbMSConstants.EBMS3_NS_URI, "Action");

    /**
     * The fully qualified name of the element ConversationId as an {@link QName}
     */
    private static final QName  Q_CONVERSATIONID = new QName(EbMSConstants.EBMS3_NS_URI, "ConversationId");

    /**
     * Creates a <code>CollaborationInfo</code> element and adds it to the given <code>UserMessage</code> element.
     *
     * @param umElement     The <code>UserMessage</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement umElement, final ICollaborationInfo data) {
        final OMFactory f = umElement.getOMFactory();

        // Create the element
        final OMElement collabInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);

        // Fill it based on the given data
        if (data.getAgreement() != null)
            AgreementRefElement.createElement(collabInfo, data.getAgreement());

        ServiceElement.createElement(collabInfo, data.getService());

        // Elements Action and ConversationId are so simple that they are created here
        final OMElement action = f.createOMElement(Q_ACTION, collabInfo);
        action.setText(data.getAction());

        final OMElement convId = f.createOMElement(Q_CONVERSATIONID, collabInfo);
        convId.setText(data.getConversationId());

        return collabInfo;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>CollaborationInfo</code> child element of the
     * <code>UserMessage</code> element.
     *
     * @param   parent     The parent <code>UserMessage</code> element
     * @return             The {@link OMElement} object representing the <code>CollaborationInfo</code> element,or<br>
     *                     <code>null</code> when there is no <code>CollaborationInfo</code> element as child of the
     *                     given element.
     */
    public static OMElement getElement(final OMElement parent) {
        return parent.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the business transaction information from the User Message message unit contained in the <code>
     * CollaborationInfo</code> element and returns it in a new {@link
     * org.holodeckb2b.common.messagemodel.CollaborationInfo} object.
     *
     * @param ciElement    The <code>CollaborationInfo</code> element that contains the info about this User Message
     *                     message unit
     * @return             The {@link org.holodeckb2b.common.messagemodel.CollaborationInfo} object the information is
     *                     returned in
     */
    public static org.holodeckb2b.common.messagemodel.CollaborationInfo readElement(final OMElement ciElement) {
        // There must be a CollaborationInfo element
        if (ciElement == null)
            return null;

        // Create new empty object
        final org.holodeckb2b.common.messagemodel.CollaborationInfo ciData =
                                                        new org.holodeckb2b.common.messagemodel.CollaborationInfo();

        // Start with reading the required elements: Service, Action and ConversationId
        OMElement child = ServiceElement.getElement(ciElement);
        if (child != null)
            // Read the Service element and store info in entity object
            ciData.setService(ServiceElement.readElement(child));

        // Action child element
        child = ciElement.getFirstChildWithName(Q_ACTION);
        if (child != null)
            ciData.setAction(child.getText());

        // ConversationId child element
        child = ciElement.getFirstChildWithName(Q_CONVERSATIONID);
        if (child != null)
            ciData.setConversationId(child.getText());

        // Get and read optional AgreementRef child element
        child = AgreementRefElement.getElement(ciElement);
        if (child != null)
            ciData.setAgreement(AgreementRefElement.readElement(child));

        return ciData;
    }
}
