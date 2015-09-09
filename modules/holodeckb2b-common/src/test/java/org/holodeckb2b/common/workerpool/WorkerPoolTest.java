/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.common.workerpool;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.holodeckb2b.common.workerpool.xml.XMLWorkerPoolConfig;
import org.junit.Test;

/**
 * Tests the {@link WorkerPool} 
 * 
 * @author Sander Fieten <sander@holodeck-b2b.org>
 */
public class WorkerPoolTest {
    
    public WorkerPoolTest() {
    }

    /**
     * Test creation of the pool
     */
//    @Test
//    public void testPool() {
//        String path = this.getClass().getClassLoader().getResource("workerpool/wp_create.xml").getPath();
//        
//        IWorkerPoolConfiguration cfg = XMLWorkerPoolConfig.loadFromFile(path);
//        System.out.println("Create the pool");
//        
//        WorkerPool pool = new WorkerPool(cfg);
//        
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(WorkerPoolTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        pool.stop(10);
//        
//    }
    
    /**
     * Test reconfiguration of the pool
     */
    @Test
    public void testReconfPool() {
        String path = this.getClass().getClassLoader().getResource("workerpool/wp_create.xml").getPath();
        
        IWorkerPoolConfiguration cfg = XMLWorkerPoolConfig.loadFromFile(path);
        System.out.println("Create the pool");
        
        WorkerPool pool = new WorkerPool(cfg);
        
        try {
            Thread.sleep(5000);
            
            System.out.println("Reconfigure the pool");
            
            path = this.getClass().getClassLoader().getResource("workerpool/wp_reconf.xml").getPath();
            pool.setConfiguration(XMLWorkerPoolConfig.loadFromFile(path));
            
            Thread.sleep(5000);
            
        } catch (InterruptedException ex) {
            Logger.getLogger(WorkerPoolTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        pool.stop(10);
        
    }
}
