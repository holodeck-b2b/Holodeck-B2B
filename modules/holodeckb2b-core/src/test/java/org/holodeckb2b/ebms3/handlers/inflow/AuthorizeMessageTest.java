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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 23:44 29.01.17
 *
 * Checked for cases coverage (24.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthorizeMessageTest {

    private static PMode    pmodeAuth;
    private static PMode    pmodeNoAuth;

    private MessageContext mc;
    private MessageProcessingContext  procCtx;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String baseDir = AuthorizeMessageTest.class.getClassLoader()
                    .getResource(AuthorizeMessageTest.class.getSimpleName()).getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));

        // Create the basic test data
        pmodeAuth = PMode.createFromXML(new FileInputStream(baseDir + "/pm-auth-messages.xml"));
        pmodeNoAuth = PMode.createFromXML(new FileInputStream(baseDir + "/pm-no-auth-messages.xml"));

        HolodeckB2BCore.getPModeSet().add(pmodeAuth);
        HolodeckB2BCore.getPModeSet().add(pmodeNoAuth);
    }

    @Before
    public void setUp() throws Exception {
        mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        procCtx = MessageProcessingContext.getFromMessageContext(mc);
        
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmodeAuth.getId());
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage));

        Receipt receipt = new Receipt();
        receipt.setPModeId(pmodeNoAuth.getId());
        receipt.setMessageId(MessageIdUtils.createMessageId());
        procCtx.addReceivedReceipt(HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(receipt));
    }

    @Test
    public void testAuthorized() throws Exception {

        // Mock username token result
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getTargetedRole()).thenReturn(SecurityHeaderTarget.EBMS);
        when(utResultMock.getUsername()).thenReturn("johndoe");
        when(utResultMock.getPassword()).thenReturn("secret");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);

        procCtx.addSecurityProcessingResult(utResultMock);

        try {
            new AuthorizeMessage().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testNotAuthorized() throws Exception {

        // Mock username token result
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getTargetedRole()).thenReturn(SecurityHeaderTarget.EBMS);
        when(utResultMock.getUsername()).thenReturn("janedoe");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);

        procCtx.addSecurityProcessingResult(utResultMock);

        IUserMessageEntity umEntity = procCtx.getReceivedUserMessage();
        try {
        	new AuthorizeMessage().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        
        assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
        assertEquals(1, procCtx.getGeneratedErrors().get(umEntity.getMessageId()).size());
        assertEquals("EBMS:0101", procCtx.getGeneratedErrors().get(umEntity.getMessageId()).iterator().next().getErrorCode());
        assertEquals(umEntity.getCurrentProcessingState().getState(), ProcessingState.FAILURE);
    }

}