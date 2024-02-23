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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PartnerConfig;
import org.holodeckb2b.common.pmode.PartyId;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 23:22 29.01.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class FindPModesTest {

    static final QName RECEIPT_CHILD_ELEMENT_NAME = new QName("ReceiptChild");

    private static HolodeckB2BTestCore		testCore;

    @BeforeClass
    public static void setUpClass() throws Exception {
    	testCore = new HolodeckB2BTestCore();
        HolodeckB2BCoreInterface.setImplementation(testCore);
    }

    @After
    public void tearDown() throws Exception {
        testCore.cleanStorage();
        HolodeckB2BCore.getPModeSet().removeAll();
    }

    /**
     * Test of User Message
     */
    @Test
    public void testDoProcessingOfUserMessage() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(UUID.randomUUID().toString());
        userMessage.setTimestamp(new Date());
        TradingPartner sender = new TradingPartner();
        sender.addPartyId(new PartyId("TheSender", null));
        sender.setRole("Sender");
        userMessage.setSender(sender);
        TradingPartner receiver = new TradingPartner();
        receiver.addPartyId(new PartyId("TheRecipient", null));
        receiver.setRole("Receiver");
        userMessage.setReceiver(receiver);
        CollaborationInfo collabInfo = new CollaborationInfo();
        collabInfo.setService(new Service("FindPModeTest"));
        collabInfo.setAction("Match");
        userMessage.setCollaborationInfo(collabInfo);

        // Create matching P-Mode
        PMode pmode = HB2BTestUtils.create1WayReceivePMode();

        PartnerConfig initiator = new PartnerConfig();
        pmode.setInitiator(initiator);
        initiator.setRole(sender.getRole());
        sender.getPartyIds().forEach(pid -> initiator.addPartyId(new PartyId(pid)));

        PartnerConfig responder = new PartnerConfig();
        pmode.setResponder(responder);
        responder.setRole(receiver.getRole());
        receiver.getPartyIds().forEach(pid -> responder.addPartyId(new PartyId(pid)));

        HolodeckB2BCore.getPModeSet().add(pmode);

        // Setting input message property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeReceivedMessageUnit(userMessage);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new FindPModes().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(pmode.getId(), userMessageEntity.getPModeId());
    }

    @Test
    public void testDoProcessingOfErrorSignal() throws Exception {
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Create matching P-Mode
        PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
        HolodeckB2BCore.getPModeSet().add(pmode);

        // Setting input message property
        UserMessage usrMessage = new UserMessage();
        usrMessage.setMessageId(MessageIdUtils.createMessageId());
        usrMessage.setPModeId(pmode.getId());
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(usrMessage);

        // Initialising Error
        EbmsError ebmsError = new EbmsError();
        ebmsError.setRefToMessageInError(userMessageEntity.getMessageId());
        ErrorMessage error = new ErrorMessage(ebmsError);
        error.setMessageId(MessageIdUtils.createMessageId());

        // Setting input Error
        IErrorMessageEntity errorMessageEntity = storageManager.storeReceivedMessageUnit(error);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedError(errorMessageEntity);

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new FindPModes().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(userMessageEntity.getPModeId(), errorMessageEntity.getPModeId());
    }

    @Test
    public void testDoProcessingOfReceipt() throws Exception {
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Create matching P-Mode
        PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
        HolodeckB2BCore.getPModeSet().add(pmode);

        // Setting input message property
        UserMessage usrMessage = new UserMessage();
        usrMessage.setMessageId(MessageIdUtils.createMessageId());
        usrMessage.setPModeId(pmode.getId());
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(usrMessage);

        // Initialising Error
        Receipt receipt = new Receipt();
        receipt.setMessageId(MessageIdUtils.createMessageId());
        receipt.setRefToMessageId(userMessageEntity.getMessageId());

        // Setting input Receipt
        IReceiptEntity rcptEntity = storageManager.storeReceivedMessageUnit(receipt);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedReceipt(rcptEntity);

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new FindPModes().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(userMessageEntity.getPModeId(), rcptEntity.getPModeId());
    }

   /**
    * Test no matching P-Mode
    */
   @Test
   public void testNoPModeFound() throws Exception {
       MessageContext mc = new MessageContext();
       mc.setFLOW(MessageContext.IN_FLOW);

       UserMessage userMessage = new UserMessage();
       userMessage.setMessageId(UUID.randomUUID().toString());
       userMessage.setTimestamp(new Date());
       TradingPartner sender = new TradingPartner();
       sender.addPartyId(new PartyId("TheSender", null));
       sender.setRole("Sender");
       userMessage.setSender(sender);
       TradingPartner receiver = new TradingPartner();
       receiver.addPartyId(new PartyId("TheRecipient", null));
       receiver.setRole("Receiver");
       userMessage.setReceiver(receiver);
       CollaborationInfo collabInfo = new CollaborationInfo();
       collabInfo.setService(new Service("FindPModeTest"));
       collabInfo.setAction("Match");
       userMessage.setCollaborationInfo(collabInfo);

       // Create a non-matching P-Mode
       PMode pmode = HB2BTestUtils.create1WayReceivePMode();
       PartnerConfig initiator = new PartnerConfig();
       pmode.setInitiator(initiator);
       initiator.setRole(sender.getRole());
       sender.getPartyIds().forEach(pid -> initiator.addPartyId(new PartyId(pid)));

       PartnerConfig responder = new PartnerConfig();
       pmode.setResponder(responder);
       responder.setRole("dont-match");
       receiver.getPartyIds().forEach(pid -> responder.addPartyId(new PartyId(pid)));

       HolodeckB2BCore.getPModeSet().add(pmode);

       // Setting input message property
       StorageManager updateManager = HolodeckB2BCore.getStorageManager();
       IUserMessageEntity userMessageEntity = updateManager.storeReceivedMessageUnit(userMessage);

       IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
       procCtx.setUserMessage(userMessageEntity);

       try {
           assertEquals(Handler.InvocationResponse.CONTINUE, new FindPModes().invoke(mc));
       } catch (Exception e) {
           fail(e.getMessage());
       }

       assertNull(userMessageEntity.getPModeId());
       assertEquals(ProcessingState.FAILURE, userMessageEntity.getCurrentProcessingState().getState());
       assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
       assertTrue(procCtx.getGeneratedErrors().get(userMessageEntity.getMessageId()).size() == 1);
       assertEquals("EBMS:0010",
    		   	procCtx.getGeneratedErrors().get(userMessageEntity.getMessageId()).iterator().next().getErrorCode());
   }
}