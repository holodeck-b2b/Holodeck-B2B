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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:57 28.02.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class GetMessageUnitForPullingTest {

    private static final String T_MPC_1 = "http://test.holodeck-b2b.org/mpc";

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    /**
     * Tests the case when the pulled message is present
     * @throws Exception
     */
    @Test
    public void testAvailableOnMPC() throws Exception {
        StorageManager storeManager = HolodeckB2BCore.getStorageManager();
        
        PMode pmode = TestUtils.create1WayReceivePushPMode();
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
        Leg leg = pmode.getLeg(Label.REQUEST);
        
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());
        userMessage.setMPC(T_MPC_1);
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        IUserMessageEntity umEntity = storeManager.storeOutGoingMessageUnit(userMessage);
        storeManager.setProcessingState(umEntity, ProcessingState.AWAITING_PULL);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC(T_MPC_1);
        IPullRequestEntity pullRequestEntity = storeManager.storeIncomingMessageUnit(pullRequest);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setPullRequest(pullRequestEntity);

        procCtx.setProperty(FindPModesForPullRequest.FOUND_PULL_PMODES, Collections.singletonList(pmode));

        try {
            assertEquals(InvocationResponse.CONTINUE, new GetMessageUnitForPulling().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        IUserMessageEntity pulledMsg = procCtx.getSendingUserMessage();
        assertNotNull(pulledMsg);
        assertEquals(userMessage.getMessageId(), pulledMsg.getMessageId());
        assertEquals(ProcessingState.PROCESSING, pulledMsg.getCurrentProcessingState().getState());
        
        assertEquals(ProcessingState.DONE, pullRequestEntity.getCurrentProcessingState().getState());
    }
    
    /**
     * Tests the case when the pulled message is present
     * @throws Exception
     */
    @Test
    public void testEmptyMPC() throws Exception {
        StorageManager storeManager = HolodeckB2BCore.getStorageManager();
        
        PMode pmode = TestUtils.create1WaySendPushPMode();
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
        Leg leg = pmode.getLeg(Label.REQUEST);
        
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());
        userMessage.setMPC(T_MPC_1);
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        IUserMessageEntity umEntity = storeManager.storeOutGoingMessageUnit(userMessage);
        storeManager.setProcessingState(umEntity, ProcessingState.DELIVERED);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC(T_MPC_1);
        pullRequest.setMessageId(MessageIdUtils.createMessageId());
        IPullRequestEntity pullRequestEntity = storeManager.storeIncomingMessageUnit(pullRequest);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setPullRequest(pullRequestEntity);

        procCtx.setProperty(FindPModesForPullRequest.FOUND_PULL_PMODES, Collections.singletonList(pmode));

        try {
            assertEquals(InvocationResponse.CONTINUE, new GetMessageUnitForPulling().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertNull(procCtx.getSendingUserMessage());
        assertEquals(ProcessingState.WARNING, pullRequestEntity.getCurrentProcessingState().getState());
        
        assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors().get(pullRequestEntity.getMessageId())));
        assertEquals("EBMS:0006" ,
        			 procCtx.getGeneratedErrors().get(pullRequestEntity.getMessageId()).iterator().next().getErrorCode());
    }    
}