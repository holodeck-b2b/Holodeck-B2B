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
package org.holodeckb2b.deliverymethod.file;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.mmd.xml.Property;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;

/**
 * Is an {@link IMessageDeliverer} implementation that delivers the message unit to the business application by writing
 * the message unit info to a file using the same format as in the ebMS messaging header as defined in the xml schema
 * definition <code>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/</code>. Because a file is generated
 * per message unit the <code>eb:Messaging</code> element contains only one child element.
 * <p>As only Receipt and Error signals are delivered (notified in ebMS terminology) to the business application the
 * <code>eb:SignalMessage</code> element can only have the <code>eb:Receipt</code> and <code>eb:Error</code> child
 * elements (in addition to the required <code>eb:MessageInfo</code> element).
 * <p>For receipts the <code>eb:Receipt</code> element must contain at least one child element. Because the content
 * as included in the actual ebMS message is not for delivery it is replaced with a single child element
 * <code>ReceiptChild</code> that contains the name (including namespace prefix if available) of the first element
 * contained in the actuals AS4 message. The <code>ReceiptChild</code> element itself is defined in xml schema
 * definition <code>http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild</code>.
 * <p>For user messages the payloads are copied to the same directory and referred to through a <i>additional</i> part
 * property named "<i>org:holodeckb2b:location</i>" in <code>eb:PartProperties/eb:Property</code>.
 * <p><b>Examples</b>
 * <p><u>User message</u>
 * <p>For a received user message unit containing two payloads there will be one XML file containing the
 * message info and two files containing the payload content. The message info file is like this:
<pre>
{@code
<eb:Messaging xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
    <eb:UserMessage>
        <eb:MessageInfo>
            <eb:Timestamp>2014-08-14T17:50:00.000+02:00</eb:Timestamp>
            <eb:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb:MessageId>
        </eb:MessageInfo>
        <eb:PartyInfo>
            <eb:From>
                <eb:PartyId type="org:holodeckb2b:test">Party_1</eb:PartyId>
                <eb:Role>Sender</eb:Role>
            </eb:From>
            <eb:To>
                <eb:PartyId type="org:holodeckb2b:test">Party_2</eb:PartyId>
                <eb:Role>Receiver</eb:Role>
            </eb:To>
        </eb:PartyInfo>
        <eb:CollaborationInfo>
            <eb:Service type="org:holodeckb2b:test">service</eb:Service>
            <eb:Action>Test</eb:Action>
            <eb:ConversationId>org:holodeckb2b:test:conversation</eb:ConversationId>
        </eb:CollaborationInfo>
        <eb:PayloadInfo>
            <eb:Payload>
                <eb:PartProperties>
                    <eb:Property name="originalFileName">simpletest.xml</eb:Property>
                    <eb:Property name="org:holodeckb2b:location"
                        >/Users/safi/holodeck-test/pl-d6a19bec-4ffa-4e9c-862b-6dc127b00077_mountain-lion.fritz.box-body-16.xml</eb:Property>
                </eb:PartProperties>
            </eb:Payload>
        </eb:PayloadInfo>
    </eb:UserMessage>
</eb:Messaging>
}</pre>
 * <p><u>Receipt</u>
 * <p>For a received Receipt message unit containing a non repudiation receipt the message info file is like this:
<pre>
{@code
<eb:Messaging xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
    <eb:SignalMessage>
        <eb:MessageInfo>
            <eb:Timestamp>2014-08-14T17:50:00.000+02:00</eb:Timestamp>
            <eb:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb:MessageId>
        </eb:MessageInfo>
        <eb:Receipt>
            <ReceiptChild xmlns="http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild"
            >ebbp:NonRepudiationInformation</ReceiptChild>
        </eb:Receipt>
    </eb:SignalMessage>
</eb:Messaging>
}</pre>
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see FileDeliveryFactory
 */
public class SimpleFileDeliverer extends AbstractFileDeliverer {

    /**
     * The QName of the container element that contains the message info, i.e. the <code>eb:Messaging</code> element
     */
    protected static final QName XML_ROOT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Messaging", "eb");

    protected static final String RECEIPT_CHILD_NS_URI =
                                                "http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild";
    protected static final String RECEIPT_CHILD_ELEM_NAME = "ReceiptChild";
    /**
     * Constructs a new deliverer which will write the files to the given directory.
     *
     * @param dir   The directory where file should be written to.
     */
    public SimpleFileDeliverer(final String dir) {
        super(dir);
    }

    /**
     * Writes the user message meta data to file using the same structure as in the ebMS header.
     *
     * @param mmd           The user message meta data.
     * @throws IOException  When the information could not be written to disk.
     */
    @Override
    protected void writeUserMessageInfoToFile(final MessageMetaData mmd) throws IOException {
        final OMFactory   f = OMAbstractFactory.getOMFactory();
        final OMElement    container = f.createOMElement(XML_ROOT_NAME);
        container.declareNamespace(EbMSConstants.EBMS3_NS_URI, "eb");

        if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
            log.debug("Add payload info to XML container");
            // We add the local file location as a Part property
            for (final IPayload p : mmd.getPayloads()) {
                final Property locationProp = new Property();
                locationProp.setName("org:holodeckb2b:location");
                locationProp.setValue(p.getContentLocation());
                p.getProperties().add(locationProp);
            }
        }

        log.debug("Add message info to XML container");
        // Add the information on the user message to the container
        final OMElement  usrMsgElement = UserMessageElement.createElement(container, mmd);
        log.debug("Information complete, write XML document to file");

        writeXMLDocument(container, mmd.getMessageId());
    }

    /**
     * Writes the signal message meta data to file using the same structure as in the ebMS header. For Receipt signals
     * the content of the receipt element is removed before writing the info to file.
     *
     * @param  sigMsgUnit        The signal message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the signal message to the business
     *                                  application
     */
    @Override
    protected void deliverSignalMessage(final ISignalMessage sigMsgUnit) throws MessageDeliveryException {
        final OMFactory   f = OMAbstractFactory.getOMFactory();
        final OMElement    container = f.createOMElement(XML_ROOT_NAME);
        container.declareNamespace(EbMSConstants.EBMS3_NS_URI, "eb");

        if (sigMsgUnit instanceof IReceipt) {
            log.debug("Create a new Receipt to prevent content from inclusion in XML");
            IReceipt deliveryReceipt = createDeliveryReceipt((IReceipt) sigMsgUnit);
            log.debug("Add receipt meta data to XML");
            ReceiptElement.createElement(container, deliveryReceipt);
        } else if (sigMsgUnit instanceof IErrorMessage) {
            log.debug("Add error meta data to XML");
            ErrorSignalElement.createElement(container, (IErrorMessage) sigMsgUnit);
        }

        log.debug("Added signal meta data to XML, write to disk");
        try {
            writeXMLDocument(container, sigMsgUnit.getMessageId());
            log.info("Signal message with msgID=" + sigMsgUnit.getMessageId() + " successfully delivered");
        } catch (final IOException ex) {
            log.error("An error occurred while delivering the signal message [" + sigMsgUnit.getMessageId()
                                                                    + "]\n\tError details: " + ex.getMessage());
            // And signal failure
            throw new MessageDeliveryException("Unable to deliver signal message [" + sigMsgUnit.getMessageId()
                                                    + "]. Error details: " + ex.getMessage());
        }
    }

    /**
     * Helper to write the XML to file. Serializes the given XML element to file named
     * "<code>mi-«<i>message id</i>».xml</code>".
     *
     * @param xml       The xml element to write to file
     * @param msgId     The message id of the message unit the XML is the meta data of
     * @return          The path to the new file containing the XML document
     * @throws IOException When the XML can not be written to disk
     */
    protected String writeXMLDocument(final OMElement xml, final String msgId) throws IOException {
        final Path msgFilePath = Utils.createFileWithUniqueName(directory + "mi-"
                                                                    + msgId.replaceAll("[^a-zA-Z0-9.-]", "_")
                                                                    + ".xml");

        try {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(
                                                                                new FileWriter(msgFilePath.toString()));
            xml.serialize(writer);
            writer.close();

            return msgFilePath.toString();
        } catch (final Exception ex) {
            // Can not write the message info XML to file -> delivery not possible
            // Try to remove the already created file
            try {
                Files.deleteIfExists(msgFilePath);
            } catch (IOException io) {
                log.error("Could not remove temp file [" + msgFilePath.toString() + "]! Remove manually.");
            }
            throw new IOException("Error writing message unit info to file!", ex);
        }
    }

    /**
     * Helper class to create a new Receipt that includes only the name of the first element of the original Receipt's
     * content.
     *
     * @param originalReceipt   The original Receipt
     * @return  A new Receipt with only the name of the first element of the original as content.
     */
    private IReceipt createDeliveryReceipt(IReceipt originalReceipt) {
        Receipt deliveryReceipt = new Receipt(originalReceipt);

        final OMElement rcptChild = OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEM_NAME,
                                                                               RECEIPT_CHILD_NS_URI, "");
        rcptChild.declareDefaultNamespace(RECEIPT_CHILD_NS_URI);

        // If there was actual content (and we could get access to it) use the name of first element
        final List<OMElement>  actual = originalReceipt.getContent();
        String firstChildName = null;
        if (!Utils.isNullOrEmpty(actual)) {
            final OMElement firstChild = actual.get(0);
            firstChildName = Utils.getValue(firstChild.getPrefix(), "");
            firstChildName += (firstChildName.isEmpty() ? "" : ":") + firstChild.getLocalName();
        } else
            // If we can not access the actual content use a descriptive text
            firstChildName = "Receipt content unknown";
        rcptChild.setText(firstChildName);

        deliveryReceipt.addElementToContent(rcptChild);

        return deliveryReceipt;
    }

}
