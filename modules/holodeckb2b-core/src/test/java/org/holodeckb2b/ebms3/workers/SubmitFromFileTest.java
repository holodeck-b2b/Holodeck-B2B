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
package org.holodeckb2b.ebms3.workers;

import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created at 8:04 19.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SubmitFromFileTest {

    private String basePath = TestUtils.getPath(SubmitFromFileTest.class, "submitfromfiletest");

    private static SubmitFromFileImpl worker;

    @Before
    public void setUp() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(basePath));
        worker = new SubmitFromFileImpl();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testMessageSubmission() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", basePath);
        try {
            worker.setParameters(params);
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
        assertNotNull(worker);

        worker.run();

        assertEquals(1, worker.c);
    }

    class SubmitFromFileImpl extends SubmitFromFile {
        public int c = 0;

        @Override
        public void run() {
            c = 0;
            super.run();
        }

        @Override
        public void onChange(final File f, final Event event) {
            System.out.println("Change [" + event.name() + "] reported for " + f.getAbsolutePath());
            c++;
        }
    }
}