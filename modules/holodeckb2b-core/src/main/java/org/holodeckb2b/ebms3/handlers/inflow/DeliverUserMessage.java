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
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Is the <i>IN_FLOW</i> handler responsible for the delivery of the User message message unit to the business 
 * application. 
 * <p>To prevent that the message unit is delivered twice in parallel delivery only takes place when the processing 
 * state can be successfully changed from {@link ProcessingStates#READY_FOR_DELIVERY} to 
 * {@link ProcessingStates#OUT_FOR_DELIVERY}.
 * <p>NOTE: The actual delivery to the business application is done through a <i>DeliveryMethod</i> which is specified 
 * in the P-Mode for this message unit.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DeliverUserMessage extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws DatabaseException {
        
        // Prepare message for delivery by checking it is still ready for delivery and then 
        // change its processing state to "out for delivery"
        log.debug("Prepare message [" + um.getMessageId() + "] for delivery");
        if(MessageUnitDAO.startDeliveryOfMessageUnit(um)) {
            // Message can be delivered to business application
            log.debug("Start delivery of user message");
            try {
                // Get the delivery specification from the P-Mode
                IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(um.getPMode());
                // For now we just have one leg, so we get the delivery spec of the first leg
                IDeliverySpecification deliveryMethod = pmode.getLegs().iterator().next().getDefaultDelivery();
                IMessageDeliverer deliverer = HolodeckB2BCoreInterface.getMessageDeliverer(deliveryMethod);
                log.debug("Delivering the message using delivery specification: " + deliveryMethod.getId());
                deliverer.deliver(um);
                // Indicate that message is delivered so receipt can be created
                mc.setProperty(MessageContextProperties.DELIVERED_USER_MSG, true);
                log.info("Successfully delivered user message [msgId=" + um.getMessageId() +"]");
                log.debug("Set the processing state to delivered");            
                um = MessageUnitDAO.setDelivered(um);                
            } catch (MessageDeliveryException ex) {
                log.error("Could not deliver the user message [msgId=" + um.getMessageId() 
                            + "] using specified delivery method!" 
                            + "\n\tError details: " + ex.getMessage());
                // Indicate failure in processing state
                um = MessageUnitDAO.setDeliveryFailure(um);
            }
        } else {
            // This message is not ready for delivery now which is caused by it already been delivered by another
            // thread. This however should not occur normaly.
            log.warn("Usermessage [" + um.getMessageId() + "] is already being delivered!");
        }

        return InvocationResponse.CONTINUE;
    }
}
