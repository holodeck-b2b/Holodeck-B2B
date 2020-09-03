/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.as4.multihop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.PartyId;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Test if correct ebint:RoutingInput element is created
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class RoutingInputTest {

	@Test
    public void testFullUserMessageHeader() {
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
        collabInfo.setService(new Service("MultiHopTest"));
        collabInfo.setAction("CreateRoutingInput");
        userMessage.setCollaborationInfo(collabInfo);
        
        // Create a SOAP envelope that should contain the RoutingInput element
        final SOAPEnvelope    env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);

        final OMElement ri = RoutingInput.createElement(env, userMessage);

        assertEquals(MultiHopConstants.ROUTING_INPUT_NS_URI, ri.getNamespaceURI());
        final Iterator<?> umChilds = ri.getChildrenWithLocalName("UserMessage");
        assertTrue(umChilds.hasNext());
        final OMElement umChild = (OMElement) umChilds.next();
        assertEquals(MultiHopConstants.ROUTING_INPUT_NS_URI, umChild.getNamespaceURI());

        final Iterator<?> ciChilds = umChild.getChildrenWithLocalName("CollaborationInfo");
        assertTrue(ciChilds.hasNext());
        final OMElement ciChild = (OMElement) ciChilds.next();
        assertEquals(EbMSConstants.EBMS3_NS_URI, ciChild.getNamespaceURI());
    }
}
