/**
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
package org.holodeckb2b.common.workers;

import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.junit.After;
import org.junit.Before;
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
    String baseDir;

    @Before
    public void setUp() {
        baseDir = TestUtils.getPath(this.getClass(), "workers");
    }

    @After
    public void tearDown() {
        baseDir = null;
    }

    @Test
    public void testSetParameters() {
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