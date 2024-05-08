/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the handler that hands over the Receipts that are ready for delivery to the back-end application to the 
 * {@link IDeliveryManager} that will manage the actual delivery to the back-end. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DeliverErrors extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) 
    																					throws StorageException {
        // Check if this message contains error signals
        final Collection<IErrorMessageEntity> errSignals = procCtx.getReceivedErrors();

        if (Utils.isNullOrEmpty(errSignals))
            // No Errors to deliver
            return InvocationResponse.CONTINUE;

        // Process each signal
        for(final IErrorMessageEntity error : errSignals) {
        	if (ProcessingState.READY_FOR_DELIVERY == error.getCurrentProcessingState().getState()) {
        		log.trace("Hand over Error (msgId={}) to Delivery Manager", error.getMessageId());
    	    	try {
    				HolodeckB2BCore.getDeliveryManager().deliver(error);
    			} catch (IllegalArgumentException | MessageDeliveryException deliveryFailure) {
    				// Processing state is already changed by the Delivery Manager, so nothing we can do here.
    			}
        	} else
        		log.debug("Error (msgId={}) is not ready for delivery", error.getMessageId());
        }
        log.debug("Processed all Error signals in message");
        return InvocationResponse.CONTINUE;    	
    }
}
