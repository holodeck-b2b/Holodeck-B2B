/*
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.errors.PolicyNoncompliance;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PartnerConfig;
import org.holodeckb2b.common.pmode.SecurityConfig;
import org.holodeckb2b.common.pmode.SigningConfig;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
//@RunWith(MockitoJUnitRunner.class)
public class CheckSignatureRequirementTest {

	private PMode pmode;
	private MessageContext mc;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Before
    public void prepareContext() throws Exception {
        // Create the basic test data
        pmode = HB2BTestUtils.create1WayReceivePMode();
        PartnerConfig senderCfg = new PartnerConfig();
        senderCfg.setSecurityConfiguration(new SecurityConfig());
        pmode.setInitiator(senderCfg);

        HolodeckB2BCore.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        userMessage.setPModeId(pmode.getId());

    	mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(userMessage));
    }

    @Test
    public void testNoRequiredSigning() throws Exception {
        try {
            new CheckSignatureRequirement().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }

        assertTrue(Utils.isNullOrEmpty(MessageProcessingContext.getFromMessageContext(mc).getGeneratedErrors()));
    }

    @Test
    public void testSigned() throws Exception {

    	pmode.getInitiator().getSecurityConfiguration().setSignatureConfiguration(new SigningConfig());

    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        procCtx.addSecurityProcessingResult(signatureResult);

        try {
        	new CheckSignatureRequirement().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }

        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testNotSigned() throws Exception {

    	SecurityConfig secCfg = new SecurityConfig();
    	secCfg.setSignatureConfiguration(new SigningConfig());
    	pmode.getInitiator().setSecurityConfiguration(secCfg);

        try {
        	new CheckSignatureRequirement().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }

        Map<String, Collection<IEbmsError>> generatedErrors =
        										MessageProcessingContext.getFromMessageContext(mc).getGeneratedErrors();

        assertFalse(Utils.isNullOrEmpty(generatedErrors));
        assertEquals(PolicyNoncompliance.ERROR_CODE,
        								generatedErrors.values().iterator().next().iterator().next().getErrorCode());
    }

}