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
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
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

    static final QName MESSAGING_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Messaging");
    static final QName USER_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");
    static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    static final QName MESSAGE_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");
    static final QName COLLABORATION_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "CollaborationInfo");
    static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");
    static final QName PAYLOAD_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");
    static final QName PART_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateElement() throws Exception {
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("multihop/icloud/full_mmd.xml").getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        //System.out.println("Payloads:");

        // Check the mmd for PayloadInfo properties presence

        Collection<IPayload> payloadCollection = mmd.getPayloads();
        for(IPayload payload : payloadCollection) {
            //System.out.println("payload: " + payload.getContainment().toString());
            if(payload.getContainment().toString().equalsIgnoreCase("EXTERNAL")) {
                //System.out.println("here!!!");
                Collection<IProperty> properties = payload.getProperties();
                assertTrue(properties.size() == 2);
                assertNotNull(properties);
                Iterator<IProperty> pIt = properties.iterator();
                IProperty p1 = pIt.next();
                assertEquals("ozJjzK1OZJEF", p1.getName());
                IProperty p2 = pIt.next();
                assertEquals("VCHWFAqaEiadKr2F-", p2.getName());
            }
        }

        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement userMessage = UserMessage.createElement(headerBlock, mmd);
        System.out.println("userMessage: " + userMessage);
        // Check that soap header block of the envelope header contains user message
        SOAPHeader header = env.getHeader();
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
        OMElement ciElement = CollaborationInfo.getElement(userMessageElement);
        assertEquals(COLLABORATION_INFO_ELEMENT_NAME, ciElement.getQName());
        OMElement arElement = AgreementRef.getElement(ciElement);
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, arElement.getQName());

        // Check the UserMessage for PayloadInfo properties presence

        OMElement piElement = PayloadInfo.getElement(userMessageElement);
        assertEquals(PAYLOAD_INFO_ELEMENT_NAME, piElement.getQName());
        it = piElement.getChildrenWithName(PART_INFO_ELEMENT_NAME);
        assertTrue(it.hasNext());

        OMElement partInfoElem1 = (OMElement)it.next();
        assertNotNull(partInfoElem1);
        OMElement partInfoElem2 = (OMElement)it.next();
        assertNotNull(partInfoElem2);
        OMElement schema = Schema.getElement(partInfoElem2);
        assertNotNull(schema);
        OMElement descr = Description.getElement(partInfoElem2);
        assertNotNull(descr);
        OMElement partProps = PartProperties.getElement(partInfoElem2);
        assertNotNull(partProps); // PartProperties element should be present but it is missed!!!
    }

    @Test
    public void testGetElements() throws Exception {

    }

    @Test
    public void testReadElement() throws Exception {

    }
}