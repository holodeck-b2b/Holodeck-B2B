/**
 * Copyright (C) 2021 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.workerpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;
import org.junit.Test;

public class XMLWorkerPoolConfigurationTest {

	@Test
	public void testNAonInit() {
		final Path path = TestUtils.getPath("workerpoolcfg").resolve("notthere.xml");

        try {
			new XMLWorkerPoolConfiguration(path);
			fail();
		} catch (WorkerPoolException e1) {			
		}
	}
	
	@Test
	public void testNAonReload() throws IOException {
		final Path path = TestUtils.getPath("workerpoolcfg/wp_complete.xml");
		final Path copy = TestUtils.getPath("workerpoolcfg").resolve("disappearing.xml");
		
		Files.copy(path, copy);
		
		XMLWorkerPoolConfiguration configuration = null;
        try {
			configuration = new XMLWorkerPoolConfiguration(copy);
		} catch (WorkerPoolException e1) {			
			fail();
		}
        assertNotNull(configuration);
        
        Files.delete(copy);
        
        try {
        	assertTrue(configuration.hasConfigChanged(Instant.now()));
        } catch (AssertionError a) {
        	throw a;
        } catch (Throwable t) {
        	fail();
        }
        try {
        	configuration.reload();
        	fail();
        } catch (WorkerPoolException e) {
        }
	}
	
	@Test
	public void testReload() throws IOException {
		final Path org = TestUtils.getPath("workerpoolcfg/wp_complete.xml");
		final Path chg = TestUtils.getPath("workerpoolcfg/wp_empty.xml");
		final Path test = TestUtils.getPath("workerpoolcfg").resolve("test.xml");
		
		Files.copy(org, test);
		
		XMLWorkerPoolConfiguration configuration = null;
        try {
			configuration = new XMLWorkerPoolConfiguration(test);
		} catch (WorkerPoolException e1) {			
			fail();
		}
        assertNotNull(configuration);
        
        Files.copy(chg, test, StandardCopyOption.REPLACE_EXISTING);
        
    	try {
			configuration.reload();
		} catch (WorkerPoolException e) {
			fail();
		}
    	
    	assertTrue(Utils.isNullOrEmpty(configuration.getWorkers()));
    	
    	Files.delete(test);
	}
	
	@Test
	public void testModified() throws IOException {
		final Path path = TestUtils.getPath("workerpoolcfg/wp_complete.xml");
		
		XMLWorkerPoolConfiguration configurator = null;
        try {
			configurator = new XMLWorkerPoolConfiguration(path);
		} catch (WorkerPoolException e1) {			
			fail();
		}
        assertNotNull(configurator);
        
        assertFalse(configurator.hasConfigChanged(Instant.now()));		
        Files.setLastModifiedTime(path, FileTime.from(Instant.now()));        
        assertTrue(configurator.hasConfigChanged(Instant.now().minusSeconds(10)));		
	}

	@Test
	public void testGetConfig() throws IOException {
		final Path path = TestUtils.getPath("workerpoolcfg/wp_complete.xml");
		
		XMLWorkerPoolConfiguration configuration = null;
        try {
			configuration = new XMLWorkerPoolConfiguration(path);
		} catch (WorkerPoolException e1) {			
			fail();
		}
        assertNotNull(configuration);
        assertFalse(Utils.isNullOrEmpty(configuration.getWorkers()));
        assertEquals(5, configuration.getConfigurationRefreshInterval());
	}	

	@Test
	public void testNoRefresh() throws IOException {
		final Path path = TestUtils.getPath("workerpoolcfg/wp_empty.xml");
		
		XMLWorkerPoolConfiguration configuration = null;
		try {
			configuration = new XMLWorkerPoolConfiguration(path);
		} catch (WorkerPoolException e1) {			
			fail();
		}
		assertNotNull(configuration);
		assertEquals(-1, configuration.getConfigurationRefreshInterval());
	}	
}
