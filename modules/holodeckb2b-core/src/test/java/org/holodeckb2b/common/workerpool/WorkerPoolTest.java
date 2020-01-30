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

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.common.workerpool.xml.XMLWorkerPoolConfig;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.junit.Test;

/**
 * Tests the {@link WorkerPool}
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
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
        Path path = TestUtils.getPath("workerpool/wp_create.xml");

        final IWorkerPoolConfiguration cfg = XMLWorkerPoolConfig.loadFromFile(path);
        System.out.println("Create the pool");

        final WorkerPool pool = new WorkerPool(cfg);

        try {
            Thread.sleep(5000);

            System.out.println("Reconfigure the pool");

            path = TestUtils.getPath("workerpool/wp_reconf.xml");
            pool.setConfiguration(XMLWorkerPoolConfig.loadFromFile(path));

            Thread.sleep(5000);

        } catch (final InterruptedException ex) {
            Logger.getLogger(WorkerPoolTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        pool.stop(10);

    }
}
