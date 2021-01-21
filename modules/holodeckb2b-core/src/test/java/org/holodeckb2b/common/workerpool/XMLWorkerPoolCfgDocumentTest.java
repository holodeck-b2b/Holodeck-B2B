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
package org.holodeckb2b.common.workerpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.junit.Test;

/**
 * Tests correct loading of XML configuration for the worker pool
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class XMLWorkerPoolCfgDocumentTest {

    @Test
    public void testCompleteConfig() {
        final Path path = TestUtils.getPath("workerpoolcfg/wp_complete.xml");

        XMLWorkerPoolCfgDocument result = null;
		try {
			result = XMLWorkerPoolCfgDocument.readFromFile(path);
		} catch (Exception e1) {
			fail();
		}

        assertNotNull(result);
        assertEquals(new Integer(5), result.getRefreshInterval());
        
        final List<IWorkerConfiguration> workers = result.getWorkers();
        assertEquals(2, workers.size());

        assertFalse(workers.get(0).activate());
        assertEquals("workerClass1", workers.get(0).getWorkerTask());

        final Map<String, ?> params = workers.get(0).getTaskParameters();

        assertEquals(4, params.size());
        final HashMap<String, String> expected = new HashMap<String, String>() {{ put("p1", "value1"); 
        																	  	  put("p2", "value2"); 
        																	  	  put("p3", "value3"); 
        																	  	  put("p4", "value4"); }};
        params.entrySet().parallelStream().allMatch(e -> expected.containsKey(e.getKey()) && 
        													expected.get(e.getKey()).equals(e.getValue()));
    }
    
    @Test
    public void testEmptyFile() {
    	final Path path = TestUtils.getPath("workerpoolcfg/wp_empty.xml");
    	
        XMLWorkerPoolCfgDocument result = null;
		try {
			result = XMLWorkerPoolCfgDocument.readFromFile(path);
    	} catch (Exception e) {
    		fail();
    	}     
    	
    	assertNotNull(result);
    	assertNull(result.getRefreshInterval());
    	assertNotNull(result.getWorkers());
    	assertTrue(result.getWorkers().isEmpty());
    }

    @Test
    public void testInvalidFile() {
        final Path path = TestUtils.getPath("workerpoolcfg/wp_invalid.xml");

        try {
			XMLWorkerPoolCfgDocument.readFromFile(path);
			fail();
		} catch (Exception e) {
		}        
    } 
}
