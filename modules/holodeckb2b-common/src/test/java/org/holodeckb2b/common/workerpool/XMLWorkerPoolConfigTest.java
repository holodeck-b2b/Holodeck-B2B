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

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.common.workerpool.xml.XMLWorkerPoolConfig;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests correct loading of XML configuration for the worker pool
 *
 * @author Sander Fieten <sander@holodeck-b2b.org>
 */
public class XMLWorkerPoolConfigTest {

    public XMLWorkerPoolConfigTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }


    /**
     * Test of loadFromFile method, of class XMLWorkerPoolConfig.
     *
     * <p>Loads <i>wp_config1.xml</i> which contains the following correct pool configuration:
     * <pre>
          <?xml version="1.0" encoding="UTF-8"?>
          <workers xmlns="http://www.holodeck-b2b.org/2012/12/workers"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.holodeck-b2b.org/2012/12/workers file:/Users/safi/Development/holodeck-b2b-1.1-b/config/workers.xsd" poolName="correctpool">
              <worker name="worker1" activate="false" workerClass="workerClass1">
              </worker>
              <worker name="worker2" activate="true" workerClass="workerClass2" concurrent="5" interval="30">
              </worker>
          </workers>
     * </pre>
     *
     */
    @Test
    public void testLoadCorrectFile() {
        System.out.println("loadFromFile");
        final String path = TestUtils.getPath(this.getClass(), "workerpoolcfg/wp_config1.xml");

        final IWorkerPoolConfiguration result = XMLWorkerPoolConfig.loadFromFile(path);

        assertNotNull(result);
        assertEquals("correctpool", result.getName());

        final List<IWorkerConfiguration> workers = result.getWorkers();
        assertEquals(2, workers.size());

        assertFalse(workers.get(0).activate());
        assertEquals("workerClass1", workers.get(0).getWorkerTask());

        final Map<String, ?> params = workers.get(0).getTaskParameters();

        assertEquals(4, params.size());
        final HashSet<String> keys = new HashSet<String>() {{ add("p1"); add("p2"); add("p3"); add("p4"); }};
        final HashSet<String> values = new HashSet<String>() {{ add("value1"); add("value2"); add("value3"); add("value4"); }};
        for (final String k : params.keySet()) {
            assertTrue(keys.contains(k));
            assertTrue(((String) params.get(k)).endsWith(k.substring(1)));
        }

    }

    /**
     * Test of loadFromFile method, of class XMLWorkerPoolConfig.
     *
     * <p>Loads <i>wp_config1.xml</i> which contains the following invalid pool configuration:
     * <pre>
<?xml version="1.0" encoding="UTF-8"?>
<workers xmlns="http://www.holodeck-b2b.org/2012/12/workers"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://www.holodeck-b2b.org/2012/12/workers file:/Users/safi/Development/holodeck-b2b-1.1-b/config/workers.xsd" poolName="invalidcfg">
    <worker name="name1" activate="false">
    </worker>
    <worker activate="false" workerClass="workerClass3">
    </worker>
</workers>
     * </pre>
     *
     */
    @Test
    public void testLoadInvalidFile() {
        System.out.println("loadFromFile");
        final String path = TestUtils.getPath(this.getClass(), "workerpoolcfg/wp_config2.xml");

        final IWorkerPoolConfiguration result = XMLWorkerPoolConfig.loadFromFile(path);

        assertNull(result);
    }

    /**
     * Test of loadFromFile method, of class XMLWorkerPoolConfig.
     *
     * <p>Loads <i>wp_config1.xml</i> which contains the following correct pool configuration:
     * <pre>
<?xml version="1.0" encoding="UTF-8"?>
<workers xmlns="http://www.holodeck-b2b.org/2012/12/workers"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://www.holodeck-b2b.org/2012/12/workers file:/Users/safi/Development/holodeck-b2b-1.1-b/config/workers.xsd">
    <worker name="name1" activate="false" workerClass="workerClass1">
    </worker>

</workers>
     * </pre>
     *
     */
    @Test
    public void testLoadUnnamedFile() {
        System.out.println("loadFromFile");
        final String path = TestUtils.getPath(this.getClass(), "workerpoolcfg/wp_config3.xml");

        final IWorkerPoolConfiguration result = XMLWorkerPoolConfig.loadFromFile(path);

        assertNotNull(result);
        assertEquals("wp_config3", result.getName());

        final List<IWorkerConfiguration> workers = result.getWorkers();
        assertEquals(1, workers.size());

        assertFalse(workers.get(0).activate());
        assertEquals("name1", workers.get(0).getName());
        assertEquals("workerClass1", workers.get(0).getWorkerTask());

        final Map<String, ?> params = workers.get(0).getTaskParameters();

        assertNull(params);

    }

}
