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
package org.holodeckb2b.core.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.DeliveryConfiguration;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod.NullDeliverer;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:05 15.03.17
 *
 * Checked for cases coverage (24.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DeliverReceiptsTest {
    private static final QName RECEIPT_CHILD_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

   	@BeforeClass
       public static void setUpClass() throws Exception {
           HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
       }

       @After
       public void tearDown() throws Exception {
           TestUtils.cleanOldMessageUnitEntities();
           HolodeckB2BCoreInterface.getPModeSet().removeAll();
       }

       @Test
       public void testDoProcessing() throws Exception {
    	   PMode pmode = TestUtils.create1WaySendPushPMode();        
           Leg leg = pmode.getLeg(Label.REQUEST);
           
           DeliveryConfiguration deliverySpecification = new DeliveryConfiguration();
           deliverySpecification.setFactory(NullDeliveryMethod.class.getName());
           deliverySpecification.setId("delivery_spec_id");
           leg.setDefaultDelivery(deliverySpecification);
           
           ReceiptConfiguration rcptCfg = new ReceiptConfiguration();
           rcptCfg.setNotifyReceiptToBusinessApplication(true);
           leg.setReceiptConfiguration(rcptCfg);
           
           HolodeckB2BCore.getPModeSet().add(pmode);

           UserMessage userMessage = new UserMessage();
           userMessage.setMessageId(MessageIdUtils.createMessageId());
           userMessage.setPModeId(pmode.getId());
           
           MessageContext mc = new MessageContext();
           mc.setFLOW(MessageContext.IN_FLOW);

           Receipt receipt = new Receipt();
           OMElement receiptChildElement = OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
           ArrayList<OMElement> content = new ArrayList<>();
           content.add(receiptChildElement);
           receipt.setContent(content);
           receipt.setMessageId(MessageIdUtils.createMessageId());
           receipt.setPModeId(pmode.getId());
           receipt.setRefToMessageId(userMessage.getMessageId());

           StorageManager storageManager = HolodeckB2BCore.getStorageManager();
           IMessageUnitEntity umEntity = storageManager.storeOutGoingMessageUnit(userMessage);        
           
           IReceiptEntity rcptMessageEntity = storageManager.storeIncomingMessageUnit(receipt);        
           storageManager.setProcessingState(rcptMessageEntity, ProcessingState.READY_FOR_DELIVERY);
           
           IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
           procCtx.addReceivedReceipt(rcptMessageEntity);        
           
           try {
               assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverReceipts().invoke(mc));
           } catch (Exception e) {
               fail(e.getMessage());
           }

           assertTrue(((NullDeliverer) HolodeckB2BCore.getMessageDeliverer(deliverySpecification))
        		   														  .wasDelivered(receipt.getMessageId()));
           assertEquals(ProcessingState.DONE, rcptMessageEntity.getCurrentProcessingState().getState());
       }
}