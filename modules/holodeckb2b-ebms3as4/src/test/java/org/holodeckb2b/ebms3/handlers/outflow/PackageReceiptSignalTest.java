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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:45 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PackageReceiptSignalTest {

    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Test
    public void testDoProcessing() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock messaging = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        
        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        
        Receipt rcpt1 = new Receipt();
        rcpt1.setMessageId(MessageIdUtils.createMessageId());
        rcpt1.addElementToContent(OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME));
        procCtx.addSendingReceipt(updateManager.storeOutGoingMessageUnit(rcpt1));

        Receipt rcpt2 = new Receipt();
        rcpt2.setMessageId(MessageIdUtils.createMessageId());
        rcpt2.addElementToContent(OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME));
        procCtx.addSendingReceipt(updateManager.storeOutGoingMessageUnit(rcpt2));

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new PackageReceiptSignal().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        Iterator<OMElement> signals = messaging.getChildElements();
        assertTrue(signals.hasNext());
        OMElement signalElem = signals.next();
        assertEquals("SignalMessage", signalElem.getLocalName());
        assertEquals(EbMSConstants.EBMS3_NS_URI, signalElem.getNamespaceURI());
        assertNotNull(signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "Receipt")));
        OMElement msgInfoElem = signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo"));
        assertNotNull(msgInfoElem);
        assertEquals(rcpt1.getMessageId(), 
        			 msgInfoElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageId")).getText());
            
        assertTrue(signals.hasNext());
        signalElem = signals.next();
        assertEquals("SignalMessage", signalElem.getLocalName());
        assertEquals(EbMSConstants.EBMS3_NS_URI, signalElem.getNamespaceURI());
        assertNotNull(signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "Receipt")));
        msgInfoElem = signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo"));
        assertNotNull(msgInfoElem);
        assertEquals(rcpt2.getMessageId(), 
        		msgInfoElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageId")).getText());
        
    }
}