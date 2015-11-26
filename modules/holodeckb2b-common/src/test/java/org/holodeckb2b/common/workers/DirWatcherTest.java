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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.common.workers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.holodeckb2b.common.workerpool.TaskConfigurationException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Sander Fieten <sander@holodeck-b2b.org>
 */
public class DirWatcherTest {
    
    DirWatcherImpl  instance;
    String          basePath;
    File            testDir;
    
    public DirWatcherTest() {
    }
    
    @Before
    public void setUp() {
        instance = new DirWatcherImpl();
        basePath = this.getClass().getClassLoader().getResource("dirwatcher").getPath();
        
        testDir = new File(basePath + "/checkdir");
        
        try {
            FileUtils.deleteDirectory(testDir);
            FileUtils.copyDirectory(new File(basePath + "/clean"), testDir);
        } catch (IOException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        basePath += "/checkdir";
    }
    
    @After
    public void tearDown() {       
        try {
            FileUtils.deleteDirectory(testDir);
        } catch (IOException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test of setParameters method, of class DirWatcher.
     */
    @Test
    public void testSetParameters() {
        System.out.println("setParameters");
        Map<String, String> parameters = new HashMap<String, String>();
        
        parameters.put("watchPath", basePath);
        
        try {
            instance.setParameters(parameters);
        } catch (TaskConfigurationException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Configuration failed");
        }
    }

    /**
     * First test, all files look new 
     */
    @Test
    public void testFirstRun() {
        
        testSetParameters();
        instance.run();
        
        assertEquals(4, instance.c);               
    }

    /**
     * Second test, add a file
     */
    @Test
    public void testAddToEnd() {
        
        testSetParameters();
        instance.run();
        
        assertEquals(4, instance.c);               
        
        String opath = basePath + "/ignore-me/Foto-5.JPG";
        String npath = basePath + "/Foto-5.JPG";
        
        try {
            FileUtils.copyFile(new File(opath), new File(npath));
        } catch (IOException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        instance.run();
        assertEquals(1, instance.c);               
    }

    /**
     * Delete first
     */
    @Test
    public void testDeleteFirst() {
        
        testSetParameters();
        instance.run();
        
        assertEquals(4, instance.c);               
        
        String dpath = basePath + "/Foto-1.jpg";
        
        new File(dpath).delete();
        
        instance.run();
        assertEquals(1, instance.c);               
    }    
    
    /**
     * Delete second, add to end
     */
    @Test
    public void testDeleteAndAdd() {
        
        testSetParameters();
        instance.run();
        
        assertEquals(4, instance.c);               
        
        String dpath = basePath + "/Foto-2.jpg";
        
        new File(dpath).delete();
        
        String opath = basePath + "/ignore-me/Foto-5.JPG";
        String npath = basePath + "/Foto-5.JPG";
        try {
            FileUtils.copyFile(new File(opath), new File(npath));
        } catch (IOException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        instance.run();
        assertEquals(2, instance.c);               
    }    
    
    public class DirWatcherImpl extends DirWatcher {
        public int c = 0;
        
        @Override
        public void run() {
            c = 0;
            super.run();
        }
        
        @Override
        public void onChange(File f, Event event) {
            System.out.println("Change [" + event.name() + "] reported for " + f.getAbsolutePath());
            c++;
        }
    }
}
