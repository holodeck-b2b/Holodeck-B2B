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

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 13:14 15.10.16
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class UserMessageElementTest {

    private static final QName MESSAGING_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Messaging");
    private static final QName USER_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");
    private static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    private static final QName TIMESTAMP_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Timestamp");
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
        mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Creating SOAP envelope
        soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
    }

    @Test
    public void testCreateElement() throws Exception {
        UserMessageElement.createElement(headerBlock, mmd);

        // Check that soap header block of the envelope header contains user message
        SOAPHeader header = soapEnvelope.getHeader();
        OMElement messagingElement = header.getFirstElement();
        assertEquals(MESSAGING_ELEMENT_NAME, messagingElement.getQName());
        OMElement userMessageElement = messagingElement.getFirstElement();
        assertEquals(USER_MESSAGE_ELEMENT_NAME, userMessageElement.getQName());
        OMElement miElement = MessageInfoElement.getElement(userMessageElement);
        assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());
        Iterator it = miElement.getChildrenWithName(TIMESTAMP_ELEMENT_NAME);
        assertTrue(it.hasNext());
        it = miElement.getChildrenWithName(MESSAGE_ID_ELEMENT_NAME);
        assertTrue(it.hasNext());
        if(it.hasNext()) {
            OMElement idElement = (OMElement)it.next();
            assertEquals("n-soaDLzuliyRmzSlBe7", idElement.getText());
        }
        OMElement ciElement = CollaborationInfoElement.getElement(userMessageElement);
        assertEquals(COLLABORATION_INFO_ELEMENT_NAME, ciElement.getQName());
        OMElement arElement = AgreementRefElement.getElement(ciElement);
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, arElement.getQName());

        // Check the UserMessage for MessageProperties presence
        OMElement mpElement =
                MessagePropertiesElement.getElement(userMessageElement);
        it = mpElement.getChildElements();
        assertTrue(it.hasNext());
        if(it.hasNext()) {
            OMElement pElement = (OMElement)it.next();
            assertEquals("y1", pElement.getText());
            pElement = (OMElement)it.next();
            assertEquals("sWkOqek8-iNy_kNLcpS_jBiM.Q_", pElement.getText());
        }

        // Check the UserMessage for PayloadInfo properties presence
        OMElement piElement = PayloadInfoElement.getElement(userMessageElement);
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
        OMElement schema = SchemaElement.getElement(partInfoElem2);
        assertNotNull(schema);
        OMElement descr = DescriptionElement.getElement(partInfoElem2);
        assertNotNull(descr);
        OMElement partProps = PartPropertiesElement.getElement(partInfoElem2);
        assertNotNull(partProps);
    }

    @Test
    public void testGetElements() throws Exception {
        Iterator<OMElement> it = UserMessageElement.getElements(headerBlock);
        assertNotNull(it);
    }

    @Test
    public void testReadElement() throws Exception {
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        System.out.println("umElement: " + umElement);

        UserMessage userMessage =
                UserMessageElement.readElement(umElement);

        CollaborationInfo collaborationInfo = userMessage.getCollaborationInfo();
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
        assertEquals(IPayload.Containment.BODY, p1.getContainment());
        assertTrue(Utils.isNullOrEmpty(p1.getProperties()));
        org.holodeckb2b.interfaces.messagemodel.IPayload p2 = it.next();
        assertEquals(IPayload.Containment.EXTERNAL, p2.getContainment());
        assertFalse(Utils.isNullOrEmpty(p2.getProperties()));
    }
}