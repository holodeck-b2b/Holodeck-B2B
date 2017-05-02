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
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.events.SignatureCreatedEvent;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.security.handlers.CreateWSSHeaders;
import org.holodeckb2b.security.handlers.RaiseSignatureCreatedEvent;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created at 17:01 10.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSignatureCompletenessTest {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private CreateWSSHeaders createWSSHeadersHandler;

    private RaiseSignatureCreatedEvent raiseSignatureCreatedEventHandler;

    private CheckSignatureCompleteness handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // launched after org.holodeckb2b.ebms3.handlers.inflow.StartProcessingUsrMessage handler
        baseDir = CheckSignatureCompletenessTest.class.getClassLoader()
                .getResource("security").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        createWSSHeadersHandler = new CreateWSSHeaders();
        raiseSignatureCreatedEventHandler = new RaiseSignatureCreatedEvent();

        handler = new CheckSignatureCompleteness();
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testDoProcessing() throws Exception {

        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);

        String pmodeId = userMessage.getCollaborationInfo().getAgreement().getPModeId();

        userMessage.setPModeId(pmodeId);

        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        // Setting token configuration
        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        // Setting signature configuration
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");
        sigConfig.setRevocationCheck(true); // optional
        mc.setProperty(SecurityConstants.SIGNATURE, sigConfig);

        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");

        // Setting security configuration
        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);
        secConfig.setEncryptionConfiguration(encConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);
        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, secConfig);

        PartnerConfig initiator = new PartnerConfig();
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        responder.setSecurityConfiguration(secConfig);
        pmode.setResponder(responder);

        Leg leg = new Leg();
        ReceiptConfiguration receiptConfiguration = new ReceiptConfiguration();
        leg.setReceiptConfiguration(receiptConfiguration);

        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);


        final Map<String, IAuthenticationInfo> authInfo = new HashMap<>();
        authInfo.put(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);

        mc.setProperty(SecurityConstants.MC_AUTHENTICATION_INFO, authInfo);

        StorageManager updateManager = core.getStorageManager();

        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);

        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setProperty(SecurityConstants.ADD_SECURITY_HEADERS, Boolean.TRUE);
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = createWSSHeadersHandler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Adding event processor to make sure the SignatureCreatedEvent
        // is actually raised.
        final TestEventProcessor eventProcessor = new TestEventProcessor();
        core.setEventProcessor(eventProcessor);

        try {
            Handler.InvocationResponse invokeResp = raiseSignatureCreatedEventHandler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(1, eventProcessor.events.size());
        assertTrue(eventProcessor.events.get(0) instanceof SignatureCreatedEvent);

        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        assertEquals(ProcessingState.RECEIVED,
                userMessageEntity.getCurrentProcessingState().getState());

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.RECEIVED,
                userMessageEntity.getCurrentProcessingState().getState());
    }
}