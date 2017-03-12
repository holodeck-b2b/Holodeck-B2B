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
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
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

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 17:25 31.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class SetupWSSProcessingTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private SetupWSSProcessing handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = SetupWSSProcessingTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        // launched after org.holodeckb2b.ebms3.handlers.inflow.FindPModes handler
        handler = new SetupWSSProcessing();
        // Adding appender to the FindPModes logger
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

        PMode pmode = new PMode();
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

        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");

        // Setting security configuration
        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);
        secConfig.setEncryptionConfiguration(encConfig);

        PartnerConfig initiator = new PartnerConfig();
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        responder.setSecurityConfiguration(secConfig);
        pmode.setResponder(responder);

        Leg leg = new Leg();

        pmode.addLeg(leg);

        UserMessage um = UserMessageElement.readElement(umElement);

        String pmodeId = um.getCollaborationInfo().getAgreement().getPModeId();

        pmode.setId(pmodeId);

        um.setPModeId(pmodeId);

        System.out.println("umElement: " + umElement.toString());

        //Adding PMode to the managed PMode set.
        core.getPModeSet().add(pmode);
        IUserMessageEntity userMessageEntity =
                core.getStorageManager()
                        .storeIncomingMessageUnit(um);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        HashMap<String, Boolean> messages = new HashMap<>();
        messages.put("Primary message unit is user message, detect initiator or responder", false);
        messages.put("Security is  configured for the primary message unit", false);
        messages.put("P-Mode enables revocation check of certificates", false); // optional
        messages.put("Encryption used, provide access to private key.", false);
        Set<String> keys = messages.keySet();
        for(LoggingEvent e : events) {
            if(e.getLevel().equals(Level.DEBUG)) {
                String key = e.getRenderedMessage();
                if(keys.contains(key)) {
                    messages.put(key, true);
                }
            }
        }
        boolean containsAllMessages = true;
        for(Boolean flag : messages.values()) {
            containsAllMessages &= flag;
        }
        assertTrue(containsAllMessages);
    }
}