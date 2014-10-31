/*
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

package org.holodeckb2b.ebms3.pulling;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.holodeckb2b.common.util.Interval;
import org.holodeckb2b.common.workerpool.IWorkerConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PullConfigurationTest {
    
    public PullConfigurationTest() {
    }

//    @Test
//    public void testGetName() {
//    }
//
//    @Test
//    public void testGetWorkers() {
//    }

    /**
     * 
     */
    @Test
    public void testLoad_DefaultOnly() {
        String path = this.getClass().getClassLoader().getResource("pulltest/pullcfg1.xml").getPath();
        File   f = new File(path);

        try {
            PullConfiguration pullCfg = PullConfiguration.loadFromFile(path);

            assertNotNull(pullCfg);
            List<IWorkerConfiguration> workers = pullCfg.getWorkers();
            
            assertEquals(1, workers.size());
            assertEquals(new Interval(1826646350, TimeUnit.SECONDS), workers.get(0).getInterval());
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    /**
     * 
     */
    @Test
    public void testLoad_CompleteConfig() {
        String path = this.getClass().getClassLoader().getResource("pulltest/pullcfg2.xml").getPath();
        File   f = new File(path);
        
        try {
            PullConfiguration pullCfg = PullConfiguration.loadFromFile(path);
            
            assertNotNull(pullCfg);
            List<IWorkerConfiguration> workers = pullCfg.getWorkers();
            
            assertEquals(3, workers.size());
            
            assertEquals(new Interval(1985496162, TimeUnit.SECONDS), workers.get(0).getInterval());
            Map<String,?> params = (Map<String, ?>) workers.get(0).getTaskParameters();
            assertFalse(params.isEmpty());
            assertTrue((Boolean) params.get(PullWorker.PARAM_INCLUDE));
            Collection<String> pmodes = (Collection<String>) params.get(PullWorker.PARAM_PMODES);
            assertEquals(1 , pmodes.size());
            assertTrue(pmodes.contains("LO7.wrubhpHfZ9Kzr3buPpHMVI"));
            
            assertEquals(new Interval(1622097273, TimeUnit.SECONDS), workers.get(1).getInterval());
            params = (Map<String, ?>) workers.get(1).getTaskParameters();
            assertFalse(params.isEmpty());
            assertTrue((Boolean) params.get(PullWorker.PARAM_INCLUDE));
            pmodes = (Collection<String>) params.get(PullWorker.PARAM_PMODES);
            assertEquals(2 , pmodes.size());
            assertTrue(pmodes.contains("INyEfTQuibb8t"));
            assertTrue(pmodes.contains("Qw1hfHh"));

            params = (Map<String, ?>) workers.get(2).getTaskParameters();
            assertFalse(params.isEmpty());
            assertFalse((Boolean) params.get(PullWorker.PARAM_INCLUDE));
            pmodes = (Collection<String>) params.get(PullWorker.PARAM_PMODES);
            assertEquals(3 , pmodes.size());
            assertTrue(pmodes.contains("LO7.wrubhpHfZ9Kzr3buPpHMVI"));
            assertTrue(pmodes.contains("INyEfTQuibb8t"));
            assertTrue(pmodes.contains("Qw1hfHh"));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }        
    }
    
    /**
     * 
     */
    @Test
    public void testLoad_ErrorConfig() {
        String path = this.getClass().getClassLoader().getResource("pulltest/pullcfg3.xml").getPath();
        File   f = new File(path);
        
        try {
            PullConfiguration pullCfg = PullConfiguration.loadFromFile(path);
            
            assertNull(pullCfg);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }        
    }
    
    
}
