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
package org.holodeckb2b.ebms3.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 23:23 29.01.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ReadUserMessageTest {

	private static final String T_MSG_ID = "test-msg-id@test.holodeck-b2b.org";
	
	private static final String SOAP_XML = 
			"<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\"\n" + 
			"    xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\"\n" + 
			"    xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\"\n" + 
			"    xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">\n" + 
			"    <soapenv:Header>       \n" + 
			"        <eb3:Messaging xmlns:mustUnderstand=\"http://www.w3.org/2003/05/soap-envelope\"\n" + 
			"            mustUnderstand:mustUnderstand=\"true\">\n" + 
			"            <eb3:UserMessage>\n" + 
			"                <eb3:MessageInfo>\n" + 
			"                    <eb3:Timestamp>2019-02-08T09:20:06.101Z</eb3:Timestamp>\n" + 
			"                    <eb3:MessageId>" + T_MSG_ID + "</eb3:MessageId>\n" + 
			"                </eb3:MessageInfo>\n" + 
			"                <eb3:PartyInfo>\n" + 
			"                    <eb3:From>\n" + 
			"                        <eb3:PartyId>org:holodeckb2b:example:company:A</eb3:PartyId>\n" + 
			"                        <eb3:Role>Sender</eb3:Role>\n" + 
			"                    </eb3:From>\n" + 
			"                    <eb3:To>\n" + 
			"                        <eb3:PartyId>org:holodeckb2b:example:company:B</eb3:PartyId>\n" + 
			"                        <eb3:Role>Receiver</eb3:Role>\n" + 
			"                    </eb3:To>\n" + 
			"                </eb3:PartyInfo>\n" + 
			"                <eb3:CollaborationInfo>\n" + 
			"                    <eb3:AgreementRef>http://agreements.holodeckb2b.org/examples/agreement0</eb3:AgreementRef>\n" + 
			"                    <eb3:Service type=\"org:holodeckb2b:services\">Test</eb3:Service>\n" + 
			"                    <eb3:Action>StoreMessage</eb3:Action>\n" + 
			"                    <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>\n" + 
			"                </eb3:CollaborationInfo>\n" + 
			"                <eb3:PayloadInfo>\n" + 
			"                    <eb3:PartInfo>\n" + 
			"                        <eb3:PartProperties>\n" + 
			"                            <eb3:Property name=\"original-file-name\">simple_document.xml</eb3:Property>\n" + 
			"                        </eb3:PartProperties>\n" + 
			"                    </eb3:PartInfo>\n" + 
			"                    <eb3:PartInfo href=\"cid:ff4d26fd-173e-445c-8f40-2ab96d72b3c7-1824330587@gecko.fritz.box\"/>\n" + 
			"                </eb3:PayloadInfo>\n" + 
			"            </eb3:UserMessage>\n" + 
			"        </eb3:Messaging>\n" + 
			"    </soapenv:Header>\n" + 
			"    <soapenv:Body>\n" + 
			"        <example-document>\n" + 
			"            <content>This is just a very simple XML document to show transport of XML payloads in\n" + 
			"                the SOAP body </content>\n" + 
			"        </example-document>\n" + 
			"    </soapenv:Body>\n" + 
			"</soapenv:Envelope>\n" + 
			""; 
			
    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Test
    public void testUserMessage() throws Exception {        
    	SOAPModelBuilder soapModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(new StringReader(SOAP_XML));
    	
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setEnvelope(soapModelBuilder.getSOAPEnvelope());

        MessageProcessingContext procCtx = new MessageProcessingContext(mc);
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new ReadUserMessage().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(procCtx.getReceivedUserMessage());
        assertEquals(T_MSG_ID, procCtx.getReceivedUserMessage().getMessageId());                
    }
}