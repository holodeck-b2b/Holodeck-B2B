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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.wsdl.WSDLConstants;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.testhelpers.TestConfig;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:44 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PrepareResponseMessageTest {

    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");


    private static HolodeckB2BTestCore core;

    private static String baseDir;

    private PrepareResponseMessage handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PrepareResponseMessageTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PrepareResponseMessage();
    }

    @Test
    public void testDoProcessingOfTheUserMessage() throws Exception {
        MessageMetaData userMessage = TestUtils.getMMD("handlers/full_mmd.xml", this);

        MessageContext inMsgCtx = new MessageContext();
        inMsgCtx.setServerSide(true);
        inMsgCtx.setFLOW(MessageContext.IN_FLOW);

        // Setting input message property
        IUserMessageEntity userMessageEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        inMsgCtx.setProperty(MessageContextProperties.OUT_USER_MESSAGE, userMessageEntity);

        MessageContext outMsgCtx = new MessageContext();
        outMsgCtx.setServerSide(true);
        outMsgCtx.setFLOW(MessageContext.OUT_FLOW);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(inMsgCtx);
        
        outMsgCtx.setOperationContext(operationContext);
        
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(outMsgCtx);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertNotNull(outMsgCtx.getProperty(MessageContextProperties.OUT_USER_MESSAGE));
    }

    @Test
    public void testDoProcessingOfTheResponseReceipt() throws Exception {
        MessageContext inMsgCtx = new MessageContext();
        inMsgCtx.setServerSide(true);
        inMsgCtx.setFLOW(MessageContext.IN_FLOW);

        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IReceiptEntity receiptEntity = updateManager.storeIncomingMessageUnit(receipt);
        inMsgCtx.setProperty(MessageContextProperties.RESPONSE_RECEIPT, receiptEntity);

        MessageContext outMsgCtx = new MessageContext();
        outMsgCtx.setServerSide(true);
        outMsgCtx.setFLOW(MessageContext.OUT_FLOW);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(inMsgCtx);
        
        outMsgCtx.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(outMsgCtx);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(outMsgCtx.getProperty(MessageContextProperties.OUT_RECEIPTS));
        assertTrue(!Utils.isNullOrEmpty((Collection) outMsgCtx.getProperty(MessageContextProperties.OUT_RECEIPTS)));
    }

    @Test
    public void testDoProcessingOfTheErrors() throws Exception {
        MessageContext inMsgCtx = new MessageContext();
        inMsgCtx.setServerSide(true);
        inMsgCtx.setFLOW(MessageContext.IN_FLOW);

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IErrorMessageEntity errorMessageEntity = updateManager.storeIncomingMessageUnit(errorMessage);

        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        inMsgCtx.setProperty(MessageContextProperties.OUT_ERRORS, errorMessageEntities);

        MessageContext outMsgCtx = new MessageContext();
        outMsgCtx.setServerSide(true);
        outMsgCtx.setFLOW(MessageContext.OUT_FLOW);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(inMsgCtx);
        
        outMsgCtx.setOperationContext(operationContext);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(outMsgCtx);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertNotNull(outMsgCtx.getProperty(MessageContextProperties.OUT_ERRORS));
        assertTrue(!Utils.isNullOrEmpty((Collection) outMsgCtx.getProperty(MessageContextProperties.OUT_ERRORS)));
    }

    @Test
    public void testDoProcessingOfMultipleErrors() throws Exception {
        MessageContext inMsgCtx = new MessageContext();
        inMsgCtx.setServerSide(true);
        inMsgCtx.setFLOW(MessageContext.IN_FLOW);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();

        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();

        // Adding first error message
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code1");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        IErrorMessageEntity errorMessageEntity = updateManager.storeIncomingMessageUnit(errorMessage);
        errorMessageEntities.add(errorMessageEntity);

        // Adding second error message
        errorMessage = new ErrorMessage();
        errors = new ArrayList<>();
        ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code2");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        errorMessageEntity = updateManager.storeIncomingMessageUnit(errorMessage);
        errorMessageEntities.add(errorMessageEntity);

        inMsgCtx.setProperty(MessageContextProperties.OUT_ERRORS, errorMessageEntities);

        // Mocking the Axis2 Operation Context
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE))
                .thenReturn(inMsgCtx);

        MessageContext outMsgCtx = new MessageContext();
        outMsgCtx.setServerSide(true);
        outMsgCtx.setFLOW(MessageContext.OUT_FLOW);
        outMsgCtx.setOperationContext(operationContext);

        InternalConfiguration config = core.getConfiguration();
        Field f = TestConfig.class.getDeclaredField("allowSignalBundling");
        f.setAccessible(true);
        f.setBoolean(config, true);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(outMsgCtx);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(outMsgCtx.getProperty(MessageContextProperties.OUT_ERRORS));
        Iterator outErrors = ((Collection) outMsgCtx.getProperty(MessageContextProperties.OUT_ERRORS)).iterator();
        assertTrue(!Utils.isNullOrEmpty(outErrors));
        outErrors.next();
        assertTrue(outErrors.hasNext());        
    }
}