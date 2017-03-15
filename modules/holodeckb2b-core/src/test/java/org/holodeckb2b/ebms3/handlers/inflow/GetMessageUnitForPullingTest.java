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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.security.tokens.X509Certificate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created at 15:57 28.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class GetMessageUnitForPullingTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private FindPModesForPullRequest findPModesForPullRequestHandler;

    private GetMessageUnitForPulling handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = GetMessageUnitForPullingTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        findPModesForPullRequestHandler = new FindPModesForPullRequest();

        handler = new GetMessageUnitForPulling();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        // Setting token configuration
        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        X509Certificate sigConfig = new X509Certificate(null);
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");
        secConfig.setEncryptionConfiguration(encConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        Leg leg = new Leg();
        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setSecurityConfiguration(secConfig);
        leg.addPullRequestFlow(prFlow);

        pmode.addLeg(leg);

        final Map<String, IAuthenticationInfo> authInfo = new HashMap<>();
        authInfo.put(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);
        authInfo.put(SecurityConstants.SIGNATURE, sigConfig);

        mc.setProperty(SecurityConstants.MC_AUTHENTICATION_INFO, authInfo);

        // Pull request and corresponding user message should have similar MPC
        String commonMPC = "some_mpc";

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC(commonMPC);

        core.getPModeSet().add(pmode);

        // Setting input message property
        StorageManager storageManager = core.getStorageManager();
        System.out.println("um: " + storageManager.getClass());

        IPullRequestEntity pullRequestEntity =
                storageManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST,
                pullRequestEntity);

        try {
            Handler.InvocationResponse invokeResp = findPModesForPullRequestHandler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);
        String pmodeId = userMessage.getCollaborationInfo().getAgreement().getPModeId();
        userMessage.setPModeId(pmodeId);
        userMessage.setMPC(commonMPC);
        pmode.setId(pmodeId);

        // Setting input message property
        StorageManager updateManager = core.getStorageManager();
        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        updateManager.setProcessingState(userMessageEntity, ProcessingState.AWAITING_PULL);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE));
    }
}