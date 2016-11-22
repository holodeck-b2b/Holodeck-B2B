package org.holodeckb2b.module;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created at 21:30 19.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class HolodeckB2BCoreImplTest {
    private static URL repoUrl;

    private static ConfigurationContext cc;
    private static AxisModule am;

    @BeforeClass
    public static void setUpClass() {
        repoUrl = HolodeckB2BCoreImplTest.class.getClassLoader()
                .getResource("moduletest/repository");

        AxisConfiguration ac = new AxisConfiguration();
        ac.setRepository(repoUrl);

        cc = new ConfigurationContext(ac);
        am = new AxisModule();
        am.setName(HolodeckB2BCoreImpl.HOLODECKB2B_CORE_MODULE);
    }

    @Test
    public void testInit() {
        HolodeckB2BCoreImpl coreImpl = new HolodeckB2BCoreImpl();
        try {
            coreImpl.init(cc, am);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }
        assertNotNull(coreImpl.getPModeSet());
    }
}