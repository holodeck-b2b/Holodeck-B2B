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
package org.holodeckb2b.as4.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.receptionawareness.ReceiptCreatedEvent;
import org.holodeckb2b.ebms3.handlers.inflow.ReadUserMessage;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:04 13.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CreateReceiptTest {

	private static final String XML_START =
	"<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
	"    xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\"\n" +
	"    xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\"\n" +
	"    xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">\n" +
	"    <soapenv:Header>        \n";

	private static final String XML_WSS_HEADER =
	"<wsse:Security\n" +
	"    xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
	"    xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
	"    soapenv:mustUnderstand=\"true\">\n" +
	"    <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
	"        Id=\"SIG-4b284201872c695-368d-42e6-857b-3e952b59191e\">\n" +
	"        <ds:SignedInfo>\n" +
	"            <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
	"                <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\"\n" +
	"                    PrefixList=\"eb3 soapenv xsd xsi\"/>\n" +
	"            </ds:CanonicalizationMethod>\n" +
	"            <ds:SignatureMethod\n" +
	"                Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/>\n" +
	"            <ds:Reference URI=\"#id-4b284203e76351d-b8d2-438c-b438-fb3201c9e4d4\">\n" +
	"                <ds:Transforms>\n" +
	"                    <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
	"                        <ec:InclusiveNamespaces\n" +
	"                            xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\"\n" +
	"                            PrefixList=\"soapenv xsd xsi\"/>\n" +
	"                    </ds:Transform>\n" +
	"                </ds:Transforms>\n" +
	"                <ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>\n" +
	"                <ds:DigestValue>AFLaU+LoMTKFGP0ylIAXVf1ReccCa8/Zyx7sFESewa8=</ds:DigestValue>\n" +
	"            </ds:Reference>\n" +
	"            <ds:Reference URI=\"#id-4b28420ec025c03-68bd-4911-954c-57c4177d44f2\">\n" +
	"                <ds:Transforms>\n" +
	"                    <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
	"                        <ec:InclusiveNamespaces\n" +
	"                            xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\"\n" +
	"                            PrefixList=\"eb3 xsd xsi\"/>\n" +
	"                    </ds:Transform>\n" +
	"                </ds:Transforms>\n" +
	"                <ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>\n" +
	"                <ds:DigestValue>mQWfwfLt1x2KanqpCo13+puvYlRIeBooutsfRqfIO/M=</ds:DigestValue>\n" +
	"            </ds:Reference>\n" +
	"            <ds:Reference URI=\"cid:202c32b4-72a8-404e-af02-01e850133fe6-1963015097@gecko\">\n" +
	"                <ds:Transforms>\n" +
	"                    <ds:Transform\n" +
	"                        Algorithm=\"http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content-Signature-Transform\"\n" +
	"                    />\n" +
	"                </ds:Transforms>\n" +
	"                <ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>\n" +
	"                <ds:DigestValue>VKaXrPA/cBSUk2oqa+E4IeG1gOtXDh4zyHCHu3KA7eo=</ds:DigestValue>\n" +
	"            </ds:Reference>                    \n" +
	"        </ds:SignedInfo>\n" +
	"        <ds:SignatureValue>signature_digest_would_go_here</ds:SignatureValue>\n" +
	"        <ds:KeyInfo Id=\"KI-4b28420e08b2caf-e5c9-40a1-a768-0fac78debd0d\">\n" +
	"            <wsse:SecurityTokenReference\n" +
	"                wsu:Id=\"STR-4b28420b42216dd-4287-4341-a31c-76687d422cf9\">\n" +
	"                <wsse:KeyIdentifier\n" +
	"                    EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\"\n" +
	"                    ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\"\n" +
	"                    >Bw+QObPR+hkO8SrI6CK5zSakCN8=</wsse:KeyIdentifier>\n" +
	"            </wsse:SecurityTokenReference>\n" +
	"        </ds:KeyInfo>\n" +
	"    </ds:Signature>\n" +
	"</wsse:Security>";

	private static final String XML_END =
	"    <eb3:Messaging xmlns:mustUnderstand=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
	"        xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
	"        wsu:Id=\"id-4b284203e76351d-b8d2-438c-b438-fb3201c9e4d4\"\n" +
	"        mustUnderstand:mustUnderstand=\"true\">\n" +
	"        <eb3:UserMessage>\n" +
	"            <eb3:MessageInfo>\n" +
	"                <eb3:Timestamp>2019-06-19T13:55:45.432Z</eb3:Timestamp>\n" +
	"                <eb3:MessageId>202c32b4-72a8-404e-af02-01e850133fe6@gecko</eb3:MessageId>\n" +
	"            </eb3:MessageInfo>\n" +
	"            <eb3:PartyInfo>\n" +
	"                <eb3:From>\n" +
	"                    <eb3:PartyId>org:holodeckb2b:example:company:A</eb3:PartyId>\n" +
	"                    <eb3:Role>Sender</eb3:Role>\n" +
	"                </eb3:From>\n" +
	"                <eb3:To>\n" +
	"                    <eb3:PartyId>org:holodeckb2b:example:company:B</eb3:PartyId>\n" +
	"                    <eb3:Role>Receiver</eb3:Role>\n" +
	"                </eb3:To>\n" +
	"            </eb3:PartyInfo>\n" +
	"            <eb3:CollaborationInfo>\n" +
	"                <eb3:AgreementRef>http://agreements.holodeckb2b.org/examples/agreement0</eb3:AgreementRef>\n" +
	"                <eb3:Service type=\"org:holodeckb2b:services\">Test</eb3:Service>\n" +
	"                <eb3:Action>StoreMessage</eb3:Action>\n" +
	"                <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>\n" +
	"            </eb3:CollaborationInfo>\n" +
	"            <eb3:PayloadInfo>\n" +
	"                <eb3:PartInfo>\n" +
	"                    <eb3:PartProperties>\n" +
	"                        <eb3:Property name=\"original-file-name\"\n" +
	"                            >simple_document.xml</eb3:Property>\n" +
	"                    </eb3:PartProperties>\n" +
	"                </eb3:PartInfo>\n" +
	"                <eb3:PartInfo href=\"cid:202c32b4-72a8-404e-af02-01e850133fe6-1963015097@gecko\"/>\n" +
	"            </eb3:PayloadInfo>\n" +
	"        </eb3:UserMessage>\n" +
	"    </eb3:Messaging>\n" +
	"</soapenv:Header>\n" +
	"<soapenv:Body\n" +
	"    xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
	"    wsu:Id=\"id-4b28420ec025c03-68bd-4911-954c-57c4177d44f2\">\n" +
	"    <example-document>\n" +
	"        <content> This is just a very simple XML document to show transport of XML payloads in\n" +
	"            the SOAP body </content>\n" +
	"    </example-document>\n" +
	"</soapenv:Body>\n" +
	"</soapenv:Envelope>";

	private static final TestEventProcessor eventProcessor = new TestEventProcessor();

    @BeforeClass
    public static void setUpClass() throws Exception {
    	HolodeckB2BTestCore  core = new HolodeckB2BTestCore();
    	// Adding event processor to make sure the ReceiptCreatedEvent
        // is actually raised.
        core.setMessageProcessingEventProcessor(eventProcessor);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void clearEvents() {
    	eventProcessor.reset();
    }

    @Test
    public void testUnsignedMessage() throws Exception {
        // Prepare P-Mode
    	PMode pmode = TestUtils.create1WayReceivePushPMode();
        Leg leg = pmode.getLeg(Label.REQUEST);
        ReceiptConfiguration receiptConfig = new ReceiptConfiguration();
        receiptConfig.setPattern(ReplyPattern.RESPONSE);
        leg.setReceiptConfiguration(receiptConfig);
        HolodeckB2BCore.getPModeSet().add(pmode);

        // Creating a SOAP envelope with unsigned User Message
    	SOAPModelBuilder soapModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(
    																			new StringReader(XML_START + XML_END));

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);
        mc.setEnvelope(soapModelBuilder.getSOAPEnvelope());

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        new ReadUserMessage().invoke(mc);

        IUserMessageEntity userMessage = procCtx.getReceivedUserMessage();
        HolodeckB2BCore.getStorageManager().setPModeId(userMessage, pmode.getId());
        HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, ProcessingState.DELIVERED);

        try {
            Handler.InvocationResponse invokeResp = new CreateReceipt().invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(1, procCtx.getSendingReceipts().size());
        List<OMElement> content = procCtx.getSendingReceipts().iterator().next().getContent();
        assertEquals(1, content.size());
        assertEquals(UserMessageElement.Q_ELEMENT_NAME, content.get(0).getQName());

        assertEquals(1, eventProcessor.events.size());
        assertTrue(eventProcessor.events.get(0) instanceof ReceiptCreatedEvent);
    }

    @Test
    public void testSignedMessage() throws Exception {
    	// Prepare P-Mode
    	PMode pmode = TestUtils.create1WayReceivePushPMode();
    	Leg leg = pmode.getLeg(Label.REQUEST);
    	ReceiptConfiguration receiptConfig = new ReceiptConfiguration();
    	receiptConfig.setPattern(ReplyPattern.RESPONSE);
    	leg.setReceiptConfiguration(receiptConfig);
    	HolodeckB2BCore.getPModeSet().add(pmode);

    	// Creating a SOAP envelope with unsigned User Message
    	SOAPModelBuilder soapModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(
    			new StringReader(XML_START + XML_WSS_HEADER + XML_END));

    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);
    	mc.setServerSide(true);
    	mc.setEnvelope(soapModelBuilder.getSOAPEnvelope());

    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	new ReadUserMessage().invoke(mc);

    	IUserMessageEntity userMessage = procCtx.getReceivedUserMessage();
    	HolodeckB2BCore.getStorageManager().setPModeId(userMessage, pmode.getId());
    	HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, ProcessingState.DUPLICATE);

    	procCtx.addSecurityProcessingResult(mock(ISignatureProcessingResult.class));

    	try {
    		Handler.InvocationResponse invokeResp = new CreateReceipt().invoke(mc);
    		assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}

    	assertEquals(1, procCtx.getSendingReceipts().size());
    	List<OMElement> content = procCtx.getSendingReceipts().iterator().next().getContent();
    	assertEquals(1, content.size());
    	assertEquals(CreateReceipt.QNAME_NRI_ELEM, content.get(0).getQName());

    	assertEquals(1, eventProcessor.events.size());
    	assertTrue(eventProcessor.events.get(0) instanceof ReceiptCreatedEvent);
    }
}