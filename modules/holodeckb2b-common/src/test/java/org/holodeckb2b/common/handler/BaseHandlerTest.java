/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.common.handler;

import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
//import org.holodeckb2b.security.handlers.CreateWSSHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Created at 14:55 14.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class BaseHandlerTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    @BeforeClass
    public static void setUpClass() {
        baseDir = BaseHandlerTest.class
                .getClassLoader().getResource("common/handler").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testIsInFlow() throws Exception {
        fail("Not implemented yet!");
    }

    @Test
    public void testInvoke() throws Exception {
        fail("Not implemented yet!");
    }

    @Test
    public void testFlowComplete() throws Exception {
        fail("Not implemented yet!");
    }

    @Test
    public void testRunningInCorrectFlow() throws Exception {
        fail("Not implemented yet!");
    }
}