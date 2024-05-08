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
package org.holodeckb2b.ebms3.validation.header;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PartyId;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.core.validation.header.HeaderValidationHandler;
import org.holodeckb2b.ebms3.module.EbMS3Module;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IPullRequestEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 23:49 29.01.17
 *
 * Checked for compliance (29.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class HeaderValidationTest {

    static final QName RECEIPT_CHILD_ELEMENT_NAME = new QName("ReceiptChild");
    private HeaderValidationHandler handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }
    
    @Before
    public void setUp() throws Exception {
        handler = new HeaderValidationHandler();
        
        HandlerDescription handlerDesc = new HandlerDescription("testHeaderValidation");
        handlerDesc.addParameter(new Parameter(HeaderValidationHandler.P_VALIDATOR_FACTORY, 
        										Ebms3HeaderValidatorFactory.class.getName()));
        
        ParameterInclude parent = new AxisModule(EbMS3Module.HOLODECKB2B_EBMS3_MODULE);
        parent.addParameter(new Parameter("HandledMessagingProtocol", "AS4"));
        handlerDesc.setParent(parent);
        handler.init(handlerDesc);
    }

    @Test
    public void testDoProcessingOfUserMessage() throws Exception {
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
        collabInfo.setService(new Service("PackagingTest"));
        collabInfo.setAction("Create");
        userMessage.setCollaborationInfo(collabInfo);
    	userMessage.addMessageProperty(new Property("some-meta-data", "description"));
        Payload p = new Payload();
    	p.setPayloadURI("cid:as_attachment");
    	p.addProperty(new Property("p1", "v1"));
    	userMessage.addPayload(p);
    	
    	// Setting input message property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeReceivedMessageUnit(userMessage);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // When validation is successful there should be no error in the message context
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testDoProcessingOfPullRequest() throws Exception {
        // Adding PullRequest
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setMessageId("some_id");
        // there should not be ref to message id
//        pullRequest.setRefToMessageId("some_ref_to_message_id");
        pullRequest.setTimestamp(new Date());

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        // Setting input PullRequest property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IPullRequestEntity pullRequestEntity =
                updateManager.storeReceivedMessageUnit(pullRequest);
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setPullRequest(pullRequestEntity);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // When validation is succesful there should be no error in the message context
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testDoProcessingOfReciepts() throws Exception {
        // Adding Receipt
        Receipt receipt = new Receipt();
        receipt.setMessageId("some_message_id");
        receipt.setRefToMessageId("some_ref_to_message_id");
        receipt.setTimestamp(new Date());
        ArrayList<OMElement> receiptContent = new ArrayList<>();

        OMElement receiptChildElement = OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        receiptChildElement.setText("eb3:UserMessage");        
        receiptContent.add(receiptChildElement);
        receipt.setContent(receiptContent);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        // Setting input Receipt property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();

        IReceiptEntity receiptEntity =
                updateManager.storeReceivedMessageUnit(receipt);
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedReceipt(receiptEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // When validation is successful there should be no error in the message context
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testDoProcessingOfErrors() throws Exception {
        // Initialising Errors
        ErrorMessage error = new ErrorMessage();
        error.setMessageId("some_message_id");
        error.setTimestamp(new Date());
        ArrayList<IEbmsError> errors = new ArrayList<>();
        errors.add(new EbmsError());
        error.setErrors(errors);

        MessageContext mc = new MessageContext();

        // Setting input Receipt property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();

        IErrorMessageEntity errorMessageEntity =
                updateManager.storeReceivedMessageUnit(error);
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedError(errorMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // When validation is successful there should be no error in the message context
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }
}