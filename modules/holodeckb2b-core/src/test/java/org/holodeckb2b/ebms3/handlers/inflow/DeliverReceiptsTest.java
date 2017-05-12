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
import org.holodeckb2b.common.messagemodel.UserMessage;
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
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.DeliverySpecification;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.ReceiptConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created at 12:05 15.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DeliverReceiptsTest {
    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ProcessReceipts processReceiptsHandler;

    private DeliverReceipts handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = TestUtils.getPath(DeliverReceiptsTest.class, "handlers");
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }
    @Before
    public void setUp() throws Exception {
        processReceiptsHandler = new ProcessReceipts();
        // Executed after org.holodeckb2b.ebms3.handlers.inflow.ProcessReceipts handler
        handler = new DeliverReceipts();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);

        String msgId = "org.holodeckb2b.ebms3.handlers.inflow.ProcessReceiptsTest_01";
        UserMessage userMessage = UserMessageElement.readElement(umElement);

        String pmodeId =
                userMessage.getCollaborationInfo().getAgreement().getPModeId();
        userMessage.setPModeId(pmodeId);
        userMessage.setMessageId(msgId);

        pmode.setId(pmodeId);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        Leg leg = new Leg();
        ReceiptConfiguration receiptConfig = new ReceiptConfiguration();
        receiptConfig.setNotifyReceiptToBusinessApplication(true);
        DeliverySpecification deliverySpec = new DeliverySpecification();
        deliverySpec.setId("some_delivery_spec_01");
        receiptConfig.setReceiptDelivery(deliverySpec);
        leg.setReceiptConfiguration(receiptConfig);

        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        StorageManager storageManager = core.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        storageManager.setProcessingState(userMessageEntity, ProcessingState.AWAITING_RECEIPT);

        // Setting input receipts property
        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                env.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        receipt.setRefToMessageId(msgId);
        receipt.setMessageId("receipt_id");
        receipt.setPModeId(pmodeId);

        ReceiptElement.createElement(headerBlock, receipt);

        IReceiptEntity receiptEntity =
                storageManager.storeIncomingMessageUnit(receipt);
        ArrayList<IReceiptEntity> receiptEntities = new ArrayList<>();
        receiptEntities.add(receiptEntity);
        mc.setProperty(MessageContextProperties.IN_RECEIPTS, receiptEntities);

        assertEquals(ProcessingState.RECEIVED,
                receiptEntity.getCurrentProcessingState().getState());

        try {
            Handler.InvocationResponse invokeResp = processReceiptsHandler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.READY_FOR_DELIVERY,
                receiptEntity.getCurrentProcessingState().getState());

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.DONE,
                receiptEntity.getCurrentProcessingState().getState());
    }
}