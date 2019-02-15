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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.persistency.dao.StorageManager;
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

    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }

    @Test
    public void testDoProcessing() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock messaging = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        MessageProcessingContext procCtx = new MessageProcessingContext(mc);
        
        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        ErrorMessage errorMessage = new ErrorMessage(ebmsError);
        errorMessage.setMessageId(MessageIdUtils.createMessageId());
        
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        IErrorMessageEntity errorMessageEntity = updateManager.storeOutGoingMessageUnit(errorMessage);
        procCtx.addSendingError(errorMessageEntity);

        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new PackageErrorSignals().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        assertTrue(messaging.getChildElements().hasNext());
        OMElement signalElem = (OMElement) messaging.getChildElements().next();
        assertEquals("SignalMessage", signalElem.getLocalName());
        assertEquals(EbMSConstants.EBMS3_NS_URI, signalElem.getNamespaceURI());
        assertNotNull(signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "Error")));
        OMElement msgInfoElem = signalElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo"));
        assertNotNull(msgInfoElem);
        assertEquals(errorMessage.getMessageId(), 
        			 msgInfoElem.getFirstChildWithName(new QName(EbMSConstants.EBMS3_NS_URI, "MessageId")).getText());
    }
}