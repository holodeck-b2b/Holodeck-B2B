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
package org.holodeckb2b.test.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.experimental.categories.Category;

/**
 * Integration test. Tests the One Way Message Exchange Patterns communications
 * between two HolodeckB2B instances.
 *
 * This test takes more time than junit tests
 * That's why it is excluded from the tests during project compilation.
 *
 * To execute this integration test one should run <code>mvn integration-test</code>
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@Category(IntegrationTest.class)
public class OneWayMEPWorkflows {
    private static ITHelper itHelper;

    private static String dADirName = "HolodeckB2B-A";
    private static String dBDirName = "HolodeckB2B-B";

    private static final Logger log = LogManager.getLogger(OneWayMEPWorkflows.class);

    @BeforeClass
    public static void setUpClass() {
        System.out.println("Setting up the OneWay integration test ... ");
        itHelper = new ITHelper();
        // delete distr dirs if they exist (if test was stopped, for instance)
        itHelper.deleteDistDir(dADirName);
        itHelper.deleteDistDir(dBDirName);
        System.out.print("\tUnzipping HolodeckB2B instance ... ");
        itHelper.unzipHolodeckDistribution(dADirName);
        System.out.println("done.");
        System.out.print("\tUnzipping HolodeckB2B instance ... ");
        itHelper.unzipHolodeckDistribution(dBDirName);
        System.out.println("done.");
        System.out.print("\tConfiguring HolodeckB2B instances ... ");
        itHelper.copyPModeDescriptor(dADirName, "ex-pm-push-init.xml");
        itHelper.copyPModeDescriptor(dBDirName, "ex-pm-push-resp.xml");
        itHelper.copyPModeDescriptor(dADirName, "ex-pm-pull-ut-init.xml");
        itHelper.copyPModeDescriptor(dBDirName, "ex-pm-pull-ut-resp.xml");
        itHelper.modifyAxisServerPort(dBDirName, "9090");
        itHelper.setPullingInterval(dADirName, 10);
        System.out.println("done.");
        System.out.print("\tStarting HolodeckB2B instances ... ");
        itHelper.startHolodeckB2BInstances(dADirName, dBDirName);
        System.out.println("done.");
        System.out.println("Setting up the OneWay integration test finished.");
    }

    @AfterClass
    public static void tearDownClass() {
        System.out.print("Cleaning up the OneWay integration test resources ... ");
        itHelper.stopHolodeckB2BInstances();
        itHelper.deleteDistDir(dADirName);
        itHelper.deleteDistDir(dBDirName);
        System.out.println("done.");
    }

    @Before
    public void setUp() {
        // copy messages to be sent
        itHelper.copyExampleDataToMsgOutDir(dADirName);
        itHelper.copyExampleDataToMsgOutDir(dBDirName);
        // for onewaypull without encription we don't need keystores
//        itHelper.copyKeystores(dADirName);
//        itHelper.copyKeystores(dBDirName);
    }

    @After
    public void tearDown() {
        // delete all messages from msg_in & msg_out
        itHelper.clearMsgOutAndMsgInDirs(dADirName);
        itHelper.clearMsgOutAndMsgInDirs(dBDirName);
    }

    @Test
    public void testOneWayPush() {
        String msgFileName = "ex-mmd-push";
        System.out.println("The OneWay/Push integration test started ... ");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(itHelper.changeMsgExtensionToMMD(msgFileName+".accepted",
                dADirName));
        System.out.println("Message to push is placed into msg_out dir."
                + " Waiting for message processing ... ");
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Message should be processed now."
                + " Checking the results ...");

        // check the results of communication in A/msg_out & A/msg_in & B/msg_in

        // check A/msg_out

        // ex-mmd-push.accepted should be present
        assertTrue(itHelper.fileExistsInDirectory(msgFileName+".accepted",
                dADirName + "/data/msg_out"));
        // ex-mmd-push.rejected should not be present
        assertFalse(itHelper.fileExistsInDirectory(msgFileName+".rejected",
                dADirName + "/data/msg_out"));
        // ex-mmd-push.err should not be present
        assertFalse(itHelper.fileExistsInDirectory(msgFileName+".err",
                dADirName + "/data/msg_out"));

        // check B/msg_in

        // message xml and payload file should be present
        assertTrue(itHelper.dirIsNotEmpty(dBDirName + "/data/msg_in"));

        // check A/msg_in

        // receipt message xml should be present
        assertTrue(itHelper.dirIsNotEmpty(dADirName + "/data/msg_in"));
        System.out.println("The OneWay/Push integration test finished.");
    }

    @Test
    public void testOneWayPull() {
        String msgFileName = "ex-mmd-pull-ut";
        System.out.println("The OneWay/Pull integration test started ... ");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(itHelper.changeMsgExtensionToMMD(msgFileName+".accepted",
                dBDirName));
        System.out.println("Message to pull is placed into msg_out dir."
                + " Waiting for message processing ... ");
        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Message should be processed now."
                + " Checking the results ...");

        // check the results of communication in A/msg_out & A/msg_in & B/msg_in

        // check B/msg_out

        // ex-mmd-pull-ut.accepted should be present
        assertTrue(itHelper.fileExistsInDirectory(msgFileName+".accepted",
                dBDirName + "/data/msg_out"));
        // ex-mmd-pull-ut.rejected should not be present
        assertFalse(itHelper.fileExistsInDirectory(msgFileName+".rejected",
                dBDirName + "/data/msg_out"));
        // ex-mmd-pull-ut.err should not be present
        assertFalse(itHelper.fileExistsInDirectory(msgFileName+".err",
                dBDirName + "/data/msg_out"));

        // check A/msg_in

        // message xml and payload file should be present
        assertTrue(itHelper.dirIsNotEmpty(dADirName + "/data/msg_in"));

        // receipt message xml should be present
        assertTrue(itHelper.dirIsNotEmpty(dADirName + "/data/msg_in"));
        System.out.println("The OneWay/Pull integration test finished.");
    }
}
