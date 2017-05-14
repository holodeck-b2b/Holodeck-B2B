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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created at 23:21 29.01.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ReadReceiptTest {

    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    private static String baseDir;

    private ReadReceipt handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = ReadReceiptTest.class.getClassLoader()
                .getResource("handlers").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));
    }

    @Before
    public void setUp() throws Exception {
        handler = new ReadReceipt();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        // Setting input receipts property
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                env.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        receipt.setRefToMessageId("some_msg_id");
        receipt.setMessageId("receipt_id");

        ReceiptElement.createElement(headerBlock, receipt);

        assertNull(mc.getProperty(MessageContextProperties.IN_RECEIPTS));

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.IN_RECEIPTS));
    }
}