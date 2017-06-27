package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.customvalidation.helpers.CustomValidator;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.pmode.helpers.ValidatorConfig;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

//import org.holodeckb2b.pmode.xml.Leg;
//import org.holodeckb2b.pmode.xml.UserMessageFlow;


/**
 * Created at 17:39 24.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PerformCustomValidationsTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private PerformCustomValidations handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PerformCustomValidationsTest.class.getClassLoader()
                .getResource("customvalidation").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PerformCustomValidations();
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("customvalidation/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        String pmodeId = "some_pmode_id";
        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);

        // We need to set the PMode id value separately because
        // the agreement pmode & userMessage pmode are different
        // Currently we just set the same value
        userMessage.setPModeId(pmodeId);

        // Setting input message property
        StorageManager updateManager = core.getStorageManager();
        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        Leg leg = new Leg();

        UserMessageFlow flow = new UserMessageFlow();
        CustomValidationSpec validationSpec = new CustomValidationSpec();
        List<IMessageValidatorConfiguration> validators = new ArrayList<>();
        ValidatorConfig vc = new ValidatorConfig();
        vc.setId("some_validator_id");
        vc.setFactory("org.holodeckb2b.customvalidation.helpers.CustomValidator$Factory");
        validators.add(vc);
        validationSpec.setValidators(validators);
        flow.setCustomValidationConfiguration(validationSpec);
        leg.setUserMessageFlow(flow);
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // When validation is successful there should be no error in the message context
        assertNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }
}