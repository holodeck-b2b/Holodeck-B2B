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
import java.util.Arrays;
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
 * Is the {@link IMessageDeliverer} that implements the <i>"ebms"</i> format of the default file delivery method.
 * <p>It delivers the message unit to the business application by writing the message unit info to a file using the same
 * format as in the ebMS messaging header as defined in the xml schema definition
 * <code>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/</code>.
 * <p>As only Receipt and Error signals are delivered (notified in ebMS terminology) to the business application the
 * <code>eb:SignalMessage</code> element can only have the <code>eb:Receipt</code> and <code>eb:Error</code> child
 * elements (in addition to the required <code>eb:MessageInfo</code> element).
 * <p>For Receipts the actual content of the <code>eb:Receipt</code> element as included in the ebMS message is replaced
 * with a single child element <code>ReceiptChild</code> that contains a identifier of the type of Receipt. See the XML
 * schema definition <code>http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild</code>.
 * <p>For user messages the payloads are copied to the same directory and referred to through a <i>additional</i> part
 * property named "<i>org:holodeckb2b:location</i>" in <code>eb:PartProperties/eb:Property</code>.
 * <p><b>Examples</b>
 * <p><u>User message</u>
 * <p>For a received user message unit containing two payloads there will be one XML file containing the message info
 * and two files containing the payload content. The message info file is like this:
<pre>
{@code
<eb3:Messaging xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
    <eb3:UserMessage>
        <eb3:MessageInfo>
            <eb3:Timestamp>2014-08-14T17:50:00.000+02:00</eb3:Timestamp>
            <eb3:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb3:MessageId>
        </eb3:MessageInfo>
        <eb3:PartyInfo>
            <eb3:From>
                <eb3:PartyId type="org:holodeckb2b:test">Party_1</eb3:PartyId>
                <eb3:Role>Sender</eb3:Role>
            </eb3:From>
            <eb3:To>
                <eb3:PartyId type="org:holodeckb2b:test">Party_2</eb3:PartyId>
                <eb3:Role>Receiver</eb3:Role>
            </eb3:To>
        </eb3:PartyInfo>
        <eb3:CollaborationInfo>
            <eb3:Service type="org:holodeckb2b:test">service</eb3:Service>
            <eb3:Action>Test</eb3:Action>
            <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>
        </eb3:CollaborationInfo>
        <eb3:PayloadInfo>
            <eb3:Payload>
                <eb3:PartProperties>
                    <eb3:Property name="originalFileName">simpletest.xml</eb3:Property>
                    <eb3:Property name="org:holodeckb2b:location">
                        /Users/safi/holodeck-test/pl-d6a19bec-4ffa-4e9c-862b-6dc127b00077_mountain-lion.fritz.box-body-16.xml
                    </eb3:Property>
                </eb3:PartProperties>
            </eb3:Payload>
        </eb3:PayloadInfo>
    </eb3:UserMessage>
</eb3:Messaging>
}</pre>
 * <p><u>Receipt</u>
 * <p>For a received Receipt message unit containing a non repudiation receipt the message info file is like this:
<pre>
{@code
<eb3:Messaging xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
    <eb3:SignalMessage>
        <eb3:MessageInfo>
            <eb3:Timestamp>2014-08-14T17:50:00.000+02:00</eb3:Timestamp>
            <eb3:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb3:MessageId>
        </eb3:MessageInfo>
        <eb3:Receipt>
            <ReceiptChild xmlns="http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild"
            >ebbp:NonRepudiationInformation</ReceiptChild>
        </eb3:Receipt>
    </eb3:SignalMessage>
</eb3:Messaging>
}</pre>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see FileDeliveryFactory
 */
public class EbmsFileDeliverer extends AbstractFileDeliverer {

    /**
     * The QName of the container element that contains the message info, i.e. the <code>eb:Messaging</code> element
     */
    protected static final QName ROOT_QNAME = new QName(EbMSConstants.EBMS3_NS_URI, "Messaging",
                                                           EbMSConstants.EBMS3_NS_PREFIX);

    protected static final String RECEIPT_CHILD_NS_URI =
                                                "http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild";
    protected static final String RECEIPT_CHILD_ELEM_NAME = "ReceiptChild";
    /**
     * Constructs a new deliverer which will write the files to the given directory.
     *
     * @param dir   The directory where file should be written to.
     */
    public EbmsFileDeliverer(final String dir) {
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
        final OMElement    container = f.createOMElement(ROOT_QNAME);
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
        final OMElement   container = createContainerElementName();

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
     * Create the root element of the meta-data document.
     *
     * @return  The root element of the delivery document.
     */
    protected OMElement createContainerElementName() {
        final OMFactory   f = OMAbstractFactory.getOMFactory();
        final OMElement rootElement = f.createOMElement(ROOT_QNAME);
        rootElement.declareNamespace(EbMSConstants.EBMS3_NS_URI, EbMSConstants.EBMS3_NS_PREFIX);

        return rootElement;
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
    private String writeXMLDocument(final OMElement xml, final String msgId) throws IOException {
        final Path msgFilePath = Utils.createFileWithUniqueName(directory + "mi-"
                                                                    + msgId.replaceAll("[^a-zA-Z0-9.-]", "_")
                                                                    + ".xml");

        try (final FileWriter fw = new FileWriter(msgFilePath.toString())) {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fw);
            xml.serialize(writer);
            writer.flush();
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
        final String firstReceiptChildName = originalReceipt.getContent().get(0).getLocalName();
        String mmdRcptName;
        switch (firstReceiptChildName) {
            case "NonRepudiationInformation" :
                mmdRcptName = "ebbp:NonRepudiationInformation"; break;
            case "UserMessage" :
                mmdRcptName = "eb:UserMessage"; break;
            default :
                mmdRcptName = "unspecified";
         }
        rcptChild.setText(mmdRcptName);

        deliveryReceipt.setContent(Arrays.asList(new OMElement[] { rcptChild }));

        return deliveryReceipt;
    }
}
