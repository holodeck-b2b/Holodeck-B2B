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
package org.holodeckb2b.core.workerpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link WorkerPool}
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class WorkerPoolTest {
	
	@BeforeClass
	public static void logStart() {
		System.out.println("Starting WorkerPoolTest, takes longer time, please wait....");
	}
	
	@AfterClass
	public static void logEnd() {
		System.out.println("WorkerPoolTest completed");
	}
	
	@Test
	public void testEmpty() throws WorkerPoolException, InterruptedException {
		
		WorkerPool pool = new WorkerPool("test", new WorkerPoolTestConfiguration());	
		pool.start();
		assertFalse(pool.isRunning());
				
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);
		pool = new WorkerPool("test", configuration);
		
		pool.start();
		assertTrue(pool.isRunning());
		assertTrue(pool.getCurrentWorkers().isEmpty());
		
		Thread.sleep(1100);		
		assertTrue(configuration.reloaded > 0);
		
		pool.shutdown(1);		
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());
	}

	@Test
	public void testSingleRun() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration();		
		WorkerTestConfig workerCfg = new WorkerTestConfig("singlerun", reporter, null);		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		
		pool.start();		
		assertTrue(pool.isRunning());
		
		Thread.sleep(1000);
		
		assertTrue(reporter.workerRuns.containsKey("singlerun.0"));
		assertEquals(1, reporter.workerRuns.get("singlerun.0").intValue());

		pool.shutdown(1);		
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());		
	}
	
	@Test
	public void testDelayedRun() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration();
		
		WorkerTestConfig workerCfg = new WorkerTestConfig("delayed", reporter, null);
		workerCfg.delay = 750;		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		
		pool.start();		
		assertTrue(pool.isRunning());
		
		assertFalse(reporter.workerRuns.containsKey("delayed.0"));				
		Thread.sleep(400);
		assertFalse(reporter.workerRuns.containsKey("delayed.0"));						
		Thread.sleep(400);
		assertTrue(reporter.workerRuns.containsKey("delayed.0"));		
		
		pool.shutdown(1);			
		Thread.sleep(1100);	
		assertFalse(pool.isRunning());		
	}
	
	@Test
	public void testRepeatedRun() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration();		
		WorkerTestConfig workerCfg = new WorkerTestConfig("multirun", reporter, 
															new Interval(500, TimeUnit.MILLISECONDS));		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		
		pool.start();		
		assertTrue(pool.isRunning());
		
		Thread.sleep(1100);		
		assertTrue(reporter.workerRuns.containsKey("multirun.0"));
		int runs = reporter.workerRuns.get("multirun.0").intValue();
		assertTrue(runs >= 2);
		
		pool.shutdown(1);			
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());
		assertTrue(reporter.workerRuns.get("multirun.0").intValue() < runs + 3);		
	}
	
	@Test
	public void testAddWorker() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);		
		WorkerTestConfig workerCfg = new WorkerTestConfig("original", reporter, 
															new Interval(500, TimeUnit.MILLISECONDS));
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);

		pool.start();		
		assertTrue(pool.isRunning());
		
		Thread.sleep(600);		
		assertTrue(reporter.workerRuns.containsKey("original.0"));
		
		WorkerTestConfig addedCfg = new WorkerTestConfig("added", reporter, new Interval(200, TimeUnit.MILLISECONDS));
		configuration.configs.add(addedCfg);
		
		Thread.sleep(600);				
		assertEquals(1, configuration.reloaded);
		
		assertTrue(reporter.workerRuns.containsKey("added.0"));
		
		pool.shutdown(1);			
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());			
	}

	@Test
	public void testRemoveWorker() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configurator = new WorkerPoolTestConfiguration(1);		
		WorkerTestConfig workerCfg = new WorkerTestConfig("first", reporter, new Interval(500, TimeUnit.MILLISECONDS));
		WorkerTestConfig togoCfg = new WorkerTestConfig("togo", reporter, new Interval(5, TimeUnit.SECONDS));		
		configurator.configs.add(workerCfg);
		configurator.configs.add(togoCfg);
		
		WorkerPool pool = new WorkerPool("test", configurator);
		
		pool.start();		
		assertTrue(pool.isRunning());
		
		Thread.sleep(600);		
		assertTrue(reporter.workerRuns.containsKey("first.0"));
		assertTrue(reporter.workerRuns.containsKey("togo.0"));
		
		int runs = reporter.workerRuns.get("togo.0").intValue();		
		
		configurator.configs.remove(togoCfg);
		
		Thread.sleep(600);				
		assertEquals(1, configurator.reloaded);
		assertEquals(runs, reporter.workerRuns.get("togo.0").intValue());
		
		pool.shutdown(1);			
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());			
	}
	
	@Test
	public void testRescheduleWorkerCount() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);		
		WorkerTestConfig workerCfg = new WorkerTestConfig("reschedule", reporter, 
															new Interval(500, TimeUnit.MILLISECONDS));		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		
		pool.start();
		assertTrue(pool.isRunning());
		
		workerCfg.concurrent = 2;
		
		Thread.sleep(600);		
		assertTrue(reporter.workerRuns.containsKey("reschedule.0"));
		
		Thread.sleep(600);				
		assertEquals(1, configuration.reloaded);

		reporter.workerRuns.clear();
		Thread.sleep(600);
		assertTrue(reporter.workerRuns.containsKey("reschedule.0"));
		assertTrue(reporter.workerRuns.containsKey("reschedule.1"));
				
		pool.shutdown(1);			
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());			
	}
	
	@Test
	public void testRescheduleWorkerInterval() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);		
		WorkerTestConfig workerCfg = new WorkerTestConfig("reschedule", reporter, 
				new Interval(500, TimeUnit.MILLISECONDS));		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		
		pool.start();
		assertTrue(pool.isRunning());
		
		workerCfg.interval = new Interval(1, TimeUnit.SECONDS);
		
		Thread.sleep(600);		
		assertTrue(reporter.workerRuns.containsKey("reschedule.0"));
		
		Thread.sleep(600);				
		assertEquals(1, configuration.reloaded);
		
		reporter.workerRuns.clear();
		Thread.sleep(1200);
		assertEquals(1, reporter.workerRuns.get("reschedule.0").intValue());
		
		pool.shutdown(1);			
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());			
	}
	
	
	
	@Test
	public void testReconfigWorker() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);
		
		WorkerTestConfig workerCfg = new WorkerTestConfig("reconfigure", reporter, 
															new Interval(500, TimeUnit.MILLISECONDS));
		workerCfg.parameters.put("P1", "some_value");		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		
		pool.start();
		assertTrue(pool.isRunning());
		assertTrue(reporter.workerParams.containsKey("reconfigure.0"));
		assertEquals("some_value", reporter.workerParams.get("reconfigure.0").get("P1"));
        
		workerCfg.parameters.put("P1", "another_value");
		
		Thread.sleep(600);		
		assertTrue(reporter.workerRuns.containsKey("reconfigure.0"));
		
		Thread.sleep(600);				
		assertEquals(1, configuration.reloaded);
		assertEquals("another_value", reporter.workerParams.get("reconfigure.0").get("P1"));
				
		workerCfg.parameters.put("P2", "extra_value");
		Thread.sleep(1100);				
		assertEquals(2, configuration.reloaded);
		assertEquals("extra_value", reporter.workerParams.get("reconfigure.0").get("P2"));
		
		pool.shutdown(1);		
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());		
	}
		
	@Test
	public void testConfigError() throws WorkerPoolException, InterruptedException {
		TaskReporter reporter = new TaskReporter();
		
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);		
		WorkerTestConfig workerCfg = new WorkerTestConfig("failure", reporter, null);
		workerCfg.parameters.put("REJECT", "faulty");
		
		configuration.configs.add(workerCfg);
		
		WorkerPool pool = new WorkerPool("test", configuration);
		try {
			pool.start();
			fail();
		} catch (WorkerPoolException startFailed) {			
			 List<IWorkerConfiguration> failedWorkers = startFailed.getFailedWorkers();
			 assertFalse(Utils.isNullOrEmpty(failedWorkers));
			 assertEquals("failure", failedWorkers.get(0).getName());
		}
		
		assertTrue(pool.isRunning());		
		assertFalse(reporter.workerRuns.containsKey("failure"));	
		
		pool.shutdown(1);		
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());		
	}	
	
	@Test
	public void testStopRefresh() throws WorkerPoolException, InterruptedException {
		WorkerPoolTestConfiguration configuration = new WorkerPoolTestConfiguration(1);
		
		WorkerPool pool = new WorkerPool("refreshstop", configuration);
		
		pool.start();				
		configuration.refreshInterval = -1;

		Thread.sleep(1100);
		assertEquals(1, configuration.reloaded);
				
		Thread.sleep(1100);
		assertEquals(1, configuration.reloaded);

		pool.shutdown(1);		
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());
	}		
	
	@Test
	public void testChangeRefresh() throws WorkerPoolException, InterruptedException {
		WorkerPoolTestConfiguration configurator = new WorkerPoolTestConfiguration(1);
		
		WorkerPool pool = new WorkerPool("refreshchg", configurator);
		pool.start();
		
		configurator.refreshInterval = 2;
		
		Thread.sleep(1100);
		assertEquals(1, configurator.reloaded);		
		
		Thread.sleep(1100);
		assertEquals(1, configurator.reloaded);		
		Thread.sleep(1000);
		assertEquals(2, configurator.reloaded);				
		
		pool.shutdown(1);		
		Thread.sleep(1100);		
		assertFalse(pool.isRunning());		
	}
}
