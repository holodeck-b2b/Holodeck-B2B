package org.holodeckb2b.common.workers;

import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created at 23:08 16.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PathWatcherTest {

    @Test
    public void testSetParameters() {
        String baseDir =
                PathWatcherTest.class.getClassLoader().getResource("workers").getPath();
        PathWatcherImpl watcher = new PathWatcherImpl();
        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", baseDir);
        try {
            watcher.setParameters(params);
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
        assertEquals(baseDir, watcher.getWatchPath());
    }

    class PathWatcherImpl extends PathWatcher {

        @Override
        protected void onChange(File f, Event event) {
        }

        @Override
        protected File[] getFileList() {
            return new File[0];
        }

        public String getWatchPath() {
            return watchPath;
        }
    }
}