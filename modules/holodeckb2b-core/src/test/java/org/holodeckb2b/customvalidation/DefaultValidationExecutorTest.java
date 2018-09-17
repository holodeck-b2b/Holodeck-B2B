package org.holodeckb2b.customvalidation;

import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.helpers.CustomValidationSpec;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.UserMessageFlow;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 17:14 24.06.17
 *
 * todo complete the test
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultValidationExecutorTest {

    private static String baseDir;

    private static org.holodeckb2b.module.HolodeckB2BTestCore core;

    private DefaultValidationExecutor validationExecutor;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = DefaultValidationExecutorTest.class.getClassLoader()
                .getResource("customvalidation").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        validationExecutor = new DefaultValidationExecutor();
    }

    @After
    public void tearDown() throws Exception {
        core.getPModeSet().removeAll();
    }

    @Test
    public void testValidate() throws Exception {
        PMode pMode = new PMode();
        Leg leg = new Leg();
        UserMessageFlow flow = new UserMessageFlow();
        CustomValidationSpec validationSpec = new CustomValidationSpec("info", "info");
        flow.setCustomValidationConfiguration(validationSpec);
        leg.setUserMessageFlow(flow);
        pMode.addLeg(leg);
        pMode.setId("some_id");
        core.getPModeSet().add(pMode);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pMode.getId());
        validationExecutor.validate(userMessage, validationSpec);
    }
}