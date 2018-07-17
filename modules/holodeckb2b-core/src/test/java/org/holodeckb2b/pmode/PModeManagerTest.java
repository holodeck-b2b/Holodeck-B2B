/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 17:28 09.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeManagerTest {

    private static String baseDir;

    private static PModeManager manager;

    private PMode validPMode;

    @BeforeClass
    public static void setUpClass() {
        baseDir = PModeManagerTest.class.getClassLoader().getResource("pmode_validation").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));
        InternalConfiguration initialConf =
                (InternalConfiguration)HolodeckB2BCoreInterface.getConfiguration();
        manager = new PModeManager(initialConf);
    }

    @Before
    public void setUp() throws Exception {
        validPMode = new PMode();
    }

    @After
    public void tearDown() throws Exception {
        validPMode = null;
    }

    @Test
    public void testManagerCreation() {
        assertNotNull(manager);
    }

    @Test
    public void testPModeAddReplace() {
        String pModeId = "123";
        validPMode.setId(pModeId);
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        validPMode.addLeg(new Leg());

        try {
            manager.add(validPMode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

        try {
            manager.replace(validPMode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

        assertTrue(manager.containsId("123"));
    }
}