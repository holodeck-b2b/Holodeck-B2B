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
package org.holodeckb2b.security.handlers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PullRequestElement;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 12:10 15.03.17
 *
 * Checked for cases coverage (21.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class GetSecurityConfigurationTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private GetSecurityConfiguration handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = GetSecurityConfigurationTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        // launched after org.holodeckb2b.multihop.ConfigureMultihop handler
        handler = new GetSecurityConfiguration();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
        core.getPModeSet().removeAll();
    }

    /**
     * Test the case when primary message unit is UserMessage
     * @throws Exception
     */
    @Test
    public void testDoProcessingOfUserMessage() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("security/handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        UserMessage um = UserMessageElement.readElement(umElement);

        String pmodeId = "some_pmode_id";
        String msgId = um.getMessageId();
        um.setPModeId(pmodeId);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.addLeg(new Leg());

        // Setting token configuration
        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        // Setting signature configuration
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");
        sigConfig.setRevocationCheck(true); // optional

        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");

        // Setting security configuration
        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);
        secConfig.setEncryptionConfiguration(encConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);

        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);

        PartnerConfig initiator = new PartnerConfig();
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        responder.setSecurityConfiguration(secConfig);
        pmode.setResponder(responder);

        core.getPModeSet().add(pmode);
        IUserMessageEntity userMessageEntity =
                HolodeckB2BCore.getStorageManager()
                        .storeIncomingMessageUnit(um);
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "The primary message unit is a User Message with msg-id="
                + msgId;
        String msg1 = "Primary message unit is user message, detect initiator or responder";
        String msg2 = "Add security configuration to message context";
        String msg3 = "Username token configuration for ebms role added to message context";
        String msg4 = "Signature configuration added to message context";
        String msg5 = "Encryption configuration added to message context.";
        String msg6 = "Message context prepared for adding all security headers"
                + " (signing and encryption)";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg3));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg4));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg5));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg6));
    }

    /**
     * Test the case when primary message unit is PullRequest
     * @throws Exception
     */
    @Test
    public void testDoProcessingOfPullRequest() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        String pmodeId = "some_pmode_id";
        String msgId = "some_msg_id_01";
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setMessageId(msgId);
        pullRequest.setPModeId(pmodeId);

        PullRequestElement.createElement(headerBlock, pullRequest);

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        IPullRequestEntity pullRequestEntity =
                storageManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.OUT_PULL_REQUEST,
                pullRequestEntity);

        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.addLeg(new Leg());

        // Setting token configuration
        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        // Setting signature configuration
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");
        sigConfig.setRevocationCheck(true); // optional

        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");

        // Setting security configuration
        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);
        secConfig.setEncryptionConfiguration(encConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);

        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);

        PartnerConfig initiator = new PartnerConfig();
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        responder.setSecurityConfiguration(secConfig);
        pmode.setResponder(responder);

        core.getPModeSet().add(pmode);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "The primary message unit is a Pull Request with msg-id="
                + msgId;
        String msg1 = "Primary message unit is PullRequest, always initiator";
        String msg2 = "Add security configuration to message context";
        String msg3 = "Username token configuration for ebms role added to message context";
        String msg4 = "Signature configuration added to message context";
        String msg5 = "Message context prepared for adding all security headers"
                + " (signing and encryption)";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg3));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg4));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg5));
    }
}