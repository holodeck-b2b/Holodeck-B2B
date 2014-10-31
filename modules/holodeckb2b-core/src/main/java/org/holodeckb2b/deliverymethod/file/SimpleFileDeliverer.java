/*
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
import java.util.ArrayList;
import java.util.Date;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.delivery.MessageDeliveryException;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.messagemodel.IErrorMessage;
import org.holodeckb2b.common.messagemodel.IMessageUnit;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.common.messagemodel.IReceipt;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.mmd.xml.PartInfo;
import org.holodeckb2b.ebms3.mmd.xml.Property;
import org.holodeckb2b.ebms3.packaging.ErrorSignal;
import org.holodeckb2b.ebms3.packaging.Receipt;
import org.holodeckb2b.ebms3.packaging.UserMessage;

/**
 * Is an {@link IMessageDeliverer} implementation that delivers the message unit to the business application by writing
 * the message unit info to a file using the same format as in the ebMS messaging header. For user message the payload
 * contents are copied to the same directory and referred to through a "part property" named "<i>org:holodeckb2b:ref</i>
 * in <code>eb:PartProperties/eb:Property</code> 
 * <p>Example: For a received user message unit containing two payloads there will be one XML file containing the 
 * message info and two files containing the payload content. The message info file is like this:
<pre>
{@code 
<ebmsMessageData xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
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
            <eb:PartInfo>
                <eb:PartProperties>
                    <eb:Property name="originalFileName">simpletest.xml</eb:Property>
                    <eb:Property name="org:holodeckb2b:location"
                        >/Users/safi/holodeck-test/pl-d6a19bec-4ffa-4e9c-862b-6dc127b00077_mountain-lion.fritz.box-body-16.xml</eb:Property>
                </eb:PartProperties>
            </eb:PartInfo>
        </eb:PayloadInfo>
    </eb:UserMessage>
</ebmsMessageData>
}</pre>
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see AbstractFileDeliverer
 */
public class SimpleFileDeliverer extends AbstractFileDeliverer {

    /**
     * The element name of the container element that contains the message info
     */
    protected static QName XML_ROOT_NAME = new QName("ebmsMessageData");
            
    /**
     * Constructs a new deliverer which will write the files to the given directory.
     * 
     * @param dir   The directory where file should be written to.
     */
    SimpleFileDeliverer(String dir) {
        super(dir);
    }

    /**
     * Writes the user message meta data to file using the same structure as in the ebMS header.
     * 
     * @param mmd           The user message meta data.
     * @throws IOException  When the information could not be written to disk.
     */
    @Override
    protected void writeUserMessageInfoToFile(MessageMetaData mmd) throws IOException {
        OMFactory   f = OMAbstractFactory.getOMFactory();
        OMElement    container = f.createOMElement(XML_ROOT_NAME);
        container.declareNamespace(Constants.EBMS3_NS_URI, "eb");
            
        log.debug("Add general message info to XML container");
        // Add the information on the user message to the container
        OMElement  usrMsgElement = UserMessage.createElement(container, mmd);
            
        log.debug("Add payload info to XML container");
        
        // We add the local file location as a Part property
        for (IPayload p : mmd.getPayloads()) {
            Property locationProp = new Property();
            locationProp.setName("org:holodeckb2b:location");
            locationProp.setValue(p.getContentLocation());
            ((PartInfo) p).getProperties().add(locationProp);
        }  
                    
        org.holodeckb2b.ebms3.packaging.PayloadInfo.createElement(usrMsgElement, mmd.getPayloads());
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
    protected void deliverSignalMessage(IMessageUnit sigMsgUnit) throws MessageDeliveryException {
        OMFactory   f = OMAbstractFactory.getOMFactory();
        OMElement    container = f.createOMElement(XML_ROOT_NAME);
        container.declareNamespace(Constants.EBMS3_NS_URI, "eb");
        
        if (sigMsgUnit instanceof IReceipt) {
            log.debug("Create facade to prevent content from inclusion in XML");
            org.holodeckb2b.ebms3.persistent.message.Receipt rcpt = new IReceiptImpl((IReceipt) sigMsgUnit);
            log.debug("Add receipt meta data to XML");
            Receipt.createElement(container, rcpt);
        } else if (sigMsgUnit instanceof IErrorMessage) {
            log.debug("Add error meta data to XML");
            ErrorSignal.createElement(container, (IErrorMessage) sigMsgUnit);            
        }
        
        log.debug("Added signal meta data to XML, write to disk");
        try {
            writeXMLDocument(container, sigMsgUnit.getMessageId());
            log.info("Signal message with msgID=" + sigMsgUnit.getMessageId() + " successfully delivered");
        } catch (IOException ex) {
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
    protected String writeXMLDocument(OMElement xml, String msgId) throws IOException {
        try {
            String msgFilePath = Utils.preventDuplicateFileName(directory + "mi-" 
                                                                + msgId.replaceAll("[^a-zA-Z0-9.-]", "_") 
                                                                + ".xml");

            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileWriter(msgFilePath));
            xml.serialize(writer);
            writer.close(); 
            
            return msgFilePath;
        } catch (Exception ex) {
            // Can not write the message info XML to file -> delivery not possible
            throw new IOException("Error writing message unit info to file!", ex);
        }
    } 
    
    /**
     * Private inner class to prevent receipt content to get included in XML.
     */
    private static class IReceiptImpl extends org.holodeckb2b.ebms3.persistent.message.Receipt {

        private IReceipt    source;
        
        public IReceiptImpl(IReceipt rcpt) {
            source = rcpt;
        }

        @Override
        public Date getTimestamp() {
            return source.getTimestamp();
        }

        @Override
        public String getMessageId() {
            return source.getMessageId();
        }

        @Override
        public String getRefToMessageId() {
            return source.getRefToMessageId();
        }

        @Override
        public ArrayList<OMElement> getContent() {
            return new ArrayList<>();
        }            
    }
    
    

}
