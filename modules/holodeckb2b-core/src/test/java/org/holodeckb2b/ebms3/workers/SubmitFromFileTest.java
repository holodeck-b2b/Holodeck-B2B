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

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Random;

import org.codehaus.plexus.util.FileUtils;
import org.holodeckb2b.common.testhelpers.Submitter;
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

    private static final String basePath = SubmitFromFileTest.class.getClassLoader()
    												  .getResource(SubmitFromFileTest.class.getSimpleName()).getPath();

    private static final String watchPath = basePath + "/test";
    
    private int numOfMMDs = 50;
    
    private int renamed = 0;
    
    @Before
    public void setUp() throws Exception {
    	HolodeckB2BTestCore core = new HolodeckB2BTestCore(basePath);
        HolodeckB2BCoreInterface.setImplementation(core);

        File submitDir = new File(watchPath);
        if (submitDir.exists())
        	FileUtils.deleteDirectory(submitDir);
        
        FileUtils.mkdir(watchPath);
        File mmd = new File(basePath + "/test.mmd");
        
        //numOfMMDs = new Random().nextInt(30);
        for(int i = 0; i < numOfMMDs; i++) {
        	File submission = new File(watchPath + "/test" + i + ".mmd");
        	FileUtils.copyFile(mmd, submission);
        }
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSingleWorker() {
        SubmitFromFile worker = new SubmitFromFile();

        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", watchPath);
        try {
            worker.setParameters(params);
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
        
        worker.run();

        assertEquals(numOfMMDs, ((Submitter) HolodeckB2BCoreInterface.getMessageSubmitter()).getAllSubmitted().size());
    }
    
    @Test
    public void testMultipleWorkers() {
        
    	final int numOfWorkers = Math.max(1, new Random().nextInt(5));    	
    	
        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", watchPath);
        
        final Thread[] workers = new Thread[numOfWorkers + 1];
        try {
            for (int i = 0; i < numOfWorkers; i++) {
            	SubmitFromFile worker = new SubmitFromFile();
            	worker.setParameters(params);
            	workers[i] =  new Thread(worker);
            }        	            
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
                
        renamed = 0;
        workers[numOfWorkers] = new Thread(new Runnable() {
			public void run() {
				final File[] mmdFiles = new File(watchPath).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(final File file) {
                        return file.isFile() && file.getName().toLowerCase().endsWith(".mmd");
                    }
                });
				
				for (int i = mmdFiles.length; i > 0; i--) {
					final String mmdFileName = mmdFiles[i-1].getAbsolutePath();				
					final File tmpFile = new File(mmdFileName.substring(0, mmdFileName.toLowerCase().indexOf(".mmd"))
															 + ".processing");
		            if(mmdFiles[i-1].exists() && !tmpFile.exists()) {
		            	mmdFiles[i-1].renameTo(tmpFile);
		            	renamed++;
		            }
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        try {
    		for (final Thread w : workers)  
    			w.start();    			    		
			for (final Thread w : workers)
				w.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("Not all workers finished!");
		}
        assertEquals(numOfMMDs - renamed, ((Submitter) HolodeckB2BCoreInterface.getMessageSubmitter()).getAllSubmitted().size());
    }
    
}
