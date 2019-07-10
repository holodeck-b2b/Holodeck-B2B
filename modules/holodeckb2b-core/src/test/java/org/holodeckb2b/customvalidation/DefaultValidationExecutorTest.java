package org.holodeckb2b.customvalidation;

import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.CustomValidationConfiguration;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError.Severity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
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
    	PMode pmode = TestUtils.create1WaySendPushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
        UserMessageFlow flow = new UserMessageFlow();
        CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
        validationSpec.setStopSeverity(Severity.Info);
        validationSpec.setRejectSeverity(Severity.Info);
        flow.setCustomValidationConfiguration(validationSpec);
        leg.setUserMessageFlow(flow);
        HolodeckB2BCore.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());
        validationExecutor.validate(userMessage, validationSpec);
    }
}