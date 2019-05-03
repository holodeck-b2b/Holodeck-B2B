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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.DeliveryConfiguration;
import org.holodeckb2b.common.pmode.ErrorHandlingConfig;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod;
import org.holodeckb2b.common.testhelpers.NullDeliveryMethod.NullDeliverer;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.After;
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

    private static final String T_PMODE_ID = "t-pmode-errh";

	private static final String T_MSG_ID = "t-msg-id-1@test.holodeck-b2b.org";

	@BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
        HolodeckB2BCoreInterface.getPModeSet().removeAll();
    }

    @Test
    public void testDoProcessing() throws Exception {
        PMode pmode = new PMode();
        pmode.setId(T_PMODE_ID);
        Leg leg = new Leg();
        
        UserMessageFlow umFlow = new UserMessageFlow();

        ErrorHandlingConfig errorHandlingConfig = new ErrorHandlingConfig();
        errorHandlingConfig.setNotifyErrorToBusinessApplication(true);
        
        DeliveryConfiguration deliverySpecification = new DeliveryConfiguration();
        deliverySpecification.setFactory(NullDeliveryMethod.class.getName());
        deliverySpecification.setId("delivery_spec_id");
        errorHandlingConfig.setErrorDelivery(deliverySpecification);
        umFlow.setErrorHandlingConfiguration(errorHandlingConfig);

        leg.setUserMessageFlow(umFlow);
        pmode.addLeg(leg);

        HolodeckB2BCore.getPModeSet().add(pmode);

        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        userMessage.setPModeId(T_PMODE_ID);
        
        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        ebmsError.setRefToMessageInError(userMessage.getMessageId());
        ebmsError.setMessage("some error message");
        ErrorMessage errorMessage = new ErrorMessage(ebmsError);
        errorMessage.setMessageId(T_MSG_ID);
        errorMessage.setPModeId(T_PMODE_ID);
        errorMessage.setRefToMessageId(userMessage.getMessageId());
        
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        IMessageUnitEntity umEntity = storageManager.storeOutGoingMessageUnit(userMessage);        
        
        IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(errorMessage);        
        storageManager.setProcessingState(errorMessageEntity, ProcessingState.READY_FOR_DELIVERY);
        
        MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.addReceivedError(errorMessageEntity);        
        procCtx.addRefdMsgUnitByError(errorMessageEntity, Collections.singletonList(umEntity));
        
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverErrors().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue(((NullDeliverer) HolodeckB2BCore.getMessageDeliverer(deliverySpecification)).wasDelivered(T_MSG_ID));
        assertEquals(ProcessingState.DONE, errorMessageEntity.getCurrentProcessingState().getState());
    }
}