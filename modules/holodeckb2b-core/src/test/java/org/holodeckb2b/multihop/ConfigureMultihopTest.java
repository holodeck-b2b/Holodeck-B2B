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
package org.holodeckb2b.multihop;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.*;
import org.holodeckb2b.ebms3.persistency.entities.AgreementReference;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.pmode.PModeManager;
import org.holodeckb2b.pmode.helpers.Agreement;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.Protocol;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.holodeckb2b.testhelpers.MP;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created at 13:32 12.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ConfigureMultihopTest {

    private static String baseDir;

    private static HolodeckCore core;

    private ConfigureMultihop handler;

    private static PModeManager manager;

    @BeforeClass
    public static void setUpClass() {
        baseDir = CheckFromICloudTest.class.getClassLoader().getResource("multihop").getPath();
        core = new HolodeckCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
        InternalConfiguration initialConf =
                (InternalConfiguration)HolodeckB2BCoreInterface.getConfiguration();
        manager = new PModeManager(initialConf.getPModeValidatorImplClass(),
                initialConf.getPModeStorageImplClass());
    }

    @Before
    public void setUp() throws Exception {
        handler = new ConfigureMultihop();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testProcessingOfTheUserMessage() throws Exception {
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("multihop/icloud/full_mmd.xml").getPath();
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
        assertEquals(MP.ENVELOPE, env.toString());
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        //System.out.println("headerBlock: " + headerBlock);
        assertEquals(MP.ENVELOPE_WITH_HEADER_AND_MESSAGING, env.toString());
        // Adding UserMessage from mmd
        OMElement userMessage = UserMessage.createElement(headerBlock, mmd);

        EntityProxy<org.holodeckb2b.ebms3.persistency.entities.UserMessage>
                userMessageEntityProxy = null;
        try {
            userMessageEntityProxy =
                    MessageUnitDAO.storeReceivedMessageUnit(
                            UserMessage.readElement(userMessage));
        } catch (PackagingException e) {
            e.printStackTrace();
        }

        assertEquals(MP.ENVELOPE_WITH_HEADER_MESSAGING_AND_USER_MESSAGE, env.toString());

        OMElement ciElement = CollaborationInfo.getElement(userMessage);
        OMElement arElement = AgreementRef.getElement(ciElement);
        AgreementReference ar = AgreementRef.readElement(arElement);

        assertNotNull(ar.getPModeId());

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
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
        core.getPModeSet().add(pmode);

        userMessageEntityProxy.entity.setPMode(ar.getPModeId());

        // Setting out message property
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, userMessageEntityProxy);
        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        assertNotNull(messaging);
        // Setting Role, as stated in paragraph 4.3 of AS4 profile
        messaging.setRole(MultiHopConstants.NEXT_MSH_TARGET);

        assertNotNull(MessageContextUtils.getRcvdMessageUnits(mc));

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals("InvocationResponse.CONTINUE", invokeResp.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}