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

import java.util.HashMap;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 8:04 19.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SubmitFromFileTest {

    private String basePath = TestUtils.getPath(SubmitFromFileTest.class, "submitfromfiletest");

    private TestSubmitter submitter = new TestSubmitter();

    @Before
    public void setUp() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(basePath, submitter));
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
        submitter.c = 0;
        worker.run();

        assertEquals(1, submitter.c);
    }

    class TestSubmitter implements IMessageSubmitter {
        public int c = 0;

        @Override
        public String submitMessage(IUserMessage um) throws MessageSubmitException {
            throw new UnsupportedOperationException("This method signature is deprecated");
        }

        @Override
        public String submitMessage(IUserMessage um, boolean deletePayloadFiles) throws MessageSubmitException {
            c++;
            return "new-msg-" + c;
        }

        @Override
        public String submitMessage(IPullRequest pr) throws MessageSubmitException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}