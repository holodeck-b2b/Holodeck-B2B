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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.xml.PMode;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    private MessageContext  mc;

    private IUserMessageEntity umEntity;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String baseDir = AuthorizeMessageTest.class.getClassLoader()
                    .getResource(AuthorizeMessageTest.class.getName().replace('.', '/')).getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));

        // Create the basic test data
        pmodeAuth = PMode.createFromFile(new File(baseDir + "/pm-auth-messages.xml"));
        pmodeNoAuth = PMode.createFromFile(new File(baseDir + "/pm-no-auth-messages.xml"));

        HolodeckB2BCore.getPModeSet().add(pmodeAuth);
        HolodeckB2BCore.getPModeSet().add(pmodeNoAuth);
    }

    @Before
    public void setUp() throws Exception {
        mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmodeAuth.getId());
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, umEntity);

        Receipt receipt = new Receipt();
        receipt.setPModeId(pmodeNoAuth.getId());
        receipt.setMessageId(MessageIdUtils.createMessageId());
        IReceiptEntity  rcptEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(receipt);
        mc.setProperty(MessageContextProperties.IN_RECEIPTS, Arrays.asList(new IReceiptEntity[] {rcptEntity}));
    }

    @Test
    public void testAuthorized() throws Exception {

        // Mock username token result
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getUsername()).thenReturn("johndoe");
        when(utResultMock.getPassword()).thenReturn("secret");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);

        mc.setProperty(MessageContextProperties.EBMS_UT_RESULT, utResultMock);

        try {
            new AuthorizeMessage().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        assertNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }

    @Test
    public void testNotAuthorized() throws Exception {

        // Mock username token result
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getUsername()).thenReturn("janedoe");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);

        mc.setProperty(MessageContextProperties.EBMS_UT_RESULT, utResultMock);

        try {
            new AuthorizeMessage().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
        assertTrue(((Collection<IEbmsError>) mc.getProperty(MessageContextProperties.GENERATED_ERRORS))
                    .parallelStream().anyMatch((e) -> e.getRefToMessageInError().equals(umEntity.getMessageId())));
        assertEquals(umEntity.getCurrentProcessingState().getState(), ProcessingState.FAILURE);
    }

}