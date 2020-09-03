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
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PartyId;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 13:14 15.10.16
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class UserMessageElementTest extends AbstractPackagingTest {

    private static final QName USER_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");
    private static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    private static final QName COLLABORATION_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "CollaborationInfo");
    private static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");
    private static final QName PAYLOAD_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");
    private static final QName PART_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");

    @Test
    public void testCreateElement() throws Exception {
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(UUID.randomUUID().toString());
        userMessage.setTimestamp(new Date());
        TradingPartner sender = new TradingPartner();
        sender.addPartyId(new PartyId("TheSender", null));
        sender.setRole("Sender");
        userMessage.setSender(sender);
        TradingPartner receiver = new TradingPartner();
        receiver.addPartyId(new PartyId("TheRecipient", null));
        receiver.setRole("Receiver");
        userMessage.setReceiver(receiver);
        CollaborationInfo collabInfo = new CollaborationInfo();
        collabInfo.setService(new Service("PackagingTest"));
        collabInfo.setAction("Create");
        userMessage.setCollaborationInfo(collabInfo);
    	userMessage.addMessageProperty(new Property("some-meta-data", "description"));
        Payload p = new Payload();
    	p.setPayloadURI("cid:as_attachment");
    	p.addProperty(new Property("p1", "v1"));
    	userMessage.addPayload(p);

    	OMElement umElement = UserMessageElement.createElement(createParent(), userMessage);
    	
    	assertNotNull(umElement);
        assertEquals(USER_MESSAGE_ELEMENT_NAME, umElement.getQName());
        
        assertNotNull(MessageInfoElement.getElement(umElement));
        assertNotNull(CollaborationInfoElement.getElement(umElement));
        assertNotNull(MessagePropertiesElement.getElement(umElement));
        assertNotNull(PayloadInfoElement.getElement(umElement));
    }

    @Test
    public void testGetElements() throws Exception {
    	Iterator<OMElement> umElements = UserMessageElement.getElements(createXML(
			"<eb3:Messaging xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">\n" + 
			"    <eb3:UserMessage>\n" +
			"		Normal contents of User Message would go here" +
			"    </eb3:UserMessage>\n" + 
			"    <eb3:UserMessage>\n" +
			"		Bundled User Message (NOTE: By default this is not allowed!" +
			"    </eb3:UserMessage>\n" + 
			"</eb3:Messaging>"));
    	
    	assertFalse(Utils.isNullOrEmpty(umElements));
    	assertEquals(USER_MESSAGE_ELEMENT_NAME, umElements.next().getQName());
    	assertTrue(umElements.hasNext());
    	assertEquals(USER_MESSAGE_ELEMENT_NAME, umElements.next().getQName());
    	assertFalse(umElements.hasNext());    	
    }

    @Test
    public void testReadElement() throws Exception {
    	UserMessage userMessage = UserMessageElement.readElement(createXML(
			"<eb3:UserMessage xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">\n" + 
			"    <eb3:MessageInfo>\n" + 
			"        <eb3:Timestamp>2019-08-05T10:50:09.192Z</eb3:Timestamp>\n" + 
			"        <eb3:MessageId>8fba80a5-2a4b-42c4-9c8f-f53207d21673@gecko.fritz.box</eb3:MessageId>\n" + 
			"    </eb3:MessageInfo>\n" + 
			"    <eb3:PartyInfo>\n" + 
			"        <eb3:From>\n" + 
			"            <eb3:PartyId>org:holodeckb2b:example:company:A</eb3:PartyId>\n" + 
			"            <eb3:Role>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator</eb3:Role>\n" + 
			"        </eb3:From>\n" + 
			"        <eb3:To>\n" + 
			"            <eb3:PartyId type=\"org:holodeckb2b:example:company\">partyb</eb3:PartyId>\n" + 
			"            <eb3:Role>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder</eb3:Role>\n" + 
			"        </eb3:To>\n" + 
			"    </eb3:PartyInfo>\n" + 
			"    <eb3:CollaborationInfo>\n" + 
			"        <eb3:AgreementRef>UnitTesting</eb3:AgreementRef>\n" + 
			"        <eb3:Service>PackagingTest</eb3:Service>\n" + 
			"        <eb3:Action>GetElements</eb3:Action>\n" + 
			"        <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>\n" + 
			"    </eb3:CollaborationInfo>\n" +
			"	 <eb3:MessageProperties>" +
			"        <eb3:Property name=\"additional-meta-data\">message description</eb3:Property>\n" + 			
			"	 </eb3:MessageProperties>" +	
			"    <eb3:PayloadInfo>\n" + 
			"        <eb3:PartInfo>\n" + 
			"            <eb3:PartProperties>\n" + 
			"                <eb3:Property name=\"original-file-name\"\n" + 
			"                    >simple_document.xml</eb3:Property>\n" + 
			"            </eb3:PartProperties>\n" + 
			"        </eb3:PartInfo>\n" + 
			"        <eb3:PartInfo\n" + 
			"            href=\"cid:8fba80a5-2a4b-42c4-9c8f-f53207d21673-142514300@gecko.fritz.box\"/>\n" + 
			"    </eb3:PayloadInfo>\n" + 
			"</eb3:UserMessage>"));
    	
    	assertNotNull(userMessage);
    	assertNotNull(userMessage.getMessageId());
    	assertNotNull(userMessage.getSender());
    	assertNotNull(userMessage.getReceiver());
    	assertNotNull(userMessage.getCollaborationInfo());
    	assertFalse(Utils.isNullOrEmpty(userMessage.getMessageProperties()));
    	assertFalse(Utils.isNullOrEmpty(userMessage.getPayloads()));
    }
}