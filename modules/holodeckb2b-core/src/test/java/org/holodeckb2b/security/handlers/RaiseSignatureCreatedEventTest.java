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
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.*;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.events.SignatureCreatedEvent;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.holodeckb2b.testhelpers.TestEventProcessor;

import javax.xml.namespace.QName;
import java.io.File;

import static org.junit.Assert.*;

/**
 * Created at 22:57 13.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class RaiseSignatureCreatedEventTest {

    static final QName MESSAGE_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");

    private static String baseDir;

    private static HolodeckCore core;

    private CreateWSSHeaders wssHeadersHandler;

    private RaiseSignatureCreatedEvent handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = RaiseSignatureCreatedEventTest.class
                .getClassLoader().getResource("security").getPath();
        core = new HolodeckCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        wssHeadersHandler = new CreateWSSHeaders();
        handler = new RaiseSignatureCreatedEvent();
    }

    @Test
    public void testDoProcessing() throws Exception {
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("security/handlers/full_mmd.xml").getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        // Creating SOAP envelope
        SOAPEnvelope env =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement userMessage = UserMessage.createElement(headerBlock, mmd);

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

        OMElement agreementRef =
                AgreementRef.getElement(CollaborationInfo.getElement(userMessage));
        String pmodeId = agreementRef.getText();

        pmode.setId(pmodeId);

        //Adding PMode to the managed PMode set.
        core.getPModeSet().add(pmode);

        EntityProxy<org.holodeckb2b.ebms3.persistency.entities.UserMessage>
                userMessageEntityProxy = null;
        try {
            userMessageEntityProxy =
                    MessageUnitDAO.storeReceivedMessageUnit(
                            UserMessage.readElement(userMessage));
        } catch (PackagingException e) {
            fail(e.getMessage());
        }
        userMessageEntityProxy.entity.setPMode(pmode.getId());
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE,
                userMessageEntityProxy);

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
}