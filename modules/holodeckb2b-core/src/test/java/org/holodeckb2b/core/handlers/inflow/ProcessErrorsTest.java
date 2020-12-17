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
package org.holodeckb2b.core.handlers.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:06 15.03.17
 *
 * Checked for cases coverage (05.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ProcessErrorsTest {

	private ProcessErrors handler;

	@BeforeClass
	public static void setUpClass() throws Exception {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
	}

	@Before
	public void setUp() throws Exception {
		// Executed after org.holodeckb2b.ebms3.handlers.inflow.DeliverReceipts handler
		handler = new ProcessErrors();
		ModuleConfiguration moduleDescr = new ModuleConfiguration("test", null);
		moduleDescr.addParameter(new Parameter("HandledMessagingProtocol", "TEST"));
		HandlerDescription handlerDescr = new HandlerDescription();
		handlerDescr.setParent(moduleDescr);
		handler.init(handlerDescr);
	}

	/**
	 * Test the case when the message unit is present and is referenced in error
	 */
	@Test
	public void testDoProcessing() throws Exception {
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());

		MessageContext mc = new MessageContext();
		mc.setServerSide(true);
		mc.setFLOW(MessageContext.IN_FLOW);
		mc.setAxisService(new AxisService("TEST"));

		EbmsError ebmsError = new EbmsError();
		ebmsError.setSeverity(IEbmsError.Severity.failure);
		ebmsError.setErrorCode("some_error_code");
		ebmsError.setRefToMessageInError(userMessage.getMessageId());
		ebmsError.setMessage("some error message");
		ErrorMessage errorMessage = new ErrorMessage(ebmsError);
		errorMessage.setMessageId(MessageIdUtils.createMessageId());
		errorMessage.setRefToMessageId(userMessage.getMessageId());

		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		storageManager.storeOutGoingMessageUnit(userMessage);

		IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(errorMessage);
		storageManager.setProcessingState(errorMessageEntity, ProcessingState.RECEIVED);

		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.addReceivedError(errorMessageEntity);

		try {
			Handler.InvocationResponse invokeResp = handler.invoke(mc);
			assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertEquals(ProcessingState.FAILURE,
				HolodeckB2BCoreInterface.getQueryManager().getMessageUnitsWithId(userMessage.getMessageId()).iterator()
						.next().getCurrentProcessingState().getState());
		assertEquals(ProcessingState.READY_FOR_DELIVERY, errorMessageEntity.getCurrentProcessingState().getState());
	}

	/**
	 * Test the case when there is no reference to message unit in error
	 */
	@Test
	public void testNoRefToMsgIdButResponse() throws Exception {
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());

		MessageContext mc = new MessageContext();
		mc.setFLOW(MessageContext.OUT_FLOW);

		IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
		procCtx.setUserMessage(storageManager.storeOutGoingMessageUnit(userMessage));
				
		mc.setFLOW(MessageContext.IN_FLOW);
		mc.setAxisService(new AxisService("TEST"));

		EbmsError ebmsError = new EbmsError();
		ebmsError.setSeverity(IEbmsError.Severity.warning);
		ebmsError.setErrorCode("some_error_code");
		ebmsError.setMessage("some error message");
		ErrorMessage errorMessage = new ErrorMessage(ebmsError);
		errorMessage.setMessageId(MessageIdUtils.createMessageId());
		
		IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(errorMessage);
		storageManager.setProcessingState(errorMessageEntity, ProcessingState.RECEIVED);

		procCtx.addReceivedError(errorMessageEntity);

		try {
			Handler.InvocationResponse invokeResp = handler.invoke(mc);
			assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
		} catch (Exception e) {
			fail(e.getMessage());
		}
				
		assertEquals(ProcessingState.WARNING,
				HolodeckB2BCoreInterface.getQueryManager().getMessageUnitsWithId(userMessage.getMessageId()).iterator()
						.next().getCurrentProcessingState().getState());
		assertEquals(ProcessingState.READY_FOR_DELIVERY, errorMessageEntity.getCurrentProcessingState().getState());
	}
}