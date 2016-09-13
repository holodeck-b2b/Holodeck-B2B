package org.holodeckb2b.pmode;

import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created at 17:28 09.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeManagerTest {

    private static String baseDir;

    private static PModeManager manager;

    private PMode validPMode;

    @BeforeClass
    public static void setUpClass() {
        baseDir = PModeManagerTest.class.getClassLoader().getResource("pmode_validation").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(baseDir));
        InternalConfiguration initialConf =
                (InternalConfiguration)HolodeckB2BCoreInterface.getConfiguration();
        manager = new PModeManager(initialConf.getPModeValidatorImplClass(),
                initialConf.getPModeStorageImplClass());
    }

    @Before
    public void setUp() throws Exception {
        validPMode = new PMode();
    }

    @After
    public void tearDown() throws Exception {
        validPMode = null;
    }

    @Test
    public void testManagerCreation() {
        assertNotNull(manager);
    }

    @Test
    public void testPModeAddReplace() {
        String pModeId = "123";
        validPMode.setId(pModeId);
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        validPMode.addLeg(new Leg());

        try {
            manager.add(validPMode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

        try {
            manager.replace(validPMode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

        assertTrue(manager.containsId("123"));
    }
}