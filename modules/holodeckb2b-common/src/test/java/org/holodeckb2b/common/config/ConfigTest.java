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