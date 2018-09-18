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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:46 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PackageErrorSignalsTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private PackageErrorSignals handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PackageErrorSignalsTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PackageErrorSignals();
    }


    @Test
    public void testDoProcessing() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        ErrorSignalElement.createElement(headerBlock, errorMessage);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IErrorMessageEntity errorMessageEntity =
                updateManager.storeIncomingMessageUnit(errorMessage);
        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errorMessageEntity);
        mc.setProperty(MessageContextProperties.OUT_ERRORS,
                errorMessageEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }


    }

    @Test
    public void testDoProcessingIfShouldHaveSOAPFault() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        ErrorSignalElement.createElement(headerBlock, errorMessage);

        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IErrorMessageEntity errorMessageEntity =
                updateManager.storeIncomingMessageUnit(errorMessage);

        IErrorMessageEntity errMESpy = spy(errorMessageEntity);
        doReturn(true).when(errMESpy).shouldHaveSOAPFault();

        ArrayList<IErrorMessageEntity> errorMessageEntities = new ArrayList<>();
        errorMessageEntities.add(errMESpy);
        mc.setProperty(MessageContextProperties.OUT_ERRORS,
                errorMessageEntities);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }


    }
}