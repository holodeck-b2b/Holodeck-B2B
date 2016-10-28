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
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.*;
import org.holodeckb2b.ebms3.persistency.entities.AgreementReference;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.module.HolodeckB2BCoreImpl;
import org.holodeckb2b.module.HolodeckB2BCoreImplTest;
import org.holodeckb2b.pmode.helpers.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created at 22:57 13.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class RaiseSignatureCreatedEventTest {

    private static URL repoUrl;

    private static ConfigurationContext cc;
    private static AxisModule am;

    private HolodeckB2BCoreImpl coreImpl = new HolodeckB2BCoreImpl();

//    private static String baseDir;
//
//    private static HolodeckCore core;

    private CreateWSSHeaders wssHeadersHandler;

    private RaiseSignatureCreatedEvent handler;

    @BeforeClass
    public static void setUpClass() {
//        baseDir = CheckFromICloudTest.class
//                .getClassLoader().getResource("security").getPath();
//        core = new HolodeckCore(baseDir);
//        HolodeckB2BCoreInterface.setImplementation(core);

        repoUrl = HolodeckB2BCoreImplTest.class.getClassLoader()
                .getResource("moduletest/repository");

        AxisConfiguration ac = new AxisConfiguration();
        ac.setRepository(repoUrl);

        cc = new ConfigurationContext(ac);
        am = new AxisModule();
        am.setName(HolodeckB2BCoreImpl.HOLODECKB2B_CORE_MODULE);
    }

    @Before
    public void setUp() throws Exception {
        wssHeadersHandler = new CreateWSSHeaders();
        handler = new RaiseSignatureCreatedEvent();

        try {
            coreImpl.init(cc, am);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }
    }

    @Test
    public void testDoProcessing() throws Exception {
        System.out.println("[testDoProcessing]>");
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

        EntityProxy<org.holodeckb2b.ebms3.persistency.entities.UserMessage>
                userMessageEntityProxy = null;
        try {
            userMessageEntityProxy =
                    MessageUnitDAO.storeReceivedMessageUnit(
                            UserMessage.readElement(userMessage));
        } catch (PackagingException e) {
            fail(e.getMessage());
        }

        OMElement ciElement = CollaborationInfo.getElement(userMessage);
        OMElement arElement = AgreementRef.getElement(ciElement);
        AgreementReference ar = AgreementRef.readElement(arElement);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setProperty(SecurityConstants.ADD_SECURITY_HEADERS, Boolean.TRUE);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        // Setting security configuration
        PartnerConfig initiator = new PartnerConfig();
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");

        SecurityConfig secConfig = new SecurityConfig();
        secConfig.setSignatureConfiguration(sigConfig);

        mc.setProperty(SecurityConstants.SIGNATURE, sigConfig);

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.DEFAULT, tokenConfig);

        initiator.setSecurityConfiguration(secConfig);
        pmode.setInitiator(initiator);

        Leg leg = new Leg();

        Protocol protocolConfig = new Protocol();
        protocolConfig.setAddress("address");
        protocolConfig.setAddActorOrRoleAttribute(true);

        leg.setProtocol(protocolConfig);
        pmode.addLeg(leg);

        Agreement agreement = new Agreement();
        pmode.setAgreement(agreement);
        pmode.setId(ar.getPModeId());

        //Adding PMode to the managed PMode set.
//        core.getPModeSet().add(pmode);
        coreImpl.getPModeSet().add(pmode);

        userMessageEntityProxy.entity.setPMode(ar.getPModeId());

        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);
        mc.setProperty(SecurityConstants.DEFAULT_USERNAMETOKEN, tokenConfig);

        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, userMessageEntityProxy);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        // Invoking CreateSecurityHeaders handler
        try {
            System.out.println("[before wssHeadersHandler.invoke()]>");
            Handler.InvocationResponse invokeResp = wssHeadersHandler.invoke(mc);
            System.out.println("<[after wssHeadersHandler.invoke()]");
            assertEquals("InvocationResponse.CONTINUE", invokeResp.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Invoking RaiseSignatureEvent handler
        try {
            System.out.println("[before handler.invoke()]>");
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            System.out.println("<[after handler.invoke()]");
            assertEquals("InvocationResponse.CONTINUE", invokeResp.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        System.out.println("<[testDoProcessing]");
    }
}