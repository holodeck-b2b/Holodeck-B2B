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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 23:10 17.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CheckFromICloudTest {

    private static String baseDir;

    private CheckFromICloud handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = CheckFromICloudTest.class
                .getClassLoader().getResource("multihop").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));
    }

    @Before
    public void setUp() throws Exception {
        handler = new CheckFromICloud();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testMessageReceivedFromICloud() throws PersistenceException {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock messaging = Messaging.createElement(env);
        // Setting Role, as stated in paragraph 4.3 of AS4 profile
        messaging.setRole(MultiHopConstants.NEXT_MSH_TARGET);
        
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);        
        IUserMessageEntity userMessageEntity = HolodeckB2BCore.getStorageManager()
        													  .storeIncomingMessageUnit(new UserMessage());
        procCtx.setUserMessage(userMessageEntity);
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals("InvocationResponse.CONTINUE", invokeResp.toString());
            
            assertTrue(userMessageEntity.usesMultiHop());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
