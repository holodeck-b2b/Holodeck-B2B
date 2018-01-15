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

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.util.base64.Base64EncodingWriterOutputStream;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.mmd.xml.Property;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.packaging.PayloadInfoElement;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is the {@link IMessageDeliverer} that implements the <i>"ebms"</i> format of the default file delivery method.
 * <p>It delivers the message unit to the business application by writing all information of the message unit info,
 * <b>including</b> the payload data of a <i>User Message</i> message unit to one XML file. The format of the XML
 * document is defined by the XML schema <code>http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml</code>.
 * <p>It includes the ebMS messaging header followed by a <code>Payloads</code> element that includes all payloads of
 * the  message as <code>Payload</code> elements. Because the payload can contain binary data their content is included
 * <i>base64</i> encoded. The payloads are referenced using the <code>xml:id</code> attribute of a <code>Payload</code>
 * element which is included as a <i>"Part Property"</i> with name "<i>org:holodeckb2b:ref</i>".
 * <p><b>Examples</b>
 * <p><u>User message</u>
 * <p>For a received user message unit containing one payload the message info file is like this:
<pre>
{@code
<ebmsMessage xmlns="http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml">
    <eb3:UserMessage xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
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
            <eb3:Service type="org:holodeckb2b:test">single_xml</eb3:Service>
            <eb3:Action>Test</eb3:Action>
            <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>
        </eb3:CollaborationInfo>
        <eb3:PayloadInfo>
            <eb3:Payload>
                <eb3:PartProperties>
                    <eb3:Property name="org:holodeckb2b:ref">pl-1</eb3:Property>
                </eb3:PartProperties>
            </eb3:Payload>
        </eb3:PayloadInfo>
    </eb3:UserMessage>
    <Payloads>
        <Payload xml:id="pl-1">«base64 encoded data»</Payload>
    </Payloads>
</ebmsMessage>
}</pre>
 * <p><u>Receipt</u>
 * <p>For a received Receipt message unit containing a reception awareness receipt the message info file is like this:
<pre>
{@code
<ebmsMessage xmlns="http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml">
    <eb3:SignalMessage xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
        <eb3:MessageInfo>
            <eb3:Timestamp>2014-08-14T17:50:00.000+02:00</eb3:Timestamp>
            <eb3:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb3:MessageId>
        </eb3:MessageInfo>
        <eb3:Receipt>
            <ReceiptChild xmlns="http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild"
            >eb3:UserMessage</ReceiptChild>
        </eb3:Receipt>
    </eb3:SignalMessage>
</ebmsMessage>
}</pre>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see FileDeliveryFactory
 */
public class SingleXMLDeliverer extends EbmsFileDeliverer {

    /**
     * The namespace URI of the XML schema that defines the structure of the delivery file
     */
    private static final String DELIVERY_NS_URI = "http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml";
    /**
     * The QName of the container element that contains the message info
     */
    private static final String XML_ROOT_NAME = "ebmsMessage";

    /**
     * Constructs a new deliverer which will write the files to the given directory.
     *
     * @param dir   The directory where file should be written to.
     */
    SingleXMLDeliverer(final String dir) {
        super(dir);
    }

    /**
     * Create the root element of the meta-data document.
     *
     * @return  The root element of the delivery document.
     */
    protected OMElement createContainerElementName() {
        final OMFactory   f = OMAbstractFactory.getOMFactory();
        final OMElement rootElement = f.createOMElement(XML_ROOT_NAME, DELIVERY_NS_URI, XMLConstants.DEFAULT_NS_PREFIX);
        // Declare the namespaces
        rootElement.declareDefaultNamespace(DELIVERY_NS_URI);
        rootElement.declareNamespace(EbMSConstants.EBMS3_NS_URI, EbMSConstants.EBMS3_NS_PREFIX);

        return rootElement;
    }

    /**
     * Delivers the user message to business application.
     *
     * @param usrMsgUnit        The user message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the user message to the business
     *                                  application
     */
    @Override
    protected void deliverUserMessage(final IUserMessage usrMsgUnit) throws MessageDeliveryException {
        log.debug("Delivering user message with msgId=" + usrMsgUnit.getMessageId());

        // We first convert the user message into a MMD document
        final MessageMetaData mmd = new MessageMetaData((IUserMessage) usrMsgUnit);

        final OMElement    container = createContainerElementName();

        log.debug("Add general message info to XML container");
        // Add the information on the user message to the container
        final OMElement  usrMsgElement = UserMessageElement.createElement(container, mmd);

        if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
            log.debug("Add payload meta info to XML container");
            // Generate a element id and set this a reference in payload property
            int i = 1;
            for (final IPayload p : mmd.getPayloads()) {
                final Property refProp = new Property();
                refProp.setName("org:holodeckb2b:ref");
                refProp.setValue("pl-" + i++);
                p.getProperties().add(refProp);
            }
            PayloadInfoElement.createElement(usrMsgElement, mmd.getPayloads());
        }

        String msgFilePath = null;
        FileWriter fw = null;
        try {
            msgFilePath = Utils.createFileWithUniqueName(directory + "message-"
                                                            + mmd.getMessageId().replaceAll("[^a-zA-Z0-9.-]", "_")
                                                            + ".xml").toString();
            log.debug("Message meta data complete, start writing this to file " + msgFilePath);
            fw = new FileWriter(msgFilePath);
            final XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(fw);
            log.debug("Write the meta data to file");
            xmlWriter.writeStartElement(XML_ROOT_NAME);
            usrMsgElement.serialize(xmlWriter);
            xmlWriter.flush();
            xmlWriter.close();
            log.debug("Meta data writen to file");
            if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
                log.debug("Write payload contents");
                fw.write("<Payloads>");
                int i = 1;
                for(final IPayload p : mmd.getPayloads()) {
                    log.debug("Create <Payload> element");
                    fw.write("<Payload xml:id=\"pl-" + i++ + "\">");
                    writeEncodedPayload(p.getContentLocation(), fw);
                    fw.write("</Payload>\n");
                }
                log.debug("Close the <Payloads> element");
                fw.write("</Payloads>\n");
            }
            fw.write("</" + XML_ROOT_NAME + ">");
            fw.close();
            log.info("User message with msgID=" + mmd.getMessageId() + " successfully delivered");
        } catch (IOException | XMLStreamException ex) {
            log.error("An error occurred while delivering the user message [" + mmd.getMessageId()
                                                                    + "]\n\tError details: " + ex.getMessage());
            // Remove the delivery file (if it was already created)
            if (!Utils.isNullOrEmpty(msgFilePath))
                try {
                    if (fw != null) fw.close();
                    Files.deleteIfExists(Paths.get(msgFilePath));
                } catch (IOException io) {
                    log.error("Could not remove temp file [" + msgFilePath.toString() + "]! Remove manually.");
                }
            // And signal failure
            throw new MessageDeliveryException("Unable to deliver user message [" + mmd.getMessageId()
                                                    + "]. Error details: " + ex.getMessage());
        }
    }

    /**
     * Helper method to write the payload content base64 encoded to an output stream.
     *
     * @param sourceFile        The file to add to the output
     * @param output            The output writer
     * @throws IOException      When reading from the source or writing to the output fails
     */
    private void writeEncodedPayload(final String sourceFile, final Writer output) throws IOException {
        final Base64EncodingWriterOutputStream b64os;
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            b64os = new Base64EncodingWriterOutputStream(output);
            final byte[] buffer = new byte[4096];
            int r = fis.read(buffer);
            while (r > 0) {
                b64os.write(buffer, 0, r);
                r = fis.read(buffer);
            }
        }
        b64os.complete();
        b64os.flush();
    }

}
