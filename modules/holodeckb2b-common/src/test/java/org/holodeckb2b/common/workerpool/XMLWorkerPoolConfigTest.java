/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.common.workerpool;

import org.holodeckb2b.common.workerpool.xml.XMLWorkerPoolConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.workerpool.IWorkerConfiguration;
import org.holodeckb2b.common.workerpool.IWorkerPoolConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
        String path = this.getClass().getClassLoader().getResource("workerpoolcfg/wp_config1.xml").getPath();
        
        IWorkerPoolConfiguration result = XMLWorkerPoolConfig.loadFromFile(path);
        
        assertNotNull(result);
        assertEquals("correctpool", result.getName());
        
        List<IWorkerConfiguration> workers = result.getWorkers();
        assertEquals(2, workers.size());
        
        assertFalse(workers.get(0).activate());
        assertEquals("workerClass1", workers.get(0).getWorkerTask());
        
        Map<String, ?> params = workers.get(0).getTaskParameters();
        
        assertEquals(4, params.size());
        HashSet<String> keys = new HashSet<String>() {{ add("p1"); add("p2"); add("p3"); add("p4"); }};
        HashSet<String> values = new HashSet<String>() {{ add("value1"); add("value2"); add("value3"); add("value4"); }};
        for (String k : params.keySet()) {
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
        String path = this.getClass().getClassLoader().getResource("workerpoolcfg/wp_config2.xml").getPath();
        
        IWorkerPoolConfiguration result = XMLWorkerPoolConfig.loadFromFile(path);
        
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
        String path = this.getClass().getClassLoader().getResource("workerpoolcfg/wp_config3.xml").getPath();
        
        IWorkerPoolConfiguration result = XMLWorkerPoolConfig.loadFromFile(path);
        
        assertNotNull(result);
        assertEquals("wp_config3", result.getName());
        
        List<IWorkerConfiguration> workers = result.getWorkers();
        assertEquals(1, workers.size());
        
        assertFalse(workers.get(0).activate());
        assertEquals("name1", workers.get(0).getName());
        assertEquals("workerClass1", workers.get(0).getWorkerTask());
        
        Map<String, ?> params = workers.get(0).getTaskParameters();
        
        assertNull(params);
        
    }

}
