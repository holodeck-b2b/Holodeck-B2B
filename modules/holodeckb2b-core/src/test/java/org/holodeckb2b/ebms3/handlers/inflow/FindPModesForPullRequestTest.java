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

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.security.tokens.X509Certificate;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.holodeckb2b.core.testhelpers.TestUtils.eventContainsMsg;
import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 16:00 28.02.17
 *
 * Checked for cases coverage (03.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class FindPModesForPullRequestTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private FindPModesForPullRequest handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = FindPModesForPullRequestTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new FindPModesForPullRequest();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    /**
     * Test the case when pmode was correctly initialized and found in the storage
     */
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

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMessageId("some_msg_id");

        core.getPModeSet().add(pmode);

        // Setting input message property
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        System.out.println("um: " + storageManager.getClass());

        IPullRequestEntity pullRequestEntity =
                storageManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST,
                pullRequestEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log messages to make sure handler found pmode
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "Store the list of " + 1
                + " authorized PModes so next handler can retrieve message unit to return";
        assertTrue(eventContainsMsg(events, Level.DEBUG, msg));
    }

    /**
     * Test the case when pmode was not found for the PullRequest
     */
    @Test
    public void testDoProcessingIfPModeNotFound() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMessageId("some_msg_id");

        // Setting input message property
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        System.out.println("um: " + storageManager.getClass());

        IPullRequestEntity pullRequestEntity =
                storageManager.storeIncomingMessageUnit(pullRequest);
        mc.setProperty(MessageContextProperties.IN_PULL_REQUEST,
                pullRequestEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.FAILURE,
                pullRequestEntity.getCurrentProcessingState().getState());

        ArrayList<IEbmsError> errList =
                (ArrayList<IEbmsError>)mc.getProperty(
                        MessageContextProperties.GENERATED_ERRORS);
        assertTrue(!Utils.isNullOrEmpty(errList));

        IEbmsError error = errList.get(0);
        assertEquals(IEbmsError.Severity.FAILURE, error.getSeverity());
        assertEquals("EBMS:0010", error.getErrorCode());
        assertEquals("Processing", error.getCategory());
        assertEquals("ProcessingModeMismatch", error.getMessage());

        // Checking log messages to make sure handler found pmode
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg = "No P-Mode found for PullRequest ["
                + "some_msg_id" + "], unable to process it!";
        assertTrue(eventContainsMsg(events, Level.ERROR, msg));
    }
}