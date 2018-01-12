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

import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.common.messagemodel.Receipt;

/**
 * Is a helper class for handling the ebMS Receipt Signal message units in the ebMS SOAP header, i.e the
 * <code>eb:Receipt</code> element and its sibling <code>eb:MessageInfo</code>.
 * <p>The Receipt signal may contain any kind of element as child elements. For packaging this means we do not know what
 * the actual XML content will be so it has to be supplied when constructing the element. Similar when reading the
 * element the XML content is returned.
 * <p>The Receipt signal message unit is specified in section 5.2.3.3 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReceiptElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    public static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Receipt");

    /**
     * Creates a new <code>eb:SignalMessage</code> for a <i>Receipt Signal</i> message unit.
     *
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param receipt       The information to include in the receipt signal
     * @return              The new element representing the receipt signal
     */
    public static OMElement createElement(final OMElement messaging, final IReceipt receipt) {
        // First create the SignalMessage element that is the placeholder for
        // the Receipt element containing the receipt info
        final OMElement signalmessage = SignalMessageElement.createElement(messaging);

        // Create the generic MessageInfo element
        MessageInfoElement.createElement(signalmessage, receipt);

        // Create the Receipt element
        final OMElement rcptElement = signalmessage.getOMFactory().createOMElement(Q_ELEMENT_NAME, signalmessage);

        // Add the child elements as given in Receipt object
        for(final OMElement e : receipt.getContent())
            rcptElement.addChild(e);

        return signalmessage;
    }

    /**
     * Reads the information from a <code>eb:SignalMessage</code> and its child elements that contain the Receipt signal
     * message unit and stores it a {@link Receipt} object.
     *
     * @param sigElement    The parent <code>eb:SignalMessage</code> element that contains the <code>eb:Receipt</code>
     *                      element
     * @return              The {@link Receipt} object containing the information on
     *                      the receipt
     */
    public static Receipt readElement(final OMElement sigElement) {
        // Create a new Receipt entity object to store the information in
        final Receipt rcptData = new Receipt();

        // First read general information from the MessageInfo child
        MessageInfoElement.readElement(MessageInfoElement.getElement(sigElement), rcptData);

        // Because the content of the Receipt is not predefined read and store all child elements of the Receipt element
        rcptData.setContent(sigElement.getFirstChildWithName(Q_ELEMENT_NAME).getChildElements());

        return rcptData;
    }

    /**
     * Gets an {@link Iterator} for all <code>eb:SignalMessage</code> elements from the given ebMS 3 Messaging header in
     * the SOAP message that represent <i>Receipt Signal</i> message units.
     *
     * @param messaging   The SOAP Header block that contains the ebMS header,i.e. the <code>eb:Messaging</code> element
     * @return      An {@link Iterator} for all {@link OMElement}s representing a <code>eb:SignalMessage</code> element
     *              that contains an Receipt Signal, i.e. has one or more <code>eb:Receipt</code> child elements
     */
    public static Iterator<OMElement> getElements(final SOAPHeaderBlock messaging) {
        // Check all SignalMessage elements in the header
        final Iterator<?> signals = SignalMessageElement.getElements(messaging);

        final ArrayList<OMElement>  receipts = new ArrayList<>();
        while(signals.hasNext()) {
            final OMElement signal  = (OMElement) signals.next();
            // If this SignalMessage element has a Receipt child,
            //   it is an Receipt signal and should be returned
            if (signal.getFirstChildWithName(Q_ELEMENT_NAME) != null)
                receipts.add(signal);
        }

        return receipts.iterator();
    }
}
