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
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.pmode.helpers.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created at 17:24 31.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ProcessWSSHeadersTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private SetupWSSProcessing setupWSSProcessingHandler;
    private ProcessWSSHeaders handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // The baseDir folder should contain publickeys.jks file
        baseDir = TestUtils.getPath(ProcessWSSHeadersTest.class, "security");
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        // launched after org.holodeckb2b.security.handlers.SetupWSSProcessing handler
        setupWSSProcessingHandler = new SetupWSSProcessing();
        handler = new ProcessWSSHeaders();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("security/handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        UserMessage um = UserMessageElement.readElement(umElement);

        String pmodeId = um.getCollaborationInfo().getAgreement().getPModeId();

        um.setPModeId(pmodeId);

        System.out.println("umElement: " + umElement.toString());

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

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

        // Setting security configuration
        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);

        PartnerConfig initiator = new PartnerConfig();
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        responder.setSecurityConfiguration(secConfig);
        pmode.setResponder(responder);

        //Adding PMode to the managed PMode set.
        core.getPModeSet().add(pmode);
        IUserMessageEntity userMessageEntity =
                core.getStorageManager()
                        .storeIncomingMessageUnit(um);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        // Invoke SetupWSSProcessing handler
        try {
            Handler.InvocationResponse invokeResp = setupWSSProcessingHandler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Invoke ProcessWSSHeaders handler
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(SecurityConstants.MC_AUTHENTICATION_INFO));
    }
}
