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
package org.holodeckb2b.as4.receptionawareness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.ReceptionAwarenessConfig;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 16:52 20.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class DetectDuplicateUserMessagesTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private DetectDuplicateUserMessages handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = DetectDuplicateUserMessagesTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new DetectDuplicateUserMessages();
        ModuleConfiguration moduleDescr = new ModuleConfiguration("test", null);
        moduleDescr.addParameter(new Parameter("HandledMessagingProtocol", "TEST"));
        HandlerDescription handlerDescr = new HandlerDescription();
        handlerDescr.setParent(moduleDescr);
        handler.init(handlerDescr);        
    }


    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData userMessage = TestUtils.getMMD("handlers/full_mmd.xml", this);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        Leg leg = new Leg();

        // Turning on duplicate detection
        ReceptionAwarenessConfig rac = new ReceptionAwarenessConfig();
        rac.setDuplicateDetection(true);
        leg.setReceptionAwareness(rac);
        pmode.addLeg(leg);
        pmode.setId(userMessage.getCollaborationInfo().getAgreement().getPModeId());
        core.getPModeSet().add(pmode);

        // Setting input message property
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = updateManager.storeIncomingMessageUnit(userMessage);
        
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(userMessageEntity);

        // Do as if message is already delivered
        updateManager.setProcessingState(userMessageEntity, ProcessingState.DELIVERED);
        // Checking that the message with userMessage id was already delivered
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.DUPLICATE, HolodeckB2BCoreInterface.getQueryManager()
										        		.getMessageUnitsWithId(userMessage.getMessageId())
														.iterator().next().getCurrentProcessingState().getState()); 
    }
}