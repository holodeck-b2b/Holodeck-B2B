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
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created at 15:17 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ReceiptElementTest {

    private static final QName SIGNAL_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "SignalMessage");
    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    private SOAPEnvelope soapEnvelope;
    private SOAPHeaderBlock headerBlock;

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
    }

    @Test
    public void testCreateElement() throws Exception {
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);
        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, rElement.getQName());
    }

    @Test
    public void testReadElement() throws Exception {
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);

        receipt = ReceiptElement.readElement(rElement);
        assertEquals(RECEIPT_CHILD_ELEMENT_NAME,
                receipt.getContent().iterator().next().getQName());
    }

    @Test
    public void testGetElements() throws Exception {
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        ReceiptElement.createElement(headerBlock, receipt);

        Iterator<OMElement> it = ReceiptElement.getElements(headerBlock);
        assertTrue(it.hasNext());
        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, it.next().getQName());
    }
}