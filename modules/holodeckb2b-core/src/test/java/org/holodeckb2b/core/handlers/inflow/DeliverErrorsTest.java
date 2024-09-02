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

import java.util.UUID;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestDeliveryManager;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 12:04 15.03.17
 *
 * Checked for cases coverage (24.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DeliverErrorsTest {

	static TestDeliveryManager delman;
	
	@BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
        delman = (TestDeliveryManager) HolodeckB2BCore.getDeliveryManager();
    }
	
	@Before
	public void resetRejection() {
		delman.rejection = null;
	}

    @Test
    public void testDoProcessing() throws Exception {
    	MessageContext mc = new MessageContext();
    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);

    	for(int i = 0; i < 4; i++) {
	    	ErrorMessage errorMessage = new ErrorMessage();
	        errorMessage.setMessageId(UUID.randomUUID().toString());
	        
	        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
	        IErrorMessageEntity errorMessageEntity = storageManager.storeReceivedMessageUnit(errorMessage);        
	        
	        storageManager.setProcessingState(errorMessageEntity, ProcessingState.READY_FOR_DELIVERY);
	        
	        procCtx.addReceivedError(errorMessageEntity);
    	}
    	
        try {
            assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverErrors().invoke(mc));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue(procCtx.getReceivedErrors().parallelStream().allMatch(e -> delman.isDelivered(e.getMessageId())));
    }
    
    @Test
    public void testIgnoreNonReady() throws Exception {
    	MessageContext mc = new MessageContext();
    	IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(mc);
    	
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setMessageId(UUID.randomUUID().toString());
		
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errorMessageEntity = storageManager.storeReceivedMessageUnit(errorMessage);        
		
		storageManager.setProcessingState(errorMessageEntity, ProcessingState.PROCESSING);
		
		procCtx.addReceivedError(errorMessageEntity);
    	
    	try {
    		assertEquals(Handler.InvocationResponse.CONTINUE, new DeliverErrors().invoke(mc));
    	} catch (Exception e) {
    		fail(e.getMessage());
    	}
    	
    	assertFalse(delman.isDelivered(errorMessage.getMessageId()));
    }
    
    
}