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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.namespace.Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.pmode.Leg;
import org.holodeckb2b.common.testhelpers.pmode.PMode;
import org.holodeckb2b.common.testhelpers.pmode.Protocol;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 23:42 29.01.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateSOAPEnvelopeHandlerTest {

    @BeforeClass
    public static void setUpClass() throws Exception {       
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @After
    public void tearDown() throws Exception {
        HolodeckB2BCore.getPModeSet().removeAll();
    }

    @Test
    public void testSOAP11Env() throws Exception {
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	
    	PMode pmode = new PMode();
    	pmode.setId("soap-11");
    	Leg leg = new Leg();
    	Protocol protocolConfig = new Protocol();
    	protocolConfig.setSOAPVersion("1.1");
    	leg.setProtocol(protocolConfig);
    	pmode.addLeg(leg);
    	
    	HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	// Setting input message property
    	UserMessage userMessage = new UserMessage();
    	userMessage.setPModeId(pmode.getId());
    	
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));
    	
    	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, new CreateSOAPEnvelopeHandler().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertEquals(Constants.URI_SOAP11_ENV, mc.getEnvelope().getNamespaceURI());
    }
    
    @Test
    public void testSOAP12Env() throws Exception {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        
        PMode pmode = new PMode();
        pmode.setId("soap-12");
        Leg leg = new Leg();
        Protocol protocolConfig = new Protocol();
        protocolConfig.setSOAPVersion("1.2");
        leg.setProtocol(protocolConfig);
        pmode.addLeg(leg);

        HolodeckB2BCore.getPModeSet().add(pmode);

        // Setting input message property
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new CreateSOAPEnvelopeHandler().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(Constants.URI_SOAP12_ENV, mc.getEnvelope().getNamespaceURI());
    }

    @Test
    public void testSameVersionAsRequest() throws Exception {
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.OUT_FLOW);
    	mc.setServerSide(true);
    	
    	PMode pmode = new PMode();
    	pmode.setId("soap-12");
    	Leg leg = new Leg();
    	Protocol protocolConfig = new Protocol();
    	protocolConfig.setSOAPVersion("1.2");
    	leg.setProtocol(protocolConfig);
    	pmode.addLeg(leg);
    	
    	HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	// Setting input message property
    	UserMessage userMessage = new UserMessage();
    	userMessage.setPModeId(pmode.getId());
    	
    	MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(userMessage));
    	
    	// Mocking the Axis2 Operation Context
    	MessageContext inCtx = mock(MessageContext.class);
    	when(inCtx.isSOAP11()).thenReturn(true);
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE)).thenReturn(inCtx);
        mc.setOperationContext(operationContext);
            	
    	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, new CreateSOAPEnvelopeHandler().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertEquals(Constants.URI_SOAP11_ENV, mc.getEnvelope().getNamespaceURI());
    }

}