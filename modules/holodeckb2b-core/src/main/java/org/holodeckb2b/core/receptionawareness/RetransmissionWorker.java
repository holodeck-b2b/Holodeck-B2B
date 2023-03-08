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
package org.holodeckb2b.core.receptionawareness;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.events.impl.GenericSendMessageFailure;
import org.holodeckb2b.common.events.impl.MessageDeliveryFailure;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.workers.AbstractWorkerTask;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.holodeckb2b.interfaces.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * This worker is responsible for the retransmission of User Messages that did not receive an AS4 receipt as expected.
 * <p>
 *
 * @author Sander Fieten
 */
public class RetransmissionWorker extends AbstractWorkerTask {

    /**
     * MissingReceipts errors are always logged, independent of P-Mode configuration, to a special log. Using the log
     * configuration users can decide if this logging should be enabled and where errors should be logged.
     */
    private final Log     missingReceiptsLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.missingreceipts");

    @Override
    public void doProcessing() {

        // Get all the message id's for unacknowledged user messages
        log.trace("Get all user messages that may need to be resent");
        Collection<IUserMessageEntity> waitingForRcpt = null;
        try {
            waitingForRcpt = HolodeckB2BCore.getQueryManager()
                                                .getMessageUnitsInState(IUserMessage.class, Direction.OUT,
                                                        new ProcessingState[] { ProcessingState.AWAITING_RECEIPT,
                                                                                ProcessingState.TRANSPORT_FAILURE,
                                                                                ProcessingState.WARNING
                                                                              });
        } catch (final PersistenceException ex) {
            log.error("An error occurred while retrieving message units from the database! Details: " + ex.getMessage());
            return;
        }

        if (Utils.isNullOrEmpty(waitingForRcpt)) 
        	log.trace("There are no messages waiting for a Receipt");
        else {
            log.debug(waitingForRcpt.size() + " messages may be waiting for a Receipt");

            StorageManager storageManager = HolodeckB2BCore.getStorageManager();
            // For each message check if it should be retransmitted or not
            for (final IUserMessageEntity um : waitingForRcpt) {
                try {
                	final ProcessingState cState = um.getCurrentProcessingState().getState();
                    log.trace("Check if User Message [msgId=" + um.getMessageId() 
                    		  + "] should be resend using retry configuration from P-Mode [" + um.getPModeId() + "]");
                    // We need the current interval duration, the number of attempts already executed and the maximum
                    // number of attempts allowed
                    long intervalInMillis = 0;
                    int  attempts = 1; // there is always the initial one
                    int  maxAttempts = 1; 
                    // Retry information is contained in Leg, and as we only have One-way it is always the first
                    // and because retries is part of AS4 reception awareness feature leg should be instance of
                    // ILegAS4, if it is not we can not retransmit
                    ILeg leg;
                    try {
                    	leg = PModeUtils.getLeg(um);                   
                    } catch (IllegalStateException pmodeNotAvailable) {
                    	leg = null;
                    }
                    final IReceptionAwareness raConfig = leg != null ? leg.getReceptionAwareness() : null;
                    final Interval[] intervals = raConfig != null ? raConfig.getWaitIntervals() : null; 
                    if (intervals != null && intervals.length > 0) {
                    	maxAttempts = intervals.length;
                    	// Check the current interval
                    	attempts = Integer.max(1, HolodeckB2BCore.getQueryManager().getNumberOfTransmissions(um));
                		// Get the current applicable interval in milliseconds
                    	int interval = Math.min(attempts, maxAttempts) - 1;
                		intervalInMillis = TimeUnit.MILLISECONDS.convert(intervals[interval].getLength(),
                    													 intervals[interval].getUnit());
                    } else {
                        // There is no retry config available, can't determine if and how to resend.
                        log.warn("Message [{}] cannot be resent due to missing retry configuration in P-Mode [{}]",
                        		 um.getMessageId(), um.getPModeId());
                    	// Set state to SUSPENDED as a new P-Mode with retry configuration may come available
                        // And raise event to signal this issue
                    	if (HolodeckB2BCore.getStorageManager().setProcessingState(um, cState, ProcessingState.SUSPENDED, 
                    													"Missing reception awareness configuration")) 
	                    	HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(new GenericSendMessageFailure(um, 
                    													"Missing reception awareness configuration"));
                    	continue;
                    }
                    
                    // Check if the interval has expired
                    if (((new Date()).getTime() - um.getCurrentProcessingState().getStartTime().getTime())
                         >= intervalInMillis) {
                        // The interval expired, check if message can be resend or if a MissingReceipt error
                        // has to be generated
                        if (attempts >= maxAttempts) {
                            // No retries left, set the state to FAILURE, log and generate MissingReceipt error
                        	if (storageManager.setProcessingState(um, cState, ProcessingState.FAILURE)) {                         	
	                        	log.info("Retry attempts exhausted for User Message [msgId=" + um.getMessageId() + "]!");
	                            missingReceiptsLog.error("No Receipt received for UserMessage with messageId="
	                                                        + um.getMessageId());
	                            log.trace("Changed processing state of user message to reflect failure");
	                            // Generate and report (if requested) MissingReceipt
	                            generateMissingReceiptError(um, leg);
                        	}
                        } else {
                            // Message can be resent, is the message to be pushed or pulled?
                        	log.debug("Sending of User Message [msgId=" + um.getMessageId() + "] should be retried");
                            if (PModeUtils.doesHolodeckB2BTrigger(leg)) {
                                log.trace("Message must be pushed to receiver again");
                                storageManager.setProcessingState(um, ProcessingState.READY_TO_PUSH);
                            } else {
                                log.trace("Message must be pulled by receiver again");
                                storageManager.setProcessingState(um, ProcessingState.AWAITING_PULL);
                            }
                        }
                    } else {
                        // Time to wait for receipt has not expired yet, wait longer
                        log.trace("Retransmit interval not expired yet. Nothing to do.");
                    }
                } catch (final PersistenceException dbe) {
                    log.error("An error occurred when checking or updating the message unit meta-data [msgID="
                                + um.getMessageId() + "]. Details: " + dbe.getMessage());
                }
            }
        } 
    }

    /**
     * This worker does not need any configuration.
     *
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
    }

    /**
     * Generates the <i>MissingReceipt</i> error and notifies the business application on the error if configured in
     * the P-Mode.
     *
     * @param um        The <code>UserMessage</code> for which the <i>Receipt</i> is missing
     * @param leg       The P-Mode Leg configuration for this user message
     */
    private void generateMissingReceiptError(final IUserMessage um, final ILeg leg) {

        log.trace("Create and store MissingReceipt error");
        // Create the error and set reference to user message
        final ErrorMessage missingReceiptError = new ErrorMessage(new MissingReceipt());
        missingReceiptError.setRefToMessageId(um.getMessageId());

        IErrorMessageEntity   errorMessage;
        try {
            errorMessage = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(missingReceiptError);
        } catch (final PersistenceException ex) {
            log.error("An error occured while saving the MissingReceipt error in database!"
                        + "Details: " + ex.getMessage());
            return;
        }

        log.trace("Determine whether error must be reported");
        final IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();
        boolean deliverError = (rcptConfig != null ? rcptConfig.shouldNotifyReceiptToBusinessApplication() : false);
        IDeliverySpecification deliverySpec = (rcptConfig != null ? rcptConfig.getReceiptDelivery() : null);
        if (!deliverError) {
            // Maybe the application does not want to receive notification on receipts but it does want to receive
            // error notifications?
            final IUserMessageFlow umFlow = leg.getUserMessageFlow();
            final IErrorHandling errHandlingConfig = umFlow != null ? umFlow.getErrorHandlingConfiguration() : null;
            if (errHandlingConfig != null) {
                deliverError = errHandlingConfig.shouldNotifyErrorToBusinessApplication();
                deliverySpec = errHandlingConfig.getErrorDelivery();
            }
        }
        log.trace("MissingReceipt error should " + (deliverError ? "" : "not") + " be reported" );

        try {
            if (deliverError) {
                if (deliverySpec == null)
                    // No specific delivery set for receipt or error, use the default one
                    deliverySpec = leg.getDefaultDelivery();
                if (deliverySpec == null) {
                    // No possibility to deliver error as not delivery specs are available, log error
                    log.error("No delivery specification available for notification of MissingReceipt!"
                                + " P-Mode=" + um.getPModeId());
                    // Indicate delivery failure
                    HolodeckB2BCore.getStorageManager().setProcessingState(errorMessage, ProcessingState.FAILURE);
                    // Raise event 
                	HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(
                				new MessageDeliveryFailure(errorMessage,
                									new MessageDeliveryException("Missing delivery specification")));
                } else {
                    try {
                        // Deliver the MissingReceipt error using the given delivery spec
                        final IMessageDeliverer deliverer = HolodeckB2BCoreInterface.getMessageDeliverer(deliverySpec);
                        deliverer.deliver(errorMessage);
                        // Indicate successful delivery
                        HolodeckB2BCore.getStorageManager().setProcessingState(errorMessage, ProcessingState.DONE);
                    } catch (final Throwable t) {
                    	// Catch of Throwable used for extra safety in case the DeliveryMethod implementation does not
                    	// handle all exceptions correctly
                    	log.error("Could not deliver Missing Receipt error to business application! Error details: {}",
                    				t.getMessage());
                    	// Indicate delivery failure
                    	HolodeckB2BCore.getStorageManager().setProcessingState(errorMessage, ProcessingState.FAILURE);
                    	// Raise event
                    	HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(
                    			new MessageDeliveryFailure(errorMessage, 
        										 (t instanceof MessageDeliveryException) ? (MessageDeliveryException) t 
            									  : new MessageDeliveryException("Unexpected error in delivery", t)
            									   ));
                    }
                }
            } else
                // Indicate MissingReceipt error processing is complete
                HolodeckB2BCore.getStorageManager().setProcessingState(errorMessage, ProcessingState.DONE);
        } catch (final PersistenceException dbe) {
            log.error("An error occurred while updating the processing state of the MissingReceipt error!"
                     + " Details: " + dbe.getMessage());
        }
    }
}

