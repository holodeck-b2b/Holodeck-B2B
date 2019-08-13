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
package org.holodeckb2b.ebms3.packaging;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PartyId;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.packaging.SOAPEnv.SOAPVersion;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests schema validaty of created XML
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PackagingTest {

	private static File ebMSschemaFile;
	private static File SOAP11schemaFile;
	private static File SOAP12schemaFile;
	private static File xmlSchemaFile;

	@BeforeClass
	public static void loadSchemaFiles() {
		ebMSschemaFile = new File(TestUtils.getPath("xsd/ebms-header-3_0-200704_refactored.xsd"));
		SOAP11schemaFile = new File(TestUtils.getPath("xsd/soap11-envelope.xsd"));
		SOAP12schemaFile = new File(TestUtils.getPath("xsd/soap12-envelope.xsd"));
		xmlSchemaFile = new File(TestUtils.getPath("xsd/xml.xsd"));
	}

	@Test
	public void testUserMessage() {
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

		SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPVersion.SOAP_12);
		UserMessageElement.createElement(Messaging.createElement(env), userMessage);

		assertValidXML(env);
	}
	
	@Test
	public void testReceipt() {
        Receipt receipt = new Receipt();
        receipt.setMessageId(UUID.randomUUID().toString());
		receipt.setTimestamp(new Date());
		receipt.setRefToMessageId(UUID.randomUUID().toString());
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(OMAbstractFactory.getOMFactory().createOMElement(
        														new QName("http://just.for.a.test", "ReceiptContent")));
        receipt.setContent(content);
        
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPVersion.SOAP_12);
		ReceiptElement.createElement(Messaging.createElement(env), receipt);
		
		assertValidXML(env);
	}
	
	@Test
	public void testError() {
		ErrorMessage errorMsg = new ErrorMessage();
		errorMsg.setMessageId(UUID.randomUUID().toString());
		errorMsg.setTimestamp(new Date());
		errorMsg.setRefToMessageId(UUID.randomUUID().toString());		
		errorMsg.addError(new OtherContentError("Just a test"));
		
		SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPVersion.SOAP_12);
		ErrorSignalElement.createElement(Messaging.createElement(env), errorMsg);
		
		assertValidXML(env);
	}
	

	/*
	 * Helper to assert that the generated XML document is valid according to XML
	 * Schema of the ebMS Spec
	 */
	private void assertValidXML(final SOAPEnvelope env) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			env.serialize(baos);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(true);
			SAXParser parser = factory.newSAXParser();
			parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
					"http://www.w3.org/2001/XMLSchema");
			parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource",
					new File[] { xmlSchemaFile, SOAP11schemaFile, SOAP12schemaFile, ebMSschemaFile });
			ErrorCollector errorCollector = new ErrorCollector();
			parser.parse(new ByteArrayInputStream(baos.toByteArray()), errorCollector);
			
			if (!errorCollector.errors.isEmpty()) {
				errorCollector.printErrors();
				fail();
			}
		} catch (Exception ex) {
			fail();
		}
	}

	class ErrorCollector extends DefaultHandler {
		private MessageFormat message = new MessageFormat("({0}: {1}, {2}): {3}");

		ArrayList<SAXParseException> errors = new ArrayList<>();
		
		void printErrors() {
			errors.forEach(e -> print(e));
		}
		
		private void print(SAXParseException x) {
			String msg = message.format(new Object[] { x.getSystemId(), new Integer(x.getLineNumber()),
					new Integer(x.getColumnNumber()), x.getMessage() });
			System.out.println(msg);
		}

		@Override
		public void warning(SAXParseException x) {
			// ignore warnings
		}

		@Override
		public void error(SAXParseException x) {
			errors.add(x);			
		}

		@Override
		public void fatalError(SAXParseException x) throws SAXParseException {
			errors.add(x);			
		}
	}
}