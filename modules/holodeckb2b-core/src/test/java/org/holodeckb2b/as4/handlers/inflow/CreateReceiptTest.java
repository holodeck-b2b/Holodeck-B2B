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
package org.holodeckb2b.as4.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.as4.receptionawareness.ReceiptCreatedEvent;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:04 13.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CreateReceiptTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private CreateReceipt handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = CreateReceiptTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new CreateReceipt();
    }

    @Test
    public void testDoProcessing() throws Exception {
    	MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
    	SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        UserMessageElement.createElement(Messaging.createElement(soapEnvelope), mmd);
        
        PMode pmode = TestUtils.create1WayReceivePushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
        ReceiptConfiguration receiptConfig = new ReceiptConfiguration();
        receiptConfig.setPattern(ReplyPattern.RESPONSE);
        leg.setReceiptConfiguration(receiptConfig);
        pmode.addLeg(leg);
        HolodeckB2BCore.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage(mmd);
        userMessage.setPModeId(pmode.getId());

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeIncomingMessageUnit(userMessage);
        
        MessageContext mc = new MessageContext();
        mc.setEnvelope(soapEnvelope);
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);
        
        updateManager.setProcessingState(userMessageEntity, ProcessingState.DELIVERED);

        // Adding event processor to make sure the ReceiptCreatedEvent
        // is actually raised.
        final TestEventProcessor eventProcessor = new TestEventProcessor();
        core.setMessageProcessingEventProcessor(eventProcessor);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(1, eventProcessor.events.size());
        assertTrue(eventProcessor.events.get(0) instanceof ReceiptCreatedEvent);
    }
}