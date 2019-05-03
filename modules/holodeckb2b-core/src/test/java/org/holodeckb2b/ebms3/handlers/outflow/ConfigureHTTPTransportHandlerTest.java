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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.activation.DataHandler;

import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.transport.http.HTTPConstants;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
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

        PMode pmode = new PMode();
        pmode.setId("http-def-cfg");
        Leg leg = new Leg();
        // Setting all protocol configurations checked by the tested handler
        Protocol protocolConfig = new Protocol();
        String destUrl = "http://default.example.com";
        protocolConfig.setAddress(destUrl);
        leg.setProtocol(protocolConfig);
        pmode.addLeg(leg);

        HolodeckB2BCore.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());
        
        // Simulate a payload attachment
        mc.addAttachment("pl-1", new DataHandler("Some text", "application/text"));
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage));

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new ConfigureHTTPTransportHandler().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(destUrl, mc.getProperty(Constants.Configuration.TRANSPORT_URL));

        Options options = mc.getOptions();
        assertNotNull(options);
        assertFalse((Boolean) options.getProperty(HTTPConstants.MC_GZIP_REQUEST));
        assertFalse((Boolean) options.getProperty(HTTPConstants.CHUNKED));
        assertTrue((Boolean) options.getProperty(Constants.Configuration.ENABLE_SWA));
    }

    @Test
    public void testDoProcessing() throws Exception {
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	
    	PMode pmode = new PMode();
    	pmode.setId("http-cfg");
    	Leg leg = new Leg();
    	// Setting all protocol configurations checked by the tested handler
    	Protocol protocolConfig = new Protocol();
    	String destUrl = "http://example.com";
    	protocolConfig.setAddress(destUrl);
    	protocolConfig.setHTTPCompression(true);
    	protocolConfig.setChunking(true);
    	leg.setProtocol(protocolConfig);
    	pmode.addLeg(leg);
    	
    	HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	UserMessage userMessage = new UserMessage();
    	userMessage.setPModeId(pmode.getId());
    	
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage));
    	
    	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, new ConfigureHTTPTransportHandler().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertEquals(destUrl, mc.getProperty(Constants.Configuration.TRANSPORT_URL));
    	
    	Options options = mc.getOptions();
    	assertNotNull(options);
    	assertTrue((Boolean) options.getProperty(HTTPConstants.MC_GZIP_REQUEST));
    	assertTrue((Boolean) options.getProperty(HTTPConstants.CHUNKED));
    	assertFalse((Boolean) options.getProperty(Constants.Configuration.ENABLE_SWA));
    }
}