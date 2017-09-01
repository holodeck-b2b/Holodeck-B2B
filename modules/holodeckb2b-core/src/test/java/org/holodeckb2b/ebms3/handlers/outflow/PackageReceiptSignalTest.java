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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 15:45 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PackageReceiptSignalTest {

    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private PackageReceiptSignal handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PackageReceiptSignalTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PackageReceiptSignal();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testDoProcessing() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                env.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);

        ReceiptElement.createElement(headerBlock, receipt);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IReceiptEntity receiptEntity =
                updateManager.storeIncomingMessageUnit(receipt);
        ArrayList<IReceiptEntity> receiptEntities = new ArrayList<>();
        receiptEntities.add(receiptEntity);
        mc.setProperty(MessageContextProperties.OUT_RECEIPTS, receiptEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        HashMap<String, Boolean> messages = new HashMap<>();
        messages.put("Adding receipt signal(s) to the message", false);
        messages.put("Get the eb:Messaging header from the message", false);
        messages.put("Add eb:SignalMessage element to the existing eb:Messaging header", false);
        messages.put("eb:SignalMessage element succesfully added to header", false);
        Set<String> keys = messages.keySet();
        for(LoggingEvent e : events) {
            if(e.getLevel().equals(Level.DEBUG)) {
                String key = e.getRenderedMessage();
                if(keys.contains(key)) {
                    messages.put(key, true);
                }
            }
        }
        boolean containsAllMessages = true;
        for(Boolean flag : messages.values()) {
            containsAllMessages &= flag;
        }
        assertTrue(containsAllMessages);
    }
}