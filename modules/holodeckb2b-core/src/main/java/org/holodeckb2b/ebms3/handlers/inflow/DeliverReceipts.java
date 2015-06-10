/*
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
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.delivery.MessageDeliveryException;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IReceipt;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IReceiptConfiguration;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for checking if receipt messages should be delivered to the business 
 * application and if so to hand them over to the responsible {@link IMessageDeliverer}. 
 * <p>To prevent that receipts are delivered twice (in parallel) delivery only takes place when the processing state of 
 * the unit can be successfully changed from {@link ProcessingStates#READY_FOR_DELIVERY} to 
 * {@link ProcessingStates#OUT_FOR_DELIVERY}.
 * <p>NOTE: The actual delivery to the business application is done through a {@link IMessageDeliverer} which is 
 * specified in the P-Mode for the referenced user message.
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
        Collection<Receipt> rcptSignals = (Collection<Receipt>) 
                                                    mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        
        if (rcptSignals == null || rcptSignals.isEmpty())
            // No receipts to deliver
            return InvocationResponse.CONTINUE;
        
        log.debug("Message contains " + rcptSignals.size() + " Receipt signals");
        
        // Process each signal
        for(Receipt rcptSig : rcptSignals) {
            // Prepare message for delivery by checking it is still ready for delivery and then 
            // change its processing state to "out for delivery"
            log.debug("Prepare message [" + rcptSig.getMessageId() + "] for delivery");
            boolean readyForDelivery = false;
            readyForDelivery = MessageUnitDAO.startDeliveryOfMessageUnit(rcptSig);
                        
            if(readyForDelivery) {
                // Errors in this signal can be delivered to business application
                log.debug("Start delivery of Errors in signal [" + rcptSig.getMessageId() + "]");
                try {
                    processReceipt(rcptSig);
                    // Receipt signal processed, change the processing state to done
                    MessageUnitDAO.setDone(rcptSig);
                } catch (MessageDeliveryException ex) {                        
                    log.warn("Could not deliver error to application! Error details: " + ex.getMessage());
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
    private void processReceipt(final IReceipt receipt) throws MessageDeliveryException {
        IDeliverySpecification deliverySpec = null;
        
        log.debug("Determine P-Mode for receipt");
        // Get the referenced message unit. There may be more than one MU with the given id, we assume they
        // all use the same P-Mode
        String refToMsgId = receipt.getRefToMessageId();
        MessageUnit refdMsgUnit = null;
        try {
            refdMsgUnit = MessageUnitDAO.getSentMessageUnitWithId(refToMsgId);
        } catch (DatabaseException dbe) {
            log.error("A database error occurred while searching for the referenced message unit! Error details:"
                        + dbe.getMessage());
        }

        if (refdMsgUnit != null)
            // Found referenced message unit(s), use its P-Mode to determine if and how to deliver receipt
            deliverySpec = getReceiptDelivery(refdMsgUnit);
        else
            // No messsage units found for refToMsgId. This should not occur here as this is already checked in
            // previous handler!
            log.error("No referenced message unit found! Probably there is a configuration error!");
        
        // If a delivery specification was found the receipt should be delivered, else no reporting is needed
        if (deliverySpec != null) {
            log.debug("Receipt should be delivered using delivery specification with id:" + deliverySpec.getId());
            IMessageDeliverer deliverer = HolodeckB2BCore.getMessageDeliverer(deliverySpec);
            log.debug("Delivering the Receipt using deliverer");
            deliverer.deliver(receipt);
            log.debug("Receipt successfully delivered!");
        } else
            log.debug("Receipt does not need to (or can not) be delivered");
    }
    
    
    /**
     * Is a helper method to determine if and how an error should be delivered to the business application. The P-Mode
     * of the referenced message unit (which is also the P-Mode for the error) is used for this check.
     * 
     * @param refdUM    The message unit referenced by the Receipt signal
     * @return          When the receipt should be delivered to the business application, the {@link 
     *                  IDeliverySpecification} that should be used for the delivery,<br>
     *                  <code>null</code> otherwise
     */
    protected IDeliverySpecification getReceiptDelivery(MessageUnit refdMU) {
        IDeliverySpecification deliverySpec = null;
        
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(refdMU.getPMode());
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
