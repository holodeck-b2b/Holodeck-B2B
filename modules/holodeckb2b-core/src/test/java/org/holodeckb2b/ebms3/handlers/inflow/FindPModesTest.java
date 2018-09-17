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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.Agreement;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.PartnerConfig;
import org.junit.After;
import org.junit.Before;
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

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private FindPModes handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = FindPModesTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new FindPModes();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
        core.getPModeSet().removeAll();
    }

    /**
     *
     */
    @Test
    public void testDoProcessingOfUserMessage() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        PartnerConfig initiator = new PartnerConfig();
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        pmode.setResponder(responder);

        Leg leg = new Leg();
        pmode.addLeg(leg);

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);

        String msgId = userMessage.getMessageId();

        TradingPartner sender = userMessage.getSender();
        initiator.setRole(sender.getRole());
        initiator.setPartyIds(sender.getPartyIds());

        TradingPartner receiver = userMessage.getReceiver();
        responder.setRole(receiver.getRole());
        responder.setPartyIds(receiver.getPartyIds());

        AgreementReference agreementReference =
                userMessage.getCollaborationInfo().getAgreement();
        String pmodeId = agreementReference.getPModeId();
        String agreementRefName = agreementReference.getName();
        String agreementRefType = agreementReference.getType();

        pmode.setId(pmodeId);

        Agreement agreement = new Agreement();
        agreement.setName(agreementRefName);
        agreement.setType(agreementRefType);
        pmode.setAgreement(agreement);

        core.getPModeSet().add(pmode);

        // Setting input message property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        System.out.println("um: " + updateManager.getClass());
        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testDoProcessingOfErrorSignal() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
  
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        // Setting input message property
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(mmd);

        String pmodeId = userMessageEntity.getPModeId();

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        Leg leg = new Leg();
        pmode.addLeg(leg);
        pmode.setId(pmodeId);

        core.getPModeSet().add(pmode);

        // Initialising Errors
        String errMsgId = "some_err_message_id";
        ErrorMessage error = new ErrorMessage();
        error.setMessageId(errMsgId);
        error.setTimestamp(new Date());
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setRefToMessageInError(userMessageEntity.getMessageId());
        errors.add(ebmsError);
        error.setErrors(errors);

        // Setting input Receipt property
        IErrorMessageEntity errorMessageEntity =
                storageManager.storeIncomingMessageUnit(error);
        System.out.println("errors: " + errorMessageEntity.getErrors());
        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setProperty(MessageContextProperties.IN_ERRORS, errorMessageEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log handler found pmode
        assertEquals(pmodeId, errorMessageEntity.getPModeId());
    }

    @Test
    public void testDoProcessingOfReceipt() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        Leg leg = new Leg();
        pmode.addLeg(leg);

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);

        String msgId = userMessage.getMessageId();

        AgreementReference agreementReference =
                userMessage.getCollaborationInfo().getAgreement();
        String pmodeId = agreementReference.getPModeId();

        pmode.setId(pmodeId);

        core.getPModeSet().add(pmode);

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input message property
        System.out.println("um: " + storageManager.getClass());
        IUserMessageEntity userMessageEntity =
                storageManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        // Adding Receipts
        Receipt receipt = new Receipt();
        receipt.setMessageId("some_receipt_id");
        receipt.setRefToMessageId(msgId);
        receipt.setTimestamp(new Date());
        ArrayList<OMElement> receiptContent = new ArrayList<>();

        OMElement receiptChildElement = headerBlock.getOMFactory()
                .createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        receiptChildElement.setText("eb3:UserMessage");
        receiptContent.add(receiptChildElement);
        receipt.setContent(receiptContent);

        IReceiptEntity receiptEntity =
                storageManager.storeIncomingMessageUnit(receipt);
        ArrayList<IReceiptEntity> receiptEntities = new ArrayList<>();
        receiptEntities.add(receiptEntity);
        mc.setProperty(MessageContextProperties.IN_RECEIPTS,
                receiptEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }


    }
}