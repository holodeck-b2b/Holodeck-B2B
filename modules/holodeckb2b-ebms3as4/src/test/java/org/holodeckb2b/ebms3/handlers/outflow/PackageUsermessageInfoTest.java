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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.PartyId;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:44 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PackageUsermessageInfoTest {

    @BeforeClass
    public static void setUpClass() throws Exception {       
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Test
    public void testDoProcessing() throws Exception {
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
    	
    	// Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock messaging = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeReceivedMessageUnit(userMessage);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new PackageUsermessageInfo().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertTrue(messaging.getChildElements().hasNext());
        OMElement signalElem = (OMElement) messaging.getChildElements().next();
        assertEquals("UserMessage", signalElem.getLocalName());
        assertEquals(EbMSConstants.EBMS3_NS_URI, signalElem.getNamespaceURI());
        assertNotNull(signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "PartyInfo")));
        OMElement msgInfoElem = signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo"));
        assertNotNull(msgInfoElem);
        assertEquals(userMessage.getMessageId(), 
        			 msgInfoElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageId")).getText());
    }
}