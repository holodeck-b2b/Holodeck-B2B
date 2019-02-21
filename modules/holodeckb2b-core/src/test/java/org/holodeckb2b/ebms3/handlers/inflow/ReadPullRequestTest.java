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
package org.holodeckb2b.ebms3.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:08 15.03.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ReadPullRequestTest {

	private static final String XML_START = 
			"<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\"\n" + 
			"    xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\"\n" + 
			"    xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\"\n" + 
			"    xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">\n" + 
			"    <soapenv:Header>        \n" + 
			"        <eb3:Messaging xmlns:mustUnderstand=\"http://www.w3.org/2003/05/soap-envelope\"\n" + 
			"            xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" + 
			"            wsu:Id=\"id-501d4b2b-0cac5f6a-30cc-46b4-856d-8e2e4f4e2321\"\n" + 
			"            mustUnderstand:mustUnderstand=\"true\">";
	
	private static final String XMLT_SIGNAL = 
			"            <eb3:SignalMessage>\n" + 
			"                <eb3:MessageInfo>\n" + 
			"                    <eb3:Timestamp>2019-01-28T12:03:13.355Z</eb3:Timestamp>\n" + 
			"                    <eb3:MessageId>%s</eb3:MessageId>\n" + 
			"                </eb3:MessageInfo>\n" +
			"                <eb3:PullRequest %s /> \n" + 
			"            </eb3:SignalMessage>";
	
	private static final String XML_END =			
			"        </eb3:Messaging>\n" + 
			"    </soapenv:Header>\n" + 
			"    <soapenv:Body/>\n" + 
			"</soapenv:Envelope>";
	
    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Test
    public void testNoMPC() throws Exception {        
    	String envXML = XML_START + 
    					String.format(XMLT_SIGNAL, "pull-req-id", "") +     					
    					XML_END;
    	
    	SOAPModelBuilder soapModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(new StringReader(envXML));
    	
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setEnvelope(soapModelBuilder.getSOAPEnvelope());

        MessageProcessingContext procCtx = new MessageProcessingContext(mc);
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new ReadPullRequest().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(procCtx.getReceivedPullRequest());
        assertEquals("pull-req-id", procCtx.getReceivedPullRequest().getMessageId());
        assertEquals(EbMSConstants.DEFAULT_MPC, procCtx.getReceivedPullRequest().getMPC());
    }

    @Test
    public void testSpecificMPC() throws Exception {        
    	String envXML = XML_START + 
    					String.format(XMLT_SIGNAL, "pull-req-id2", "mpc=\"http://some.specific.mpc/\"") +     					
    					XML_END;
    	
    	SOAPModelBuilder soapModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(new StringReader(envXML));
    	
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setEnvelope(soapModelBuilder.getSOAPEnvelope());

        MessageProcessingContext procCtx = new MessageProcessingContext(mc);
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new ReadPullRequest().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(procCtx.getReceivedPullRequest());
        assertEquals("pull-req-id2", procCtx.getReceivedPullRequest().getMessageId());
        assertEquals("http://some.specific.mpc/", procCtx.getReceivedPullRequest().getMPC());
    }
    
    @Test
    public void testIgnoreMultipeReq() throws Exception {        
    	String envXML = XML_START + 
    					String.format(XMLT_SIGNAL, "pull-req-id1", "") +     					
    					String.format(XMLT_SIGNAL, "pull-req-id2", "mpc=\"http://some.specific.mpc/\"") +     					
    					XML_END;
    	
    	SOAPModelBuilder soapModelBuilder = OMXMLBuilderFactory.createSOAPModelBuilder(new StringReader(envXML));
    	
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setEnvelope(soapModelBuilder.getSOAPEnvelope());

        MessageProcessingContext procCtx = new MessageProcessingContext(mc);
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new ReadPullRequest().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(procCtx.getReceivedPullRequest());
        assertEquals("pull-req-id1", procCtx.getReceivedPullRequest().getMessageId());        
    }
    
}