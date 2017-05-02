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
import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created at 23:18 29.01.17
 *
 * Checked for cases coverage (27.04.2017)
 *
 * We should test both use cases of the Description element
 * 1. inside Signal element
 * 2. inside PartInfo element
 * See DescriptionElement javadoc for details
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DescriptionElementTest {

    private static final QName DESCRIPTION_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Description");
    private static final QName LANG_ATTR_NAME =
            new QName("http://www.w3.org/XML/1998/namespace", "lang");
    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    private SOAPEnvelope soapEnvelope;
    private SOAPHeaderBlock headerBlock;
    private OMElement plElement; // PayloadInfo

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
    }

    @Test
    public void testCreateElementAsPartInfoChild() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        // Creating PayloadInfo element from mmd
        plElement = PayloadInfoElement.createElement(umElement, mmd.getPayloads());

        OMElement piElement = PartInfoElement.createElement(plElement, new Payload());
        Description description = new Description("some_text", "en-CA");
        OMElement dElement =
                DescriptionElement.createElement(piElement, description);
        assertNotNull(dElement);
        assertEquals(DESCRIPTION_ELEMENT_NAME, dElement.getQName());
        assertEquals("en-CA", dElement.getAttributeValue(LANG_ATTR_NAME));
        assertEquals("some_text", dElement.getText());
    }

    @Test
    public void testCreateElementAsSignalChild() throws Exception {
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);

        Description description = new Description("some_text", "en-CA");
        OMElement dElement = DescriptionElement.createElement(rElement, description);
        assertNotNull(dElement);
    }

    @Test
    public void testGetElementAsPartInfoChild() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        // Creating PayloadInfo element from mmd
        plElement = PayloadInfoElement.createElement(umElement, mmd.getPayloads());

        OMElement piElement = PartInfoElement.createElement(plElement, new Payload());
        Description description = new Description("some_text", "en-CA");
        DescriptionElement.createElement(piElement, description);

        OMElement dElement = DescriptionElement.getElement(piElement);
        assertNotNull(dElement);
        assertEquals(DESCRIPTION_ELEMENT_NAME, dElement.getQName());
        assertEquals("en-CA", dElement.getAttributeValue(LANG_ATTR_NAME));
        assertEquals("some_text", dElement.getText());
    }

    @Test
    public void testGetElementAsSignalChild() throws Exception {
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);
        Description description = new Description("some_text", "en-CA");
        DescriptionElement.createElement(rElement, description);

        OMElement dElement = DescriptionElement.getElement(rElement);
        assertEquals(DESCRIPTION_ELEMENT_NAME, dElement.getQName());
    }

    @Test
    public void testReadElementAsPartInfoChild() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        // Creating PayloadInfo element from mmd
        plElement = PayloadInfoElement.createElement(umElement, mmd.getPayloads());

        OMElement piElement = PartInfoElement.createElement(plElement, new Payload());
        Description description = new Description("some_text", "en-CA");
        OMElement dElement =
                DescriptionElement.createElement(piElement, description);

        description = DescriptionElement.readElement(dElement);
        assertNotNull(description);
        assertEquals("en-CA", description.getLanguage());
        assertEquals("some_text", description.getText());
    }

    @Test
    public void testReadElementAsSignalChild() throws Exception {
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);
        Description description = new Description("some_text", "en-CA");
        OMElement dElement = DescriptionElement.createElement(rElement, description);

        description = DescriptionElement.readElement(dElement);
        assertNotNull(description);
        assertEquals("en-CA", description.getLanguage());
        assertEquals("some_text", description.getText());
    }
}