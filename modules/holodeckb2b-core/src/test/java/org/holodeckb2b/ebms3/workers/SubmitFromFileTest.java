package org.holodeckb2b.ebms3.workers;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.testhelpers.HolodeckCore;
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

    private String basePath =
            SubmitFromFileTest.class.getClassLoader().getResource("submitfromfiletest").getPath();

    private static SubmitFromFileImpl worker;

    @Before
    public void setUp() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(basePath));
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