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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
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
    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ProcessErrors handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = ProcessErrorsTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }
    @Before
    public void setUp() throws Exception {
        // Executed after org.holodeckb2b.ebms3.handlers.inflow.DeliverReceipts handler
        handler = new ProcessErrors();
    }

    /**
     * Test the case when the message unit is present and is referenced in error
     */
    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData userMessage = TestUtils.getMMD("handlers/full_mmd.xml", this);

        String msgId = MessageIdUtils.createMessageId();
        userMessage.setMessageId(msgId);

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        String errorId = "error_id";
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        ebmsError.setRefToMessageInError(msgId);
        ebmsError.setMessage("some error message");

        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        errorMessage.setRefToMessageId(msgId);
        errorMessage.setMessageId("error_id");

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input message property
        IUserMessageEntity userMessageEntity = storageManager.storeOutGoingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessageEntity);

        // Setting input errors property
        IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(errorMessage);
        final ArrayList<IErrorMessageEntity>  errorSignals = new ArrayList<>();
        errorSignals.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.IN_ERRORS, errorSignals);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(ProcessingState.FAILURE, HolodeckB2BCoreInterface.getQueryManager()
														.getMessageUnitsWithId(userMessage.getMessageId())
														.iterator().next().getCurrentProcessingState().getState());        												
        assertEquals(ProcessingState.READY_FOR_DELIVERY, errorMessageEntity.getCurrentProcessingState().getState());        
    }

    /**
     * Test the case when there is no reference to message unit in error
     */
    @Test
    public void testDoProcessingIfNoRefToMsgId() throws Exception {

        MessageContext mc = new MessageContext();
        mc.setServerSide(true);
        mc.setFLOW(MessageContext.IN_FLOW);

        String errorId = "error_id";
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        ebmsError.setMessage("some error message");

        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        errorMessage.setMessageId("error_id");

        StorageManager storageManager = HolodeckB2BCore.getStorageManager();

        // Setting input errors property
        IErrorMessageEntity errorMessageEntity = storageManager.storeIncomingMessageUnit(errorMessage);
        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.IN_ERRORS, errorMessageEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
        assertEquals(ProcessingState.FAILURE, errorMessageEntity.getCurrentProcessingState().getState());
    }
}