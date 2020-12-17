/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 15:15 19.02.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessageInfoElementTest extends AbstractPackagingTest {

    private static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    private static final QName TIMESTAMP_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Timestamp");
    private static final QName MESSAGE_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");
    private static final QName REF_TO_MESSAGE_ID_ELEMENT_NAME =
    		new QName(EbMSConstants.EBMS3_NS_URI, "RefToMessageId");

    @Test
    public void testCreateElementNoRefTo() throws Exception {
    	// User Message without RefToMessageId
    	UserMessage userMessage = new UserMessage();
    	userMessage.setMessageId(UUID.randomUUID().toString());
    	userMessage.setTimestamp(new Date());
    	OMElement miElement = MessageInfoElement.createElement(createParent(), userMessage);
    	
    	assertNotNull(miElement);
    	assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());
    	
    	Iterator<OMElement> msgIdElems = miElement.getChildrenWithName(MESSAGE_ID_ELEMENT_NAME);
    	assertFalse(Utils.isNullOrEmpty(msgIdElems));
    	assertEquals(userMessage.getMessageId(), msgIdElems.next().getText());
    	Iterator<OMElement> timestampElems = miElement.getChildrenWithName(TIMESTAMP_ELEMENT_NAME);
    	assertFalse(Utils.isNullOrEmpty(timestampElems));    	
    	assertEquals(Utils.toXMLDateTime(userMessage.getTimestamp()), timestampElems.next().getText());
    	
    	assertTrue(Utils.isNullOrEmpty(miElement.getChildrenWithName(REF_TO_MESSAGE_ID_ELEMENT_NAME)));
    }
    
    @Test
    public void testCreateElementWithRefTo() throws Exception {
        // Receipt with RefToMessageId
        Receipt receipt = new Receipt();
        receipt.setMessageId(UUID.randomUUID().toString());
        receipt.setTimestamp(new Date());
        receipt.setRefToMessageId(UUID.randomUUID().toString());
    	OMElement miElement = MessageInfoElement.createElement(createParent(), receipt);
    	
    	assertNotNull(miElement);
    	assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());
    	
    	Iterator<OMElement> msgIdElems = miElement.getChildrenWithName(MESSAGE_ID_ELEMENT_NAME);
    	assertFalse(Utils.isNullOrEmpty(msgIdElems));
    	assertEquals(receipt.getMessageId(), msgIdElems.next().getText());
    	Iterator<OMElement> timestampElems = miElement.getChildrenWithName(TIMESTAMP_ELEMENT_NAME);
    	assertFalse(Utils.isNullOrEmpty(timestampElems));    	
    	assertEquals(Utils.toXMLDateTime(receipt.getTimestamp()), timestampElems.next().getText());
    	Iterator<OMElement> refToElems = miElement.getChildrenWithName(REF_TO_MESSAGE_ID_ELEMENT_NAME);
    	assertFalse(Utils.isNullOrEmpty(refToElems));    	
    	assertEquals(receipt.getRefToMessageId(), refToElems.next().getText());
    }

    @Test
    public void testGetElement() throws Exception {
	    OMElement msgInfoElement = MessageInfoElement.getElement(createXML(
        		"<parent>" +
        		"   <eb3:MessageInfo xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
        		"       <eb3:Timestamp>2019-08-05T10:50:09.192Z</eb3:Timestamp>\n" + 
        		"       <eb3:MessageId>8fba80a5-2a4b-42c4-9c8f-f53207d21673@gecko.fritz.box</eb3:MessageId>\n" + 
        		"   </eb3:MessageInfo>" +
				"</parent>"));
        assertNotNull(msgInfoElement);
        assertEquals(MESSAGE_INFO_ELEMENT_NAME, msgInfoElement.getQName());    	
    }

    @Test
    public void testReadElement() throws Exception {
    	String msgId = UUID.randomUUID().toString();
    	Date timestamp = new Date();
    	String refToMsgId = UUID.randomUUID().toString();
    	
    	UserMessage msgUnit = new UserMessage(); 
    	MessageInfoElement.readElement(createXML(
        		"<eb3:MessageInfo xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
        		"    <eb3:Timestamp>" + Utils.toXMLDateTime(timestamp) + "</eb3:Timestamp>\n" + 
        		"    <eb3:MessageId>" + msgId + "</eb3:MessageId>\n" + 
        		"    <eb3:RefToMessageId>" + refToMsgId + "</eb3:RefToMessageId>\n" + 
        		"</eb3:MessageInfo>"), msgUnit);
    	
    	assertEquals(msgId, msgUnit.getMessageId());
    	assertEquals(timestamp, msgUnit.getTimestamp());
    	assertEquals(refToMsgId, msgUnit.getRefToMessageId());
    }
}