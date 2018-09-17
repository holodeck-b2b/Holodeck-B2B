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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.holodeckb2b.common.testhelpers.Submitter;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 8:04 19.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SubmitFromFileTest {

    private String basePath = TestUtils.getPath(SubmitFromFileTest.class, "submitfromfiletest");

    @Before
    public void setUp() throws Exception {
    	HolodeckB2BTestCore core = new HolodeckB2BTestCore(basePath);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testMessageSubmission() {
        SubmitFromFile worker = new SubmitFromFile();

        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", basePath);
        try {
            worker.setParameters(params);
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
        
        worker.run();

        assertEquals(1, ((Submitter) HolodeckB2BCoreInterface.getMessageSubmitter()).getAllSubmitted().size());
    }
}
