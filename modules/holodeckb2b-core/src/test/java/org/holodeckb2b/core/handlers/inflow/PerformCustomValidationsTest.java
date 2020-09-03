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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.CustomValidationConfiguration;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.validation.IValidationExecutor;
import org.holodeckb2b.core.validation.ValidationResult;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError.Severity;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.events.ICustomValidationFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 17:39 24.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */

public class PerformCustomValidationsTest {
	
	private static TestEventProcessor eventProcessor;
	
    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BTestCore core = new HolodeckB2BTestCore();
        core.setValidationExecutor(new IValidationExecutor() {			
			@Override
			public ValidationResult validate(IMessageUnit messageUnit, IMessageValidationSpecification validationSpec)
					throws MessageValidationException {
				ValidationResult r = new ValidationResult();
				r.setExecutedAllValidators(true);
				if (validationSpec.getStopSeverity() != null) {
					r.setShouldRejectMessage(validationSpec.getRejectionSeverity() != null);
					r.addValidationErrors("TestingValidator", 
											Collections.singletonList(new MessageValidationError("Test")));
				}				
				return r;
			}
		});
        eventProcessor = (TestEventProcessor) core.getEventProcessor();
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void clearEvents() {
    	eventProcessor.reset();
    }
    
    @Test
    public void testValid() throws Exception {
    	PMode pmode = TestUtils.create1WayReceivePushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
        UserMessageFlow flow = new UserMessageFlow();
        CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
        flow.setCustomValidationConfiguration(validationSpec);
        leg.setUserMessageFlow(flow);
        HolodeckB2BCore.getPModeSet().add(pmode);
        
    	UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        userMessage.setPModeId(pmode.getId());
        
        IUserMessageEntity umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(umEntity);
        
        try {
            new PerformCustomValidations().invoke(mc);
        } catch (Exception e) {
            fail(e.getMessage());
        }        
        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
        assertTrue(eventProcessor.events.isEmpty());
        assertEquals(ProcessingState.READY_FOR_DELIVERY, umEntity.getCurrentProcessingState().getState());
    }

    @Test
    public void testRejection() throws Exception {
    	PMode pmode = TestUtils.create1WayReceivePushPMode();        
        Leg leg = pmode.getLeg(Label.REQUEST);
        UserMessageFlow flow = new UserMessageFlow();
        CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
        validationSpec.setStopSeverity(Severity.Failure);
        validationSpec.setRejectSeverity(Severity.Info);
        flow.setCustomValidationConfiguration(validationSpec);
        leg.setUserMessageFlow(flow);
        HolodeckB2BCore.getPModeSet().add(pmode);
        
    	UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        userMessage.setPModeId(pmode.getId());
        
        IUserMessageEntity umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        ProcessingState currentState = umEntity.getCurrentProcessingState().getState();
        
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(umEntity);
        
        try {
            new PerformCustomValidations().invoke(mc);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors().get(umEntity.getMessageId())));
        assertEquals("EBMS:0004" ,
        			 procCtx.getGeneratedErrors().get(umEntity.getMessageId()).iterator().next().getErrorCode());
        assertFalse(eventProcessor.events.isEmpty());
        assertTrue(eventProcessor.events.get(0) instanceof ICustomValidationFailure);
        assertEquals(ProcessingState.FAILURE, umEntity.getCurrentProcessingState().getState());        
    }
    
    @Test
    public void testWarningOnly() throws Exception {
    	PMode pmode = TestUtils.create1WayReceivePushPMode();        
    	Leg leg = pmode.getLeg(Label.REQUEST);
    	UserMessageFlow flow = new UserMessageFlow();
    	CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
    	validationSpec.setStopSeverity(Severity.Failure);
    	flow.setCustomValidationConfiguration(validationSpec);
    	leg.setUserMessageFlow(flow);
    	HolodeckB2BCore.getPModeSet().add(pmode);
    	
    	UserMessage userMessage = new UserMessage();
    	userMessage.setMessageId(MessageIdUtils.createMessageId());
    	userMessage.setPModeId(pmode.getId());
    	
    	IUserMessageEntity umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
    	ProcessingState currentState = umEntity.getCurrentProcessingState().getState();
    	
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);
    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(umEntity);
    	
    	try {
    		new PerformCustomValidations().invoke(mc);
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
        assertEquals(ProcessingState.READY_FOR_DELIVERY, umEntity.getCurrentProcessingState().getState());
    	assertFalse(eventProcessor.events.isEmpty());
    	assertTrue(eventProcessor.events.get(0) instanceof ICustomValidationFailure);    	       
    }
}