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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.xml.PMode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 17:39 24.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PerformCustomValidationsTest {

    private static HolodeckB2BTestCore core;

    private static PMode rejectOnFailurePmode;
    private static PMode rejectOnWarnPmode;

    private MessageContext  mc;

    private IUserMessageEntity umEntity;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String baseDir = PerformCustomValidationsTest.class.getClassLoader()
                .getResource(PerformCustomValidationsTest.class.getName().replace('.', '/')).getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);

        rejectOnFailurePmode = PMode.createFromFile(new File(baseDir + "/reject-on-failure-pmode.xml"));
        rejectOnWarnPmode = PMode.createFromFile(new File(baseDir + "/reject-on-warn-pmode.xml"));

        core.getPModeSet().add(rejectOnFailurePmode);
        core.getPModeSet().add(rejectOnWarnPmode);
    }

    @Before
    public void setUp() throws Exception {
        mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
//        LogManager.getRootLogger().removeAppender(mockAppender);
        core.getPModeSet().removeAll();
    }

    @Test
     public void testDoProcessing() throws Exception {
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(rejectOnWarnPmode.getId());
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, umEntity);

        try {
            new PerformCustomValidations().invoke(mc);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }

    @Test
    public void testDoProcessingRejectOnFailure() throws Exception {
        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(rejectOnFailurePmode.getId());
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, umEntity);

        try {
            new PerformCustomValidations().invoke(mc);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }
}