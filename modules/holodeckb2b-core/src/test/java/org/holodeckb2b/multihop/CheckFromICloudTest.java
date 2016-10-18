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
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PackagingException;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created at 23:10 17.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CheckFromICloudTest {

    private static String baseDir;

    private CheckFromICloud handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = CheckFromICloudTest.class
                .getClassLoader().getResource("multihop").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(baseDir));
    }

    @Before
    public void setUp() throws Exception {
        handler = new CheckFromICloud();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testMessageReceivedFromICloud() throws DatabaseException {

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

        MessageContext mc = new MessageContext();

        // Setting input message property
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntityProxy);
        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
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
