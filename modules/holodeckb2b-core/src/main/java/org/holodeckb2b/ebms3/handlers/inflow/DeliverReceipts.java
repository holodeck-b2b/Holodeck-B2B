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

import java.util.Collection;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;

/**
 * Is the <i>IN_FLOW</i> handler responsible for checking if receipt messages should be delivered to the business 
 * application and if so to hand them over to the responsible {@link IMessageDeliverer}. 
 * <p>To prevent that a Receipt is delivered twice (in parallel) delivery only takes place when the processing state of 
 * the unit can be successfully changed from {@link ProcessingStates#READY_FOR_DELIVERY} to 
 * {@link ProcessingStates#OUT_FOR_DELIVERY}.
 * <p>NOTE: The actual delivery to the business application is done through a {@link IMessageDeliverer} which is 
 * specified in the P-Mode for the referenced user message. If available, the delivery method specification specific for
 * Receipt {@link IReceiptConfiguration#getReceiptDelivery()}) will be used, otherwise the default delivery method 
 * ({@link ILeg#getDefaultDelivery())} will be used.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DeliverReceipts extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        // Check if this message contains receipt signals
        Collection<EntityProxy<Receipt>> rcptSignals = (Collection<EntityProxy<Receipt>>) 
                                                    mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        
        if (rcptSignals == null || rcptSignals.isEmpty())
            // No receipts to deliver
            return InvocationResponse.CONTINUE;
        
        log.debug("Message contains " + rcptSignals.size() + " Receipt signals");
        
        // Process each signal
        for(EntityProxy<Receipt> rcptSigProxy : rcptSignals) {
            // Extract the entity object
            Receipt rcptSig = rcptSigProxy.entity;
            
            // Prepare message for delivery by checking it is still ready for delivery and then 
            // change its processing state to "out for delivery"
            log.debug("Prepare message [" + rcptSig.getMessageId() + "] for delivery");
            boolean readyForDelivery = MessageUnitDAO.startDeliveryOfMessageUnit(rcptSigProxy);
                        
            if(readyForDelivery) {
                // Receipt in this signal can be delivered to business application
                try {
                    deliverReceipt(rcptSig);
                    // Receipt signal processed, change the processing state to done
                    MessageUnitDAO.setDone(rcptSigProxy);
                } catch (MessageDeliveryException ex) {                        
                    log.warn("Could not deliver receipt (msgId=" + rcptSig.getMessageId() 
                                    + "]) to application! Error details: " + ex.getMessage());
                    // Although the receipt could not be delivered it was processed completely on the ebMS level,
                    //  so processing state is set to warning instead of failure
                    MessageUnitDAO.setWarning(rcptSigProxy);
                }
            } else {
                log.info("Receipt signal [" + rcptSig.getMessageId() + "] is already processed for delivery");
            }
        }
        
        log.debug("Processed all Receipt signals in message");
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Is a helper method responsible for checking whether and if so delivering a receipt to the business application. 
     * Delivery to the business application is done through a {@link IMessageDeliverer}. 
     * 
     * @param receipt       The receipt to process
     * @throws MessageDeliveryException When the receipt should be delivered to the business application but an error
     *                                  prevented successful delivery
     */
    private void deliverReceipt(final Receipt receipt) throws MessageDeliveryException {
        IDeliverySpecification deliverySpec = null;
        
        // Get delivery specification from P-Mode 
        deliverySpec = getReceiptDelivery(receipt);
        
        // If a delivery specification was found the receipt should be delivered, else no reporting is needed
        if (deliverySpec != null) {
            log.debug("Receipt should be delivered using delivery specification with id:" + deliverySpec.getId());
            IMessageDeliverer deliverer = HolodeckB2BCoreInterface.getMessageDeliverer(deliverySpec);
            // Deliver the Receipt using deliverer
            deliverer.deliver(receipt);
            log.debug("Receipt successfully delivered!");
        } else
            log.debug("Receipt does not need to be delivered");
    }
    
    
    /**
     * Is a helper method to determine if and how an error should be delivered to the business application. 
     * 
     * @param receipt   The Receipt message unit to get delivery spec for
     * @return          When the receipt should be delivered to the business application, the {@link 
     *                  IDeliverySpecification} that should be used for the delivery,<br>
     *                  <code>null</code> otherwise
     */
    protected IDeliverySpecification getReceiptDelivery(final Receipt receipt) {
        IDeliverySpecification deliverySpec = null;
        
        IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(receipt.getPMode());
        ILeg leg = pmode.getLegs().iterator().next(); // Currently only One-Way MEPS supports, so only one leg 
        IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();
        
        if (rcptConfig != null && rcptConfig.shouldNotifyReceiptToBusinessApplication()) {
            log.debug("Receipt should be delivered to business app, get delivery specification");
            deliverySpec = rcptConfig.getReceiptDelivery();
            if (deliverySpec == null) {
                log.debug("No specific delivery specified for receipt, use default delivery");
                deliverySpec = leg.getDefaultDelivery();                            
            }
        }
        
        return deliverySpec;
    }
}
