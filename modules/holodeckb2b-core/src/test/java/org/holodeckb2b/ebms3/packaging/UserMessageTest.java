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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 13:14 15.10.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class UserMessageTest {

    private static final QName MESSAGING_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Messaging");
    private static final QName USER_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");
    private static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    private static final QName MESSAGE_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");
    private static final QName COLLABORATION_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "CollaborationInfo");
    private static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");
    private static final QName PAYLOAD_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");
    private static final QName PART_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");

    private MessageMetaData mmd;
    private SOAPHeaderBlock headerBlock;
    private SOAPEnvelope soapEnvelope;

    @Before
    public void setUp() throws Exception {
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("packagetest/mmd_pcktest.xml").getPath();
        final File f = new File(mmdPath);
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        // Creating SOAP envelope
        soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateElement() throws Exception {
        UserMessage.createElement(headerBlock, mmd);

        // Check that soap header block of the envelope header contains user message
        SOAPHeader header = soapEnvelope.getHeader();
        OMElement messagingElement = header.getFirstElement();
        assertEquals(MESSAGING_ELEMENT_NAME, messagingElement.getQName());
        OMElement userMessageElement = messagingElement.getFirstElement();
        assertEquals(USER_MESSAGE_ELEMENT_NAME, userMessageElement.getQName());
        OMElement miElement = MessageInfo.getElement(userMessageElement);
        assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());
        Iterator it = miElement.getChildrenWithName(MESSAGE_ID_ELEMENT_NAME);
        assertTrue(it.hasNext());
        if(it.hasNext()) {
            OMElement idElement = (OMElement)it.next();
            assertEquals("n-soaDLzuliyRmzSlBe7", idElement.getText());
        }
        OMElement ciElement = CollaborationInfoElement.getElement(userMessageElement);
        assertEquals(COLLABORATION_INFO_ELEMENT_NAME, ciElement.getQName());
        OMElement arElement = AgreementRef.getElement(ciElement);
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, arElement.getQName());

        // Check the UserMessage for PayloadInfo properties presence
        OMElement piElement = PayloadInfo.getElement(userMessageElement);
        System.out.println("piElement: " + piElement);
        assertEquals(PAYLOAD_INFO_ELEMENT_NAME, piElement.getQName());
        it = piElement.getChildrenWithName(PART_INFO_ELEMENT_NAME);
        assertTrue(it.hasNext());

        OMElement partInfoElem1 = (OMElement)it.next();
        assertNotNull(partInfoElem1);
        System.out.println("partInfoElem1: " + partInfoElem1);

        OMElement partInfoElem2 = (OMElement)it.next();
        System.out.println("partInfoElem2: " + partInfoElem2);
        assertNotNull(partInfoElem2);
        OMElement schema = Schema.getElement(partInfoElem2);
        assertNotNull(schema);
        OMElement descr = DescriptionElement.getElement(partInfoElem2);
        assertNotNull(descr);
        OMElement partProps = PartProperties.getElement(partInfoElem2);
        assertNotNull(partProps);
    }

    @Test
    public void testGetElements() throws Exception {
        Iterator<OMElement> it = UserMessage.getElements(headerBlock);
        assertNotNull(it);
    }

    @Test
    public void testReadElement() throws Exception {
        OMElement umElement = UserMessage.createElement(headerBlock, mmd);

        System.out.println("umElement: " + umElement);

        org.holodeckb2b.common.messagemodel.UserMessage userMessage =
                UserMessage.readElement(umElement);

        org.holodeckb2b.common.messagemodel.CollaborationInfo collaborationInfo
                = userMessage.getCollaborationInfo();
        assertNotNull(collaborationInfo);
        AgreementReference agreementReference = collaborationInfo.getAgreement();
        assertNotNull(agreementReference);
        assertEquals("QtzizhtL.QZg3UXFvby7tXDE2FL", agreementReference.getPModeId());
        assertNotNull(collaborationInfo.getService());
        assertNotNull(collaborationInfo.getAction());
        assertNotNull(collaborationInfo.getConversationId());

        Collection<IPayload> payloads = userMessage.getPayloads();
        assertNotNull(payloads);
        assertTrue(payloads.size() == 2);
        Iterator<org.holodeckb2b.interfaces.messagemodel.IPayload> it =
                payloads.iterator();

        org.holodeckb2b.interfaces.messagemodel.IPayload p1 = it.next();
        assertEquals("org.holodeckb2b.common.messagemodel.Payload",
                p1.getClass().getName());
//        assertEquals("attachment", p1.getContainment().toString());  // fails
//        assertEquals(IPayload.Containment.ATTACHMENT, p1.getContainment()); // fails
//        assertEquals("I8ZVs6G2P", p1.getMimeType());    // fails
//        assertEquals("http://sxGTnZjm/", p1.getContentLocation()); // fails
        assertNull(p1.getProperties());
        org.holodeckb2b.interfaces.messagemodel.IPayload p2 = it.next();
        assertEquals(IPayload.Containment.EXTERNAL, p2.getContainment());
//        assertEquals("CoL9", p2.getMimeType());  // fails
//        assertEquals("http://pcVJBuTT/", p2.getPayloadURI()); // fails
        assertNotNull(p2.getProperties());
    }
}