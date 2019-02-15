package org.holodeckb2b.customvalidation;

import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.pmode.CustomValidationSpec;
import org.holodeckb2b.common.testhelpers.pmode.Leg;
import org.holodeckb2b.common.testhelpers.pmode.PMode;
import org.holodeckb2b.common.testhelpers.pmode.UserMessageFlow;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
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

    private DefaultValidationExecutor validationExecutor;

    @BeforeClass
    public static void setUpClass() throws Exception {        
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Before
    public void setUp() throws Exception {
        validationExecutor = new DefaultValidationExecutor();
    }

    @After
    public void tearDown() throws Exception {
        HolodeckB2BCore.getPModeSet().removeAll();
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
        HolodeckB2BCore.getPModeSet().add(pMode);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pMode.getId());
        validationExecutor.validate(userMessage, validationSpec);
    }
}