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
package org.holodeckb2b.security.util;

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
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.holodeckb2b.axis2.Axis2Utils;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.pmode.helpers.EncryptionConfig;
import org.holodeckb2b.pmode.helpers.SigningConfig;
import org.holodeckb2b.pmode.helpers.UsernameTokenConfig;
import org.holodeckb2b.security.handlers.CreateWSSHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created at 18:36 27.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class WSSProcessingEngineTest {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private CreateWSSHeaders createWSSHeadersHandler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = WSSProcessingEngineTest.class.getClassLoader()
                .getResource("security").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        createWSSHeadersHandler = new CreateWSSHeaders();
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.ALL);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testProcessSecurityHeader() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("security/handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setProperty(SecurityConstants.ADD_SECURITY_HEADERS, Boolean.TRUE);

        // Setting signature configuration
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");

        // Setting encription configuration
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partyb");
        encConfig.setCertificatePassword("ExampleB");

        // Setting token configuration
        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        mc.setProperty(SecurityConstants.SIGNATURE, sigConfig);

        mc.setProperty(SecurityConstants.ENCRYPTION, encConfig);
        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);
        mc.setProperty(SecurityConstants.DEFAULT_USERNAMETOKEN, tokenConfig);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        try {
            Handler.InvocationResponse invokeResp =
                    createWSSHeadersHandler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        Document domEnvelope = Axis2Utils.convertToDOM(mc);

        // First method parameter value
        final Element securityHeader =
                WSSecurityUtil.getSecurityHeader(domEnvelope,
                        SecurityConstants.EBMS_WSS_HEADER);

        final WSSConfig config = WSSConfig.getNewInstance();
        config.setValidator(WSSecurityEngine.USERNAME_TOKEN, new NoOpValidator());
        final WSSProcessingEngine engine = new WSSProcessingEngine();
        engine.setWssConfig(config);

        // Second method parameter value
        final RequestData requestData = new RequestData();
        requestData.setWssConfig(engine.getWssConfig());
        requestData.setMsgContext(mc);

        List<WSSecurityEngineResult> result =
                engine.processSecurityHeader(securityHeader, requestData);
        assertNotNull(result);
        assertTrue(result.size()>0);

//        System.out.println("result amount:" + result.size());
//        WSSecurityEngineResult r = result.iterator().next();
//        System.out.println("r: " + r);
    }
}