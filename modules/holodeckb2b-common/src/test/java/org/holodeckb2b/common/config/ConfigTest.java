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
package org.holodeckb2b.common.config;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created at 12:10 17.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ConfigTest {

    private static URL repoUrl;
    private static ConfigurationContext cc;

    /**
     * The default configuration of HolodeckB2B distribution is added to
     * src/test/resources/cfg dir to test config initialization.
     */
    @BeforeClass
    public static void setUpClass() {
        repoUrl = ConfigTest.class.getClassLoader().getResource("cfg/repo");
        AxisConfiguration ac = new AxisConfiguration();
        ac.setRepository(repoUrl);
        cc = new ConfigurationContext(ac);
    }

    @Test
    public void testConfigInit() {
        Config config = null;
        try {
            config = new Config(cc);
            assertNotNull(config);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // testing the presence of required parameter
        assertNotNull(config.getPersistencyUnit());

        // testing the presence of default parameters
        assertNotNull(config.getAxisConfigurationContext());
        assertNotNull(config.getHostName());
        assertNotNull(config.getHolodeckB2BHome());
        assertNotNull(config.getWorkerPoolCfgFile());
        assertNotNull(config.getTempDirectory());

        assertNotNull(config.getPrivateKeyStorePath());
        assertNotNull(config.getPrivateKeyStorePassword());
        assertNotNull(config.getPublicKeyStorePath());
        assertNotNull(config.getPublicKeyStorePassword());
        assertNotNull(config.getTrustKeyStorePath());
        assertNotNull(config.getTrustKeyStorePassword());
    }
}