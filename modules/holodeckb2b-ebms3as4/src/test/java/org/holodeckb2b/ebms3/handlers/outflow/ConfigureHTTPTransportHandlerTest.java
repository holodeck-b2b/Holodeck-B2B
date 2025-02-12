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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:48 27.02.17
 *
 * Checked for cases coverage (11.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ConfigureHTTPTransportHandlerTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }


    @After
    public void tearDown() throws Exception {
        HolodeckB2BCore.getPModeSet().removeAll();
    }

    @Test
    public void testDefault() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setServerSide(false);

        PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
        Leg leg = pmode.getLeg(Label.REQUEST);

        HolodeckB2BCore.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());

        // Add attached payload
        userMessage.addPayload(HB2BTestUtils.createPayload());

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new ConfigureHTTPTransportHandler().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(leg.getProtocol().getAddress(), mc.getProperty(Constants.Configuration.TRANSPORT_URL));
        assertTrue((Boolean) mc.getProperty(Constants.Configuration.ENABLE_SWA));
    }

    @Test
    public void testDoProcessing() throws Exception {
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	mc.setServerSide(false);

    	PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
        Leg leg = pmode.getLeg(Label.REQUEST);
        // Setting all protocol configurations checked by the tested handler
    	Protocol protocolConfig = leg.getProtocol();
    	protocolConfig.setHTTPCompression(true);
    	protocolConfig.setChunking(true);
    	leg.setProtocol(protocolConfig);

    	HolodeckB2BCore.getPModeSet().add(pmode);

    	UserMessage userMessage = new UserMessage();
    	userMessage.setPModeId(pmode.getId());

    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));

    	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, new ConfigureHTTPTransportHandler().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}

    	assertEquals(protocolConfig.getAddress(), mc.getProperty(Constants.Configuration.TRANSPORT_URL));
    	assertFalse((Boolean) mc.getProperty(Constants.Configuration.ENABLE_SWA));
    }
}