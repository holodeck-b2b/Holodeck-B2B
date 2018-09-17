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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
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
    private static final String T_PMODE_ID = "pm-for-pulling";

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
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(T_PMODE_ID);
        userMessage.setMPC(T_MPC_1);
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        IUserMessageEntity umEntity = storeManager.storeOutGoingMessageUnit(userMessage);
        storeManager.setProcessingState(umEntity, ProcessingState.AWAITING_PULL);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC(T_MPC_1);
        IPullRequestEntity pullRequestEntity = storeManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST, pullRequestEntity);

        IPMode pmode = mock(IPMode.class);
        when(pmode.getId()).thenReturn(T_PMODE_ID);
        mc.setProperty(MessageContextProperties.PULL_AUTH_PMODES, Arrays.asList(new IPMode[] {pmode}));

        try {
            new GetMessageUnitForPulling().invoke(mc);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        IUserMessage pulledMessage = (IUserMessage) mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE);
        assertNotNull(pulledMessage);
        //assertEquals(userMessage.getMessageId(), pulledMessage.getMessageId());
        assertEquals(pulledMessage.getCurrentProcessingState().getState(), ProcessingState.PROCESSING);
        assertTrue((Boolean)mc.getProperty(MessageContextProperties.RESPONSE_REQUIRED));
    }

    /**
     * Test the case when pulled message is missing
     */
    @Test
    public void testDoProcessingWhenMsgIsNull() throws Exception {
//        MessageContext mc = new MessageContext();
//        mc.setFLOW(MessageContext.IN_FLOW);
//
//        PMode pmode = new PMode();
//        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
//        pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
//
//        // Setting token configuration
//        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
//        tokenConfig.setUsername("username");
//        tokenConfig.setPassword("secret");
//
//        PartnerConfig initiator = new PartnerConfig();
//        SecurityConfig secConfig = new SecurityConfig();
//        EncryptionConfig encConfig = new EncryptionConfig();
//        encConfig.setKeystoreAlias("exampleca");
//        secConfig.setEncryptionConfiguration(encConfig);
//        secConfig.setUsernameTokenConfiguration(
//                SecurityHeaderTarget.EBMS, tokenConfig);
//        initiator.setSecurityConfiguration(secConfig);
//        pmode.setInitiator(initiator);
//
//        Leg leg = new Leg();
//        PullRequestFlow prFlow = new PullRequestFlow();
//        prFlow.setSecurityConfiguration(secConfig);
//        leg.addPullRequestFlow(prFlow);
//
//        pmode.addLeg(leg);
//
//
//        // Pull request and corresponding user message should have similar MPC
//        String commonMPC = "some_mpc";
//
//        PullRequest pullRequest = new PullRequest();
//        pullRequest.setMPC(commonMPC);
//
//        core.getPModeSet().add(pmode);
//
//        // Setting input message property
//        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
//        System.out.println("um: " + storageManager.getClass());
//
//        IPullRequestEntity pullRequestEntity =
//                storageManager.storeIncomingMessageUnit(pullRequest);
//        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST,
//                pullRequestEntity);
//
//
//        // setting the pmodeId of some message
//        pmode.setId("some_pmode_id");
//
//        try {
//            Handler.InvocationResponse invokeResp = handler.invoke(mc);
//            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
//
//        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
//        assertEquals(ProcessingState.DONE,
//                pullRequestEntity.getCurrentProcessingState().getState());
    }
}