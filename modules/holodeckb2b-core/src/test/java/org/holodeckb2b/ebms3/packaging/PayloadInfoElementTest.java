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
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 17:41 29.01.17
 *
 * Checked for cases coverage (27.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PayloadInfoElementTest {

    private static final QName PAYLOAD_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");
    private static final QName PART_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");

    private MessageMetaData mmd;
    private OMElement umElement;

    @Before
    public void setUp() throws Exception {
        mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);
        // Adding UserMessage from mmd
        umElement = UserMessageElement.createElement(headerBlock, mmd);
    }

    @Test
    public void testCreateElement() throws Exception {
        OMElement piElement =
                PayloadInfoElement.createElement(umElement, mmd.getPayloads());
        System.out.println("piElement: " + piElement);
        checkContainsPayloads(piElement);
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement piElement = PayloadInfoElement.getElement(umElement);
        System.out.println("piElement: " + piElement);
        checkContainsPayloads(piElement);
    }

    @Test
    public void testReadElement() throws Exception {
        OMElement piElement =
                PayloadInfoElement.createElement(umElement, mmd.getPayloads());
        System.out.println("piElement: " + piElement);
        checkContainsPayloads(piElement);
        Collection<IPayload> payloads = PayloadInfoElement.readElement(piElement);
        checkPayloads(payloads);
    }

    /**
     * Checks that PartInfo element contains expected data
     * @param piElement
     */
    private void checkContainsPayloads(OMElement piElement) {
        assertNotNull(piElement);
        assertEquals(PAYLOAD_INFO_ELEMENT_NAME, piElement.getQName());
        Iterator it = piElement.getChildrenWithName(PART_INFO_ELEMENT_NAME);
        assertTrue(it.hasNext());
        OMElement partInfoElem1 = (OMElement)it.next();
        assertNotNull(partInfoElem1);
        OMElement partInfoElem2 = (OMElement)it.next();
        assertNotNull(partInfoElem2);
        OMElement schema = SchemaElement.getElement(partInfoElem2);
        assertNotNull(schema);
        OMElement descr = DescriptionElement.getElement(partInfoElem2);
        assertNotNull(descr);
        OMElement partProps = PartPropertiesElement.getElement(partInfoElem2);
        assertNotNull(partProps);
    }

    /**
     * Checks the payloads data
     * @param payloads
     */
    private void checkPayloads(Collection<IPayload> payloads) {
        Iterator<IPayload> payloadsIt = payloads.iterator();
        assertTrue(payloadsIt.hasNext());
        IPayload p = payloadsIt.next();
        assertEquals(IPayload.Containment.BODY, p.getContainment());
        // createElement() method does not fully initialize
        // the containment&mimeType&location payload attributes
        // and it should not do this, because those attributes are used internally
        // assertEquals(IPayload.Containment.ATTACHMENT, p.getContainment()); // should fail
        // assertEquals("I8ZVs6G2P", p.getMimeType()); // should fail
        // assertEquals("http://sxGTnZjm/", p.getContentLocation()); // should fail
        assertTrue(payloadsIt.hasNext());
        p = payloadsIt.next();
        assertEquals(IPayload.Containment.EXTERNAL, p.getContainment());
        // assertEquals("CoL9", p.getMimeType()); // should fail
        // assertEquals("http://pcVJBuTT/", p.getPayloadURI()); // should fail
        assertNotNull(p.getSchemaReference());
        assertNotNull(p.getDescription());
        assertNotNull(p.getProperties());
    }
}