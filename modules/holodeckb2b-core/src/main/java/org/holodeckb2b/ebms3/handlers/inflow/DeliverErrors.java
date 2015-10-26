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
import java.util.Iterator;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.delivery.MessageDeliveryException;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.common.messagemodel.IErrorMessage;
import org.holodeckb2b.common.pmode.IErrorHandling;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IPullRequestFlow;
import org.holodeckb2b.common.pmode.IUserMessageFlow;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for checking if error message should be delivered to the business 
 * application and if so to hand them over to the responsible {@link IMessageDeliverer}. 
 * <p>To prevent that errors in the error message unit are delivered twice in parallel delivery only takes place when 
 * the processing state of the unit can be successfully changed from {@link ProcessingStates#READY_FOR_DELIVERY} to 
 * {@link ProcessingStates#OUT_FOR_DELIVERY}.
 * <p>To enable easy monitoring all received error signals will always be logged to a separate log 
 * (<code>org.holodeckb2b.msgproc.errors.received</code>). 
 * <p>NOTE: The actual delivery to the business application is done through a {@link IMessageDeliverer} which is 
 * specified in the P-Mode for this message unit. That P-Mode is the same as the P-Mode of the referenced message or
 * if no message is referenced and this message unit is received as a response the primary message unit in the request.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DeliverErrors extends BaseHandler {
    
    /**
     * Errors will always be logged to a special error log. Using the logging configuration users can decide if this 
     * logging should be enabled and how errors should be logged.
     */
    private Log     errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.received");
    
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        // Check if this message contains error signals
        Collection<ErrorMessage> errorSignals = (Collection<ErrorMessage>) 
                                                    mc.getProperty(MessageContextProperties.IN_ERRORS);
        
        if (errorSignals == null || errorSignals.isEmpty())
            // No errors to deliver
            return InvocationResponse.CONTINUE;
        
        log.debug("Message contains " + errorSignals.size() + " Error signals");
        
        // Process each signal
        for(ErrorMessage errorSig : errorSignals) {
            // Prepare message for delivery by checking it is still ready for delivery and then 
            // change its processing state to "out for delivery"
            log.debug("Prepare message [" + errorSig.getMessageId() + "] for delivery");
            boolean readyForDelivery = MessageUnitDAO.startDeliveryOfMessageUnit(errorSig);
                        
            if(readyForDelivery) {
                // Errors in this signal can be delivered to business application
                // Always log the error signal, even if it does not need to be delivered to the business application
                log.debug("Write error signal to error log");
                errorLog.error(errorSig);                
                log.debug("Start delivery of Errors in signal [" + errorSig.getMessageId() + "]");
                // We deliver each error in the signal separately because they can reference different
                // messages and therefor have different delivery specs
                for(IEbmsError error : errorSig.getErrors())
                    try {
                        checkAndDeliver(error, errorSig, mc);
                    } catch (MessageDeliveryException ex) {                        
                        log.warn("Could not deliver error to application! Error details: " + ex.getMessage());
                    }
                
                // All errors in signal processed, change the processing state to done
                errorSig = MessageUnitDAO.setDone(errorSig);
            } else {
                log.info("Error signal [" + errorSig.getMessageId() + "] is already processed for delivery");
            }
        }
        
        log.debug("Processed all Error signals in message");
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Is a helper method responsible for checking whether and if so delivering a specific error to the business 
     * application. Delivery to the business application is done through a {@link IMessageDeliverer}. As this interface
     * only accepts <code>IMessageUnits</code> object each error is package in a new <code>ErrorMessage</code> object.
     * 
     * @param error         The error to process
     * @param parentSignal  The Error signal that contains the error
     * @param mc            The current message context which is needed when the error does not reference a message
     *                      directly
     * @throws MessageDeliveryException When the error should be delivered to the business application but an error
     *                                  prevented successful delivery
     */
    private void checkAndDeliver(final IEbmsError error, final IErrorMessage parentSignal, final MessageContext mc) 
            throws MessageDeliveryException {
        IDeliverySpecification deliverySpec = null;
        
        log.debug("Determine P-Mode for error");
        // Does the error reference another message unit?
        String refToMsgId = error.getRefToMessageInError();
        // If error does not contain a reference, the signal may do
        if (refToMsgId == null || refToMsgId.isEmpty())
            refToMsgId = parentSignal.getRefToMessageId();
        
        if (refToMsgId != null && !refToMsgId.isEmpty()) {
            log.debug("The error references message unit with msgId=" + refToMsgId);
            // Get the referenced message unit. There may be more than one MU with the given id, we assume they
            // all use the same P-Mode
            MessageUnit refdMsgUnit = null;
            try {
                refdMsgUnit = MessageUnitDAO.getSentMessageUnitWithId(refToMsgId);
            } catch (DatabaseException dbe) {
                log.error("A database error occurred while searching for the referenced message unit! Error details:"
                            + dbe.getMessage());
            }

            if (refdMsgUnit != null)
                // Found referenced message unit(s), use its P-Mode to determine if and how to deliver error
                deliverySpec = getErrorDelivery(refdMsgUnit);
            else
                // No messsage units found for refToMsgId. This should not occur here as this is already checked in
                // previous handler!
                log.error("No referenced message unit found! Probably there is a configuration error!");
        } else {
            log.debug("Error does not directly reference a message unit");
            // If the error is a direct response and there was just on outgoing message unit we still have a 
            // reference
            Collection<MessageUnit>  reqMUs = MessageContextUtils.getSentMessageUnits(mc);
            if (reqMUs.size() == 1) {
                log.debug("Request contained one message unit, assuming error applies to it");
                deliverySpec = getErrorDelivery(reqMUs.iterator().next());
            }
        }
        
        // If a delivery specification was found the error should be delivered, else no reporting is needed
        if (deliverySpec != null) {
            log.debug("Error should be delivered using delivery specification with id:" + deliverySpec.getId());
            // Create a new Error signal message unit as delivers only accept message units
            ErrorMessage deliverError = new ErrorMessage();
            deliverError.setRefToMessageId(refToMsgId);
            deliverError.setMessageId(parentSignal.getMessageId());
            deliverError.setTimestamp(parentSignal.getTimestamp());
            deliverError.addError((EbmsError) error); // this cast is okay as the error is from a persistent object
            
            log.debug("Get deliverer from Core");
            IMessageDeliverer deliverer = HolodeckB2BCore.getMessageDeliverer(deliverySpec);
            log.debug("Delivering the error using deliverer");
            deliverer.deliver(deliverError);
            log.debug("Error successfully delivered!");
        } else
            log.debug("Error does not need to (or can not) be delivered");
    }
    
    /**
     * Is a helper method to determine if and how an error should be delivered to the business application. The P-Mode
     * of the referenced message unit (which is also the P-Mode for the error) is used for this check. It depends on
     * the type of the referenced message unit which P-Mode setting determines whether the error should be delivered
     * and how:<ul>
     * <li>For User message and Receipt signal : <code>PMode.Leg.usermessage.ErrorHandling</code></li>
     * <li>Pull Request : First <code>PMode.leg.pullrequest.ErrorHandling</code> and if that is not specified <code>
     * PMode.Leg.usermessage.ErrorHandling</code></li>
     * <li>Error signal : Here also <code>PMode.Leg.usermessage.ErrorHandling</code> is used. But this can only be done
     * if the sent error was assigned to a P-Mode. This is not guaranteed, so it possible that no error handling 
     * configuration can be retrieved. In which case the error will not be delivered.
     * </ul>
     * How the error is delivered is defined by the delivery specification linked to the error handling configuration.
     * If that is not set the default delivery specification [of the Leg] will be used.
     * 
     * @param refdMU    The message unit referenced by the error
     * @return          When the error should be delivered to the business application, the {@link 
     *                  IDeliverySpecification} that should be used for the delivery,<br>
     *                  <code>null</code> otherwise
     */
    private IDeliverySpecification getErrorDelivery(MessageUnit refdMU) {
        IDeliverySpecification deliverySpec = null;
        
        if (refdMU instanceof ErrorMessage && (refdMU.getPMode() == null || refdMU.getPMode().isEmpty()))
            return null; // Error without P-Mode, can not determine delivery
        
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(refdMU.getPMode());
        if (pmode == null) {
            log.warn("Sent message unit [" + refdMU.getMessageId() +"] does not reference valid P-Mode [" 
                        + refdMU.getPMode() + "]!");
            return null;
        }
        // First get the delivery specification for errors related to the user message as this will also be the fall
        // back for errors related to pull request if nothing is specified specifically for pull requests
        ILeg leg = pmode.getLegs().iterator().next(); // Currently only One-Way MEPS supports, so only one leg
        IUserMessageFlow umFlow = leg.getUserMessageFlow();
        IErrorHandling errHandling = umFlow != null ? umFlow.getErrorHandlingConfiguration() : null;
        if (errHandling != null) 
            deliverySpec = errHandling.getErrorDelivery();
        if (deliverySpec == null)
            deliverySpec = leg.getDefaultDelivery();                            
        
        
        if (refdMU instanceof PullRequest) {
            PullRequest pr = (PullRequest) refdMU;
            // Check if errors for pull requests must be delivered at all and if they have their own delivery spec
            errHandling = null;
            // Check each sub channel
            for(Iterator<IPullRequestFlow> flows = leg.getPullRequestFlows().iterator();
                                                                            flows.hasNext() && errHandling == null;) {
                IPullRequestFlow f = flows.next();
                errHandling = pr.getMPC().equals(f.getMPC()) ? f.getErrorHandlingConfiguration() : null;
            }
            if (errHandling != null && errHandling.getErrorDelivery() != null)
                deliverySpec = errHandling.getErrorDelivery();
        }
        
        if(errHandling != null && errHandling.shouldNotifyErrorToBusinessApplication()) 
            return deliverySpec;
        else 
            return null;
    }
}
