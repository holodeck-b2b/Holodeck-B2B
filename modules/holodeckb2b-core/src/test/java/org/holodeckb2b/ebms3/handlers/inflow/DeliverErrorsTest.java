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
package org.holodeckb2b.ebms3.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.helpers.DeliverySpecification;
import org.holodeckb2b.pmode.helpers.ErrorHandlingConfig;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.UserMessageFlow;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:04 15.03.17
 *
 * Checked for cases coverage (24.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DeliverErrorsTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ProcessErrors processErrorsHandler;

    private DeliverErrors handler;

    private UserMessage userMessage;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = DeliverErrorsTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }
    @Before
    public void setUp() throws Exception {
        processErrorsHandler = new ProcessErrors();
        // Executed after org.holodeckb2b.ebms3.handlers.inflow.ProcessErrors handler
        handler = new DeliverErrors();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
        core.getPModeSet().removeAll();
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData userMessage = TestUtils.getMMD("handlers/full_mmd.xml", this);

        String pmodeId = userMessage.getCollaborationInfo().getAgreement().getPModeId();

        PMode pmode = new PMode();
        pmode.setId(pmodeId);

        Leg leg = new Leg();

        UserMessageFlow umFlow = new UserMessageFlow();
        ErrorHandlingConfig errorHandlingConfig = new ErrorHandlingConfig();
        errorHandlingConfig.setNotifyErrorToBusinessApplication(true);
        DeliverySpecification deliverySpecification = new DeliverySpecification();
        deliverySpecification.setFactory(NullDeliveryMethod.class.getName());
        deliverySpecification.setId("delivery_spec_id");
        errorHandlingConfig.setErrorDelivery(deliverySpecification);
        umFlow.setErrorHandlingConfiguration(errorHandlingConfig);

        leg.setUserMessageFlow(umFlow);
        pmode.addLeg(leg);

        core.getPModeSet().add(pmode);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        ebmsError.setRefToMessageInError(userMessage.getMessageId());
        ebmsError.setMessage("some error message");

        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        errorMessage.setRefToMessageId(userMessage.getMessageId());
        errorMessage.setMessageId("error_id");

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(userMessage);        
        IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(errorMessage);
        storageManager.setProcessingState(errorMessageEntity, ProcessingState.READY_FOR_DELIVERY);
        
        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.IN_ERRORS, errorMessageEntities);

        MessageContextUtils.addRefdMsgUnitByError(Collections.singletonList(userMessageEntity), errorMessageEntity, mc);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.DONE, errorMessageEntity.getCurrentProcessingState().getState());
    }
}