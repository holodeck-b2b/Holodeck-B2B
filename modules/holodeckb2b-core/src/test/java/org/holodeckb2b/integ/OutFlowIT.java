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
package org.holodeckb2b.integ;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created at 13:45 06.11.16
 *
 * This test takes more time than junit tests
 * That's why it is excluded from the tests during project compilation.
 * todo check this
 * To execute this integration test one should run <code>mvn integration-test</code>
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class OutFlowIT {
    private static ITHelper itHelper;

    private static String dADirName = "HolodeckB2B-A";
    private static String dBDirName = "HolodeckB2B-B";

    private static final Logger log = LogManager.getLogger(OutFlowIT.class);

    @BeforeClass
    public static void setUpClass() {
        itHelper = new ITHelper();
        // delete distr dirs if they exist (if test was stopped, for instance)
        itHelper.deleteDistDir(dADirName);
        itHelper.deleteDistDir(dBDirName);

        itHelper.unzipHolodeckDistribution(dADirName);
        itHelper.unzipHolodeckDistribution(dBDirName);
        itHelper.copyPModeDescriptor(dADirName, "ex-pm-push-init.xml");
        itHelper.copyPModeDescriptor(dBDirName, "ex-pm-push-resp.xml");
        itHelper.modifyAxisServerPort(dBDirName, "9090");
        itHelper.startHolodeckB2BInstances(dADirName, dBDirName);
        itHelper.copyExampleDataToMsgOutDir(dADirName);
    }

    @AfterClass
    public static void tearDownClass() {
        itHelper.stopHolodeckB2BInstances();
        itHelper.deleteDistDir(dADirName);
        itHelper.deleteDistDir(dBDirName);
    }

    @Test
    public void testOneWayPush() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(itHelper.changeMsgExtensionToMMD("ex-mmd-push.accepted",
                dADirName));

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // todo check the results of communication in A/msg_out & A/msg_in & B/msg_in

        // check A/msg_out

        // ex-mmd-push.accepted should be present
        assertTrue(itHelper.fileExistsInDirectory("ex-mmd-push.accepted",
                dADirName + "/data/msg_out"));
        // ex-mmd-push.rejected should not be present
        assertFalse(itHelper.fileExistsInDirectory("ex-mmd-push.rejected",
                dADirName + "/data/msg_out"));
        // ex-mmd-push.err should not be present
        assertFalse(itHelper.fileExistsInDirectory("ex-mmd-push.err",
                dADirName + "/data/msg_out"));

        // todo check B/msg_in

        // message xml and payload file should be present
        assertTrue(itHelper.dirIsNotEmpty(dBDirName + "/data/msg_in"));

        // todo check A/msg_in

        // receipt message xml should be present
        assertTrue(itHelper.dirIsNotEmpty(dADirName + "/data/msg_in"));
    }
}
