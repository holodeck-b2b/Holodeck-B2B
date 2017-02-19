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

import java.io.File;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.*;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.events.SignatureCreatedEvent;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestEventProcessor;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 22:57 13.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class RaiseSignatureCreatedEventTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private CreateWSSHeaders wssHeadersHandler;

    private RaiseSignatureCreatedEvent handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = RaiseSignatureCreatedEventTest.class
                .getClassLoader().getResource("security").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        wssHeadersHandler = new CreateWSSHeaders();
        handler = new RaiseSignatureCreatedEvent();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = getMMD("security/handlers/full_mmd.xml");
        // Creating SOAP envelope
        SOAPEnvelope env =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        System.out.println("[1] umElement: " + umElement.toString());

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setProperty(SecurityConstants.ADD_SECURITY_HEADERS, Boolean.TRUE);

        // Setting signature configuration
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");

        mc.setProperty(SecurityConstants.SIGNATURE, sigConfig);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        // Invoking CreateSecurityHeaders handler
        try {
            Handler.InvocationResponse invokeResp = wssHeadersHandler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        // Setting security configuration
        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);

        PartnerConfig initiator = new PartnerConfig();
        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        Leg leg = new Leg();

        Protocol protocolConfig = new Protocol();
        protocolConfig.setAddress("address");
        protocolConfig.setAddActorOrRoleAttribute(true);

        leg.setProtocol(protocolConfig);
        pmode.addLeg(leg);

        UserMessage um = UserMessageElement.readElement(umElement);

        String pmodeId = um.getCollaborationInfo().getAgreement().getPModeId();

        pmode.setId(pmodeId);

        System.out.println("[2] umElement: " + umElement.toString());

        //Adding PMode to the managed PMode set.
        core.getPModeSet().add(pmode);
        IUserMessageEntity userMessageEntity =
                      core.getUpdateManager()
                              .storeIncomingMessageUnit(um);
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE,
                userMessageEntity);

        // Adding event processor to make sure the SignatureCreatedEvent
        // is actually raised.
        final TestEventProcessor eventProcessor = new TestEventProcessor();
        core.setEventProcessor(eventProcessor);

        // Invoking RaiseSignatureEvent handler
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(1, eventProcessor.events.size());
        assertTrue(eventProcessor.events.get(0) instanceof SignatureCreatedEvent);
    }

    /**
     * Get filled mmd document for testing
     * @return
     */
    private MessageMetaData getMMD(String resource) {
        final String mmdPath =
                this.getClass().getClassLoader().getResource(resource).getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        return mmd;
    }
}