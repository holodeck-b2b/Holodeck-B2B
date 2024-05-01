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

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.handlers.AbstractUserMessageHandler;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is the handler that hands over the User Message that is ready for delivery to the back-end application to the 
 * {@link IDeliveryManager} that will manage the actual delivery to the back-end. If there is a permanent delivery
 * failure a <i>Other</i> Error will be generated so the Sender of the message will be informed that the message cannot
 * be delivered to the addressee.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DeliverUserMessage extends AbstractUserMessageHandler {

    @Override
    protected InvocationResponse doProcessing(final IUserMessageEntity um, final IMessageProcessingContext procCtx, 
    										  final Logger log) throws PersistenceException {
        
    	if (ProcessingState.READY_FOR_DELIVERY == um.getCurrentProcessingState().getState()) {
    		log.trace("Hand over User Message (msgId={}) to Delivery Manager", um.getMessageId());
	    	try {
				HolodeckB2BCore.getDeliveryManager().deliver(um);
			} catch (IllegalArgumentException | MessageDeliveryException deliveryFailure) {
				// Only the MDE can happen here, as we already checked the message to be in RfD state
				if ((deliveryFailure instanceof MessageDeliveryException)  
				   && ((MessageDeliveryException) deliveryFailure).isPermanent()) {
					log.info("Generate Error to indicater permanent delivery failure of User Message (msgId={})",
							 um.getMessageId());
					procCtx.addGeneratedError(new OtherContentError("Message delivery impossible", um.getMessageId()));
				}
			}
    	} else
    		log.debug("User Message (msgId={}) is not ready for delivery", um.getMessageId());

        return InvocationResponse.CONTINUE;
    }
}
