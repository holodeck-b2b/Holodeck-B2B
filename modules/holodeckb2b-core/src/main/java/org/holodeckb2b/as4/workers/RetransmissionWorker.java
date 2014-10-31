/*
 * Copyright (C) 2012 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.as4.workers;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.holodeckb2b.common.as4.pmode.ILegAS4;
import org.holodeckb2b.common.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.common.workerpool.TaskConfigurationException;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * This worker is responsible for the retransmission of user messages that did not receive
 * an AS4 receipt as expected.
 * 
 * 
 * @author Sander Fieten
 */
public class RetransmissionWorker extends AbstractWorkerTask {

    @Override
    public void doProcessing() {
        
        // Get all the message id's for unacknowlegded messages
        log.debug("Get all messages waiting for a Receipt");
        Collection<MessageUnit> waitingForRcpt = null;
        try {
            waitingForRcpt = MessageUnitDAO.getMessageUnitsInState(ProcessingStates.AWAITING_RECEIPT);
        } catch (DatabaseException ex) {
            log.error("An error occurred while retrieving message units from the database! Details: " + ex.getMessage());
            return;
        }
        
        if (waitingForRcpt != null && !waitingForRcpt.isEmpty()) {
            log.debug(waitingForRcpt.size() + " messages are waiting for a Receipt");
            
            // For each message check if it should be retransmitted or not
            for (MessageUnit mu : waitingForRcpt) {
                if ((mu instanceof UserMessage)) 
                    try {
                        log.debug("Get retry configuration from P-Mode [" + mu.getPMode() + "]");
                        // Retry information is contained in Leg, and as we only have One-way it is always the first 
                        // and because retries is part of AS4 reception awareness feature leg should be instance of 
                        // ILegAS4, if it is not we can not retransmit and have to set message to failed
                        ILegAS4 leg = null;
                        IReceptionAwareness raConfig = null;
                        try {
                            leg = (ILegAS4) HolodeckB2BCore.getPModeSet().
                                                                get(mu.getPMode()).getLegs().iterator().next();
                            raConfig = leg.getReceptionAwareness();
                        } catch (ClassCastException cce) {} 
                        
                        if (raConfig == null) {
                            // Not an ILegAS4 instance or no RA config available, can not be resend. Is not necessarryly
                            // a failure as receipt is just targeted for business app.
                            log.warn("Message [" + mu.getMessageId() + "] can not be resend due to missing Reception"
                                        + " Awareness configuration in P-Mode [" + mu.getPMode() + "]");
                            continue; // with next message
                        }

                        // Convert configured retry interval to milliseconds
                        long retransmitInterval = TimeUnit.MILLISECONDS.convert(raConfig.getRetryInterval().getLength(),
                                                                                raConfig.getRetryInterval().getUnit());

                        int numOfRetransmits = MessageUnitDAO.getNumberOfRetransmits((UserMessage) mu);
                        if (numOfRetransmits >= raConfig.getMaxRetries()) {
                            log.warn("Retransmits exhausted for user message [msgID=" + mu.getMessageId() 
                                    + "], delivery failed!");
                            // Change processing state accordingly
                            MessageUnitDAO.setFailed(mu);
                            log.debug("Changed processing state of user message to reflect failure");
                        } else {
                            // Check if retransmit interval has passed
                            if( ((new Date()).getTime() - mu.getCurrentProcessingState().getStartTime().getTime())
                                 >= retransmitInterval) {
                                log.debug("Retransmit interval expired, resend the message");
                                
                                // Is message to be pushed or pulled?
                                if (leg.getPullRequestFlows() != null) {
                                    log.debug("Message must be pulled by receiver again");
                                    MessageUnitDAO.setWaitForPull(mu);
                                } else {
                                    log.debug("Message must be pushed to receiver again");
                                    MessageUnitDAO.setReadyToPush(mu);
                                }
                                log.debug("Message unit is ready for retransmission");
                            } else {
                                // Time to wait for receipt has not expired yet, wait longer
                                log.debug("Retransmit interval not expired yet. Nothing to do.");
                            }
                        }
                    } catch (DatabaseException dbe) {
                        log.error("An error occurred when checking retransmission of message unit [msgID=" 
                                    + mu.getMessageId() + "]. Details: " + dbe.getMessage());                        
                    }
                else {
                    log.error("A non user message unit [msgID=" + mu.getMessageId() + "] is waiting for a receipt!");
                    //@todo: Should we do something with the processing state of this signal?
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
    public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {      
    }
}

