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
package org.holodeckb2b.as4.receptionawareness;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.interfaces.as4.pmode.IAS4Leg;
import org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.PModeUtils;

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

        // Get all the message id's for unacknowlegded user messages
        log.debug("Get all user messages that may need to be resent");
        Collection<IUserMessageEntity> waitingForRcpt = null;
        try {
            waitingForRcpt = HolodeckB2BCore.getQueryManager()
                                                .getMessageUnitsInState(IUserMessage.class, IMessageUnit.Direction.OUT,
                                                        new ProcessingState[] { ProcessingState.AWAITING_RECEIPT,
                                                                                ProcessingState.TRANSPORT_FAILURE,
                                                                                ProcessingState.WARNING
                                                                              });
        } catch (final PersistenceException ex) {
            log.error("An error occurred while retrieving message units from the database! Details: " + ex.getMessage());
            return;
        }

        if (!Utils.isNullOrEmpty(waitingForRcpt)) {
            log.debug(waitingForRcpt.size() + " messages may be waiting for a Receipt");

            StorageManager   updManager = HolodeckB2BCore.getStorageManager();
            // For each message check if it should be retransmitted or not
            for (final IUserMessageEntity um : waitingForRcpt) {
                try {
                    log.debug("Get retry configuration from P-Mode [" + um.getPModeId() + "]");
                    // Retry information is contained in Leg, and as we only have One-way it is always the first
                    // and because retries is part of AS4 reception awareness feature leg should be instance of
                    // ILegAS4, if it is not we can not retransmit
                    IAS4Leg leg = null;
                    IReceptionAwareness raConfig = null;
                    try {
                        leg = (IAS4Leg) HolodeckB2BCore.getPModeSet().get(um.getPModeId()).getLeg(um.getLeg());
                        raConfig = leg.getReceptionAwareness();
                    } catch (final Exception e) {
                        // Could not get configuration for retries, maybe P-Mode configuration was deleted?
                        log.error("Message [" + um.getMessageId() + "] can not be resent due to missing P-Mode ["
                                    + um.getPModeId() + "]");
                    }
                    if (raConfig == null) {
                        // Not an ILegAS4 instance or no RA config available, can't determine if and how to resend.
                        log.error("Message [" + um.getMessageId() + "] can not be resent due to missing Reception"
                                    + " Awareness configuration in P-Mode [" + um.getPModeId() + "]");
                        // Because we don't know how to process this message further the only thing we can do is set
                        // the processing to failed
                        updManager.setProcessingState(um, ProcessingState.FAILURE);
                        continue; // with next message
                    }

                    // Check if retransmit interval has passed
                    // Convert configured retry interval to milliseconds
                    final long retransmitInterval = TimeUnit.MILLISECONDS.convert(raConfig.getRetryInterval().getLength(),
                                                                            raConfig.getRetryInterval().getUnit());
                    if (((new Date()).getTime() - um.getCurrentProcessingState().getStartTime().getTime())
                         >= retransmitInterval) {
                        // The retransmit interval expired, check if message can be resend or a MissingReceipt error
                        // has to be generated

                        // Initial transmission does not count for max retries
                        final int numOfRetransmits = HolodeckB2BCore.getQueryManager().getNumberOfTransmissions(um) - 1;
                        if (numOfRetransmits >= raConfig.getMaxRetries()) {
                            // No retries left, generate MissingReceipt error
                            missingReceiptsLog.error("No Receipt received for UserMessage with messageId="
                                                        + um.getMessageId());
                            // Change processing state accordingly
                            updManager.setProcessingState(um, ProcessingState.FAILURE);
                            log.debug("Changed processing state of user message to reflect failure");
                            // Generate and report (if requested) MissingReceipt
                            generateMissingReceiptError(um, leg);
                        } else {
                            // Message can be resend, is the message to be pushed or pulled?
                            if (PModeUtils.doesHolodeckB2BTrigger(leg)) {
                                log.debug("Message must be pushed to receiver again");
                                updManager.setProcessingState(um, ProcessingState.READY_TO_PUSH);
                            } else {
                                log.debug("Message must be pulled by receiver again");
                                updManager.setProcessingState(um, ProcessingState.AWAITING_PULL);
                            }
                            log.debug("Message unit is ready for retransmission");
                        }
                    } else {
                            // Time to wait for receipt has not expired yet, wait longer
                            log.debug("Retransmit interval not expired yet. Nothing to do.");
                    }
                } catch (final PersistenceException dbe) {
                    log.error("An error occurred when checking retransmission of message unit [msgID="
                                + um.getMessageId() + "]. Details: " + dbe.getMessage());
                }
            }
        } else
            log.debug("No messages waiting for Receipt, nothing to do");
    }

    /**
     * This worker does not need any configuration.
     *
     * @param parameters
     * @throws TaskConfigurationException
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

        log.debug("Create and store MissingReceipt error");
        // Create the error and set reference to user message
        final MissingReceipt missingReceiptError = new MissingReceipt();
        missingReceiptError.setRefToMessageInError(um.getMessageId());

        IErrorMessageEntity   errorMessage;
        try {
            errorMessage = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(
                                                                                new ErrorMessage(missingReceiptError));
        } catch (final PersistenceException ex) {
            log.error("An error occured while saving the MissingReceipt error in database!"
                        + "Details: " + ex.getMessage());
            return;
        }

        log.debug("Determine whether error must be reported");
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
        log.info("MissingReceipt error should " + (deliverError ? "" : "not") + " be reported" );

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
                } else {
                    try {
                        // Deliver the MissingReceipt error using the given delivery spec
                        final IMessageDeliverer deliverer = HolodeckB2BCoreInterface.getMessageDeliverer(deliverySpec);
                        deliverer.deliver(errorMessage);
                        // Indicate successful delivery
                        HolodeckB2BCore.getStorageManager().setProcessingState(errorMessage, ProcessingState.DONE);
                    } catch (final MessageDeliveryException ex) {
                        log.error("An error occurred while delivering the MissingReceipt error to business application!"
                                    + "Details: "  + ex.getMessage());
                        // Indicate delivery failure
                        HolodeckB2BCore.getStorageManager().setProcessingState(errorMessage, ProcessingState.FAILURE);
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

