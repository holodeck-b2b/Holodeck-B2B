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

import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

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
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");

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
    public static OMElement createElement(final OMElement messaging, final IUserMessage data) {
        final OMFactory f = messaging.getOMFactory();

        // Create the element
        final OMElement usermessage = f.createOMElement(Q_ELEMENT_NAME, messaging);

        // Fill it based on the given data

        // MPC attribute only set when not default
        final String mpc = data.getMPC();
        if (mpc != null && !mpc.equals(EbMSConstants.DEFAULT_MPC))
            usermessage.addAttribute(MPC_ATTR, mpc, null);

        // Create the MessageInfo element
        MessageInfo.createElement(usermessage, data);
        // Create the PartyInfo element
        PartyInfo.createElement(usermessage, data);
        // Create the CollaborationInfo element
        CollaborationInfo.createElement(usermessage, data.getCollaborationInfo());
        // Create the MessageProperties element (if there are message properties)
        final Collection<IProperty> msgProps = data.getMessageProperties();
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
    public static Iterator<?> getElements(final OMElement messaging) {
        return messaging.getChildrenWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the meta data of a User Message message unit from the <code>eb:UserMessage</code> element and return it as
     * a {@link org.holodeckb2b.ebms3.persistency.entities.UserMessage} entity object.
     * <p><b>NOTE :</b> The entity object is not persisted by this method! It is the responsibility of the caller to
     * store it.
     *
     * @param   umElement           The <code>UserMessage</code> element that contains the meta data to read
     * @return                      A new {@link org.holodeckb2b.ebms3.persistency.entities.UserMessage} object
     */
    public static org.holodeckb2b.ebms3.persistency.entities.UserMessage readElement(final OMElement umElement) {
        // Create a new entity object to store the information in
        final org.holodeckb2b.ebms3.persistency.entities.UserMessage umData =
                                                        new org.holodeckb2b.ebms3.persistency.entities.UserMessage();

        // Read the [optional] mpc attribute
        final String  mpc = umElement.getAttributeValue(new QName(MPC_ATTR));
        // If there was no mpc attribute or it was empty (which formally is illegal because the mpc should be a valid
        // URI) it is set to the default MPC
        umData.setMPC(Utils.isNullOrEmpty(mpc) ? EbMSConstants.DEFAULT_MPC : mpc);

        // Get the MessageInfo element
        OMElement child = MessageInfo.getElement(umElement);
        // Read the MessageInfo element and store info in the persistency object
        MessageInfo.readElement(child, umData);

        // Get and read the PartyInfo element
        PartyInfo.readElement(PartyInfo.getElement(umElement), umData);

        // Get and read the CollaborationInfo element
        umData.setCollaborationInfo(CollaborationInfo.readElement(CollaborationInfo.getElement(umElement)));

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
