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


import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IPullRequestEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProcessGeneratedErrorsTest {

	private static ProcessGeneratedErrors handler;

    private static HolodeckB2BTestCore		testCore;

    private static String oneWayPModeId;
    private static String twoWayPModeId;

    @BeforeAll
    public static void setUpClass() throws Exception {
    	testCore = new HolodeckB2BTestCore();
        HolodeckB2BCoreInterface.setImplementation(testCore);

        handler = new ProcessGeneratedErrors();
        ModuleConfiguration moduleDescr = new ModuleConfiguration("test", null);
        moduleDescr.addParameter(new Parameter("HandledMessagingProtocol", "TEST"));
        HandlerDescription handlerDescr = new HandlerDescription();
        handlerDescr.setParent(moduleDescr);
        handler.init(handlerDescr);

        PMode oneWayPMode = HB2BTestUtils.create1WayReceivePMode();
        oneWayPModeId = oneWayPMode.getId();
        testCore.getPModeSet().add(oneWayPMode);
        PMode twoWayPMode = HB2BTestUtils.create2WaySendOnRequestPMode();
        twoWayPModeId = twoWayPMode.getId();
        testCore.getPModeSet().add(twoWayPMode);
    }

    /**
     * Test anonymous errors
     */
    @Test
    public void testErrorsWithoutRef() {
    	// Prepare msg ctx
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

        // Create the Error Signal
        EbmsError error1 = new EbmsError();
        error1.setErrorDetail("Some error for testing.");
        procCtx.addGeneratedError(error1);
        EbmsError error2 = new EbmsError();
        error2.setErrorDetail("Some other error for testing.");
        procCtx.addGeneratedError(error2);

        assertEquals(Handler.InvocationResponse.CONTINUE, assertDoesNotThrow(() -> handler.invoke(mc)));

        Collection<IErrorMessageEntity> errorSigs = procCtx.getSendingErrors();
        assertFalse(Utils.isNullOrEmpty(errorSigs));
        assertEquals(1, errorSigs.size());
        IErrorMessageEntity errSig = errorSigs.iterator().next();
        assertNull(errSig.getPModeId());
        assertNull(errSig.getRefToMessageId());
        assertNotNull(errSig.getErrors());
        assertEquals(2, errSig.getErrors().size());
    }

    /**
     * Test errors with same reference
     * @throws StorageException
     */
    @Test
    public void testErrorsSameMsg() throws StorageException {
    	// Prepare message in error
    	UserMessage usrMsg = new UserMessage();
    	usrMsg.setMessageId(MessageIdUtils.createMessageId());
    	usrMsg.setPModeId(twoWayPModeId);
    	IUserMessageEntity usrMsgEntity = HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(usrMsg);

    	// Prepare msg ctx
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);
    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

    	procCtx.setUserMessage(usrMsgEntity);

    	// Create the Error Signal
    	EbmsError error1 = new EbmsError();
    	error1.setErrorDetail("Some error for testing.");
    	error1.setRefToMessageInError(usrMsg.getMessageId());
    	procCtx.addGeneratedError(error1);
    	EbmsError error2 = new EbmsError();
    	error2.setErrorDetail("Some other error for testing.");
    	error2.setRefToMessageInError(usrMsg.getMessageId());
    	procCtx.addGeneratedError(error2);

    	assertEquals(Handler.InvocationResponse.CONTINUE, assertDoesNotThrow(() -> handler.invoke(mc)));

    	Collection<IErrorMessageEntity> errorSigs = procCtx.getSendingErrors();
    	assertEquals(1, errorSigs.size());
    	IErrorMessageEntity errSig = errorSigs.iterator().next();
    	assertEquals(usrMsg.getPModeId(), errSig.getPModeId());
    	assertEquals(Label.REPLY, errSig.getLeg());
    	assertEquals(usrMsg.getMessageId(), errSig.getRefToMessageId());
    	assertNotNull(errSig.getErrors());
    	assertEquals(2, errSig.getErrors().size());
    }

    /**
     * Test errors with different references
     * @throws StorageException
     */
    @Test
    public void testErrorsDifferentMsg() throws StorageException {
    	// Prepare message in error
    	UserMessage usrMsg = new UserMessage();
    	usrMsg.setMessageId(MessageIdUtils.createMessageId());
    	usrMsg.setPModeId(twoWayPModeId);
    	IUserMessageEntity usrMsgEntity = HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(usrMsg);

    	PullRequest pullReq = new PullRequest();
    	pullReq.setMessageId(MessageIdUtils.createMessageId());
    	pullReq.setPModeId(oneWayPModeId);
    	IPullRequestEntity pullReqEntity = HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(pullReq);

    	// Prepare msg ctx
    	MessageContext mc = new MessageContext();
    	mc.setFLOW(MessageContext.IN_FLOW);
    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	procCtx.setUserMessage(usrMsgEntity);
    	procCtx.setPullRequest(pullReqEntity);

    	// Create the Errors
    	EbmsError error1 = new EbmsError();
    	error1.setErrorDetail("error-1");
    	error1.setRefToMessageInError(usrMsg.getMessageId());
    	procCtx.addGeneratedError(error1);
    	EbmsError error2 = new EbmsError();
    	error2.setErrorDetail("error-2");
    	error2.setRefToMessageInError(pullReq.getMessageId());
    	procCtx.addGeneratedError(error2);

       	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, handler.invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}

    	Collection<IErrorMessageEntity> errorSigs = procCtx.getSendingErrors();
    	assertFalse(Utils.isNullOrEmpty(errorSigs));
    	assertEquals(2, errorSigs.size());

    	assertTrue(errorSigs.parallelStream()
    						.allMatch(e -> !Utils.isNullOrEmpty(e.getRefToMessageId())
    									&& !Utils.isNullOrEmpty(e.getErrors())
    									&& ( ( usrMsg.getMessageId().equals(e.getRefToMessageId())
    										  && e.getErrors().iterator().next().getErrorDetail().equals(error1.getErrorDetail())
    										 )
    									   || ( pullReq.getMessageId().equals(e.getRefToMessageId())
    										   && e.getErrors().iterator().next().getErrorDetail().equals(error2.getErrorDetail())
    	    								  )
    									   )));

    }



}
