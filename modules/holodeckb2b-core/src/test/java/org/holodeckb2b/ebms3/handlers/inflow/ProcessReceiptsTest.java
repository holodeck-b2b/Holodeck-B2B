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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.ReceiptConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:07 15.03.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ProcessReceiptsTest {
    private static final QName RECEIPT_CHILD_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ProcessReceipts handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = ProcessReceiptsTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }
    @Before
    public void setUp() throws Exception {
        // Executed after org.holodeckb2b.ebms3.handlers.inflow.GetMessageUnitForPulling handler
        handler = new ProcessReceipts();
    }

    @After
    public void tearDown() throws Exception {    	
        core.getPModeSet().removeAll();
    }

    /**
     * Test the case when the message unit is present and is referenced in the receipt
     */
    @Test
    public void testDoProcessing() throws Exception {
        UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);

        String msgId = MessageIdUtils.createMessageId();
        String pmodeId = "pmodeid-01";
        userMessage.setPModeId(pmodeId);
        userMessage.setMessageId(msgId);

        pmode.setId(pmodeId);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        Leg leg = new Leg();
        ReceiptConfiguration receiptConfiguration = new ReceiptConfiguration();
        leg.setReceiptConfiguration(receiptConfiguration);
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(userMessage);

        storageManager.setProcessingState(userMessageEntity, ProcessingState.AWAITING_RECEIPT);

        // Setting input receipts property
        Receipt receipt = new Receipt();
        OMElement receiptChildElement = OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        receipt.setRefToMessageId(msgId);
        receipt.setMessageId("receipt_id");

        IReceiptEntity receiptEntity =
                storageManager.storeIncomingMessageUnit(receipt);
        ArrayList<IReceiptEntity> receiptEntities = new ArrayList<>();
        receiptEntities.add(receiptEntity);
        mc.setProperty(MessageContextProperties.IN_RECEIPTS, receiptEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertEquals(ProcessingState.DELIVERED, HolodeckB2BCoreInterface.getQueryManager()
        												.getMessageUnitsWithId(userMessage.getMessageId())
        												.iterator().next().getCurrentProcessingState().getState());        												
        assertEquals(ProcessingState.READY_FOR_DELIVERY, receiptEntity.getCurrentProcessingState().getState());        
    }

    /**
     * Test the case when there is no reference to message unit in the receipt
     */
    @Test
    public void testDoProcessingIfNoRefToMsgId() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        // Setting input receipts property
        Receipt receipt = new Receipt();
        OMElement receiptChildElement = OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        receipt.setMessageId("receipt_id");


        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        IReceiptEntity receiptEntity = storageManager.storeIncomingMessageUnit(receipt);
        ArrayList<IReceiptEntity> receiptEntities = new ArrayList<>();
        receiptEntities.add(receiptEntity);
        mc.setProperty(MessageContextProperties.IN_RECEIPTS, receiptEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
        assertEquals(ProcessingState.FAILURE, receiptEntity.getCurrentProcessingState().getState());
    }
}