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

import javax.xml.namespace.QName;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.pmode.Agreement;
import org.holodeckb2b.common.testhelpers.pmode.Leg;
import org.holodeckb2b.common.testhelpers.pmode.PMode;
import org.holodeckb2b.common.testhelpers.pmode.PartnerConfig;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
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

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
        HolodeckB2BCore.getPModeSet().removeAll();
    }

    /**
     * Test of User Message
     */
    @Test
    public void testDoProcessingOfUserMessage() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
        
        // Create matching P-Mode
        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.setId("matching-pmode");

        PartnerConfig initiator = new PartnerConfig();
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        pmode.setResponder(responder);

        Leg leg = new Leg();
        pmode.addLeg(leg);

        TradingPartner sender = userMessage.getSender();
        initiator.setRole(sender.getRole());
        initiator.setPartyIds(sender.getPartyIds());

        TradingPartner receiver = userMessage.getReceiver();
        responder.setRole(receiver.getRole());
        responder.setPartyIds(receiver.getPartyIds());

        AgreementReference agreementReference =
                userMessage.getCollaborationInfo().getAgreement();
        String agreementRefName = agreementReference.getName();
        String agreementRefType = agreementReference.getType();
        
        Agreement agreement = new Agreement();
        agreement.setName(agreementRefName);
        agreement.setType(agreementRefType);
        pmode.setAgreement(agreement);

        HolodeckB2BCore.getPModeSet().add(pmode);

        // Setting input message property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeIncomingMessageUnit(userMessage);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
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
        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.setId("t-error-pmode");
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
        IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(error);
        
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
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
        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.setId("t-receipt-pmode");
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
        IReceiptEntity rcptEntity = storageManager.storeIncomingMessageUnit(receipt);
        
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
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

       UserMessage userMessage = new UserMessage(TestUtils.getMMD("handlers/full_mmd.xml", this));
       userMessage.setPModeId(null);
       
       // Create matching P-Mode
       PMode pmode = new PMode();
       pmode.setMep(EbMSConstants.ONE_WAY_MEP);
       pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
       pmode.setId("not-matching-pmode");

       PartnerConfig initiator = new PartnerConfig();
       pmode.setInitiator(initiator);

       PartnerConfig responder = new PartnerConfig();
       pmode.setResponder(responder);

       Leg leg = new Leg();
       pmode.addLeg(leg);

       TradingPartner sender = userMessage.getSender();
       initiator.setRole(sender.getRole());
       initiator.setPartyIds(sender.getPartyIds());

       TradingPartner receiver = userMessage.getReceiver();
       responder.setRole("no-match");
       responder.setPartyIds(receiver.getPartyIds());

       AgreementReference agreementReference =
               userMessage.getCollaborationInfo().getAgreement();
       String agreementRefName = agreementReference.getName();
       String agreementRefType = agreementReference.getType();
       
       Agreement agreement = new Agreement();
       agreement.setName(agreementRefName);
       agreement.setType(agreementRefType);
       pmode.setAgreement(agreement);

       HolodeckB2BCore.getPModeSet().add(pmode);

       // Setting input message property
       StorageManager updateManager = HolodeckB2BCore.getStorageManager();
       IUserMessageEntity userMessageEntity = updateManager.storeIncomingMessageUnit(userMessage);
       
       MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
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