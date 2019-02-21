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
package org.holodeckb2b.as4.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.pmode.Leg;
import org.holodeckb2b.common.testhelpers.pmode.PMode;
import org.holodeckb2b.common.testhelpers.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 17:01 10.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSignatureCompletenessTest {

	private static UserMessage	userMessage;
    
	private MessageContext mc;
	private MessageProcessingContext procCtx;
	
    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());

        // Create the basic test data
        PMode pmode = new PMode();
        pmode.setId("pm-pmodeid-1");
        Leg leg = new Leg();
        ReceiptConfiguration receiptConfig = new ReceiptConfiguration();
        receiptConfig.setPattern(ReplyPattern.RESPONSE);
        leg.setReceiptConfiguration(receiptConfig);
        pmode.addLeg(leg);
        HolodeckB2BCore.getPModeSet().add(pmode);

        userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());

        Payload pl1 = new Payload();
        pl1.setContainment(IPayload.Containment.ATTACHMENT);
        pl1.setPayloadURI("first-payload-ref");
        userMessage.addPayload(pl1);
        Payload pl2 = new Payload();
        pl2.setContainment(IPayload.Containment.ATTACHMENT);
        pl2.setPayloadURI("first-payload-ref");
        userMessage.addPayload(pl2);
        Payload pl3 = new Payload();
        pl1.setContainment(IPayload.Containment.BODY);
        userMessage.addPayload(pl3);
    }

    @Before
    public void prepareContext() throws PersistenceException {
        mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        
        procCtx = new MessageProcessingContext(mc);
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage));
    }
    
    @Test
    public void testUnSigned() throws Exception {
        try {
            new CheckSignatureCompleteness().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }

        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testSignedCompletely() throws Exception {
        // Create complete set of digests
        
    	Map<IPayload, ISignedPartMetadata>  digestData = new HashMap<>();
        userMessage.getPayloads().forEach((p) -> digestData.put(p, null));

        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        when(signatureResult.getPayloadDigests()).thenReturn(digestData);
        
        procCtx.addSecurityProcessingResult(signatureResult);

        try {
        	new CheckSignatureCompleteness().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testUnsignedPayload() throws Exception {
        // Create complete set of digests
        Map<IPayload, ISignedPartMetadata>  digestData = new HashMap<>();

        Iterator<IPayload> iterator = userMessage.getPayloads().iterator();
        for (int i = 0; i < userMessage.getPayloads().size() - 1; i++) {
            digestData.put(iterator.next(), null);
        }
        
        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        when(signatureResult.getPayloadDigests()).thenReturn(digestData);
        procCtx.addSecurityProcessingResult(signatureResult);

        try {
        	new CheckSignatureCompleteness().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        
        assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors().get(userMessage.getMessageId())));
        assertEquals("EBMS:0103", 
        				procCtx.getGeneratedErrors().get(userMessage.getMessageId()).iterator().next().getErrorCode());
    }
}