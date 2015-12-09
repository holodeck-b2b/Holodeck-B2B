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

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.holodeckb2b.common.workerpool.TaskConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class FileWatcherTest {
    
    FileWatcherImpl instance;
    
    String testFile = null;
    
    public FileWatcherTest() {
    }
    
    @Before
    public void setUp() {
        instance = new FileWatcherImpl();
        String basePath = this.getClass().getClassLoader().getResource("filewatcher").getPath();
        testFile = basePath + "/testfile.tst";
        
        Map<String, String> param = new HashMap<String, String>();
        param.put("watchPath", testFile);
        
        try {
            instance.setParameters(param);
        } catch (TaskConfigurationException ex) {
            Logger.getLogger(FileWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        
        try {
            File f = new File(testFile);
            f.delete();
        } catch (Exception e ) {
        }
    }

    /**
     * Test that no change is reported when the file is not available
     */
    @Test
    public void testFileNotExisting() {
        instance.run();
        assertNull(instance.e);        
    }

    /**
     * Test of file creation
     */
    @Test
    public void testFileCreated() {
        instance.run();
        assertNull(instance.e);
        
        try {
            File f = new File(testFile);
            FileWriter fo = new FileWriter(f);
            fo.write("Just a test");
            fo.close();
            
            instance.run();
            
            assertEquals(PathWatcher.Event.ADDED, instance.e);
        
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    /**
     * Test of file deletion
     */
    @Test
    public void testFileDeleted() {
        instance.run();
        assertNull(instance.e);
        
        try {
            File f = new File(testFile);
            FileWriter fo = new FileWriter(f);
            fo.write("Just a test");
            fo.close();
            
            instance.run();
            assertEquals(PathWatcher.Event.ADDED, instance.e);
        
            f.delete();
            
            instance.run();
            assertEquals(PathWatcher.Event.REMOVED, instance.e);
        
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test of file change
     */
    @Test
    public void testFileChanged() {
        instance.run();
        assertNull(instance.e);
        
        try {
            File f = new File(testFile);
            FileWriter fo = new FileWriter(f);
            fo.write("Just a test");
            fo.close();
            
            instance.run();
            assertEquals(PathWatcher.Event.ADDED, instance.e);
            
            Thread.sleep(1000); // This sleep is necessary because changes might otherwise not be detected!
            
            fo = new FileWriter(f);
            fo.write("Just a test 2");
            fo.close();
            
            instance.run();
            assertEquals(PathWatcher.Event.CHANGED, instance.e);
        
            f.delete();
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    
    public class FileWatcherImpl extends FileWatcher {
        
        Event   e = null;
        
        @Override
        public void run() {
            e = null;
            
            super.run();
        }
        
        @Override
        protected void onChange(File f, Event event) {
            e = event;
        }
        
    }
    
}
