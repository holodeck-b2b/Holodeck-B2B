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

import java.text.ParseException;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is a helper class for handling the <code>MessageInfo</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.1 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MessageInfoElement {

    /**
     * The fully qualified name of the element as an {@see QName}
     */
    public static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");

    /**
     * The fully qualified name of the Timestamp element as an {@see QName}
     */
    private static final QName  Q_TIMESTAMP = new QName(EbMSConstants.EBMS3_NS_URI, "Timestamp");

    /**
     * The fully qualified name of the MessageId element as an {@see QName}
     */
    private static final QName  Q_MESSAGEID = new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");

    /**
     * The fully qualified name of the RefToMessageId element as an {@see QName}
     */
    private static final QName  Q_REFTO_MESSAGEID = new QName(EbMSConstants.EBMS3_NS_URI, "RefToMessageId");

    /**
     * Creates a <code>eb:MessageInfo</code> element for the given {@link IMessageUnit} object. This element is added as
     * a child to the element representing the message unit.
     *
     * @param muElement     The element representing the given message unit
     * @param data          The message unit information as an {@see IMessageUnit}
     * @return              The newly created <code>eb:MessageInfo</code> element.
     */
    public static OMElement createElement(final OMElement muElement, final IMessageUnit data) {
        final OMFactory f = muElement.getOMFactory();

        // Create the element
        final OMElement msginfo = f.createOMElement(Q_ELEMENT_NAME, muElement);

        // Add content
        final OMElement timestamp = f.createOMElement(Q_TIMESTAMP, msginfo);
        timestamp.setText(Utils.toXMLDateTime(data.getTimestamp()));

        final OMElement messageId = f.createOMElement(Q_MESSAGEID, msginfo);
        messageId.setText(data.getMessageId());

        final String refToMsgId = data.getRefToMessageId();
        if (!Utils.isNullOrEmpty(refToMsgId)) {
            final OMElement  refToMsgIdElement = f.createOMElement(Q_REFTO_MESSAGEID, msginfo);
            refToMsgIdElement.setText(refToMsgId);
        }

        return msginfo;
    }

    /**
     * Gets the {@link OMElement} object that represents the <code>MessageInfo</code> child element of the given <code>
     * UserMessage</code> or <code>SignalMessage</code> element.
     *
     * @param piElement     The parent element (either <code>UserMessage</code> or <code>SignalMessage</code>)
     * @return              The {@link OMElement} object representing the <code>MessageInfo</code> element or,<br>
     *                      <code>null</code> when the requested element is not found as child of the given element.
     */
    public static OMElement getElement(final OMElement piElement) {
        return piElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the information from the <code>eb:MessageInfo</code> element and stores it in the given implementation of
     * {@link MessageUnit}.
     * <p>Because the information read from the <code>eb:MessageInfo</code> is not stored in the message unit objects
     * themselves this method does not return an object with the info read from the header element but directly stores
     * the information in the provided message unit object.
     *
     * @param miElement     The {@link OMElement} object representing the <code>MessageInfo</code> element to read the
     *                      information from
     * @param msgUnit       The {@link MessageUnit} where information should be stored in
     */
    public static void readElement(final OMElement miElement, final MessageUnit msgUnit) {
        if (miElement == null)
            return;

        try {
            msgUnit.setTimestamp(Utils.fromXMLDateTime(miElement.getFirstChildWithName(Q_TIMESTAMP).getText()));
        } catch (final OMException | ParseException e) {
            msgUnit.setTimestamp(null);
        }

        final OMElement messageId = miElement.getFirstChildWithName(Q_MESSAGEID);
        if (messageId != null && !Utils.isNullOrEmpty(messageId.getText()))
            msgUnit.setMessageId(messageId.getText());
        else
            msgUnit.setMessageId(null);

        final OMElement  refToMsgId = miElement.getFirstChildWithName(Q_REFTO_MESSAGEID);
        if (refToMsgId != null && !Utils.isNullOrEmpty(refToMsgId.getText()))
            msgUnit.setRefToMessageId(refToMsgId.getText());
    }
}
