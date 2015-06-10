/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

import java.util.List;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.EmptyMessagePartitionChannel;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.MessageContextUtils;

/**
 * Is the <i>IN_FLOW</i> handler responsible for retrieving a message unit waiting to be pulled and which can be 
 * returned in response to a pull request.
 * <p>The {@link CheckAndAuthorizePullRequest} handler has already determined from which P-Modes messages may be 
 * selected. This handler will select the user message message unit that is waiting as longest to get pulled.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class GetMessageUnitForPulling extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }
    
    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        // First check whether this flow contained a valid pull request
        log.debug("Check for authenticated pull request");
        List<IPMode> authPModes = (List<IPMode>) mc.getProperty(MessageContextProperties.PULL_AUTH_PMODES);
        if (authPModes == null || authPModes.isEmpty()) {
            // Nothing to do, even if request contained a PullRequest no authorized P-modes could be found
            return InvocationResponse.CONTINUE;
        } 
        
        // The request contained a valid PullRequest, indicate start of processing
        PullRequest pullrequest = (PullRequest) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        log.debug("Starting processing of received pull request");
        pullrequest = MessageUnitDAO.startProcessingMessageUnit(pullrequest);

        if (pullrequest == null) {
            // Changing processing state failed, stop processing the pull request, but continue 
            //  message processing as other parts may be processed succesfully
            log.info("Failed to change processing state! Can not process PullRequest in message.");
            return InvocationResponse.CONTINUE;
        }
        
        log.debug("Get the oldest message that can be pulled for the MPC in pull request");
        UserMessage pulledMsg = getMUForPulling(authPModes, pullrequest.getMPC());
        
        if (pulledMsg == null) {
            // No message available -> return Empty MPC error
            log.debug("No message unit available for pulling, return empty MPC error");
            
            // Create the error and store it in the message context so it can be processed later
            // in the pipeline
            EmptyMessagePartitionChannel mpcEmptyError = new EmptyMessagePartitionChannel();
            mpcEmptyError.setErrorDetail("The MPC " + pullrequest.getMPC() + " is empty!");
            mpcEmptyError.setRefToMessageInError(pullrequest.getMessageId());
            
            MessageContextUtils.addGeneratedError(mc, mpcEmptyError);            
        } else {
            log.debug("Message selected for pulling, msgId=" + pulledMsg.getMessageId());
            mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, pulledMsg);
            log.debug("Message stored in context for processing in out flow");
            mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
        }
        
        return InvocationResponse.CONTINUE;        
    }

    /**
     * Helper method to retrieve a user message waiting for pulling on the requested MPC from the database. The longest 
     * waiting message is selected by default. Because the MPC is not always specified in the P-Mode the query based on
     * P-Mode does not guarantee that only messages with the given MPC are returned. Therefore the MPC is checked before
     * selecting the message. Also the message unit's processing state is changed to {@see ProcessingStates#PROCESSING}. 
     * Only if the state change is successful the message unit is returned. If the state could not be changed the next
     * available message unit is selected. 
     * 
     * @param authPModes    The list of P-Modes messages may be selected from
     * @param reqMPC        The MPC contained in the pull request
     * @return              The message unit to returned as result for Pull Request
     *                      or <code>null</code> if no message unit is available
     *                      for processing
     * @throws DatabaseException When a database error occurs while retrieving the message units waiting to be pulled.
     */
    private UserMessage getMUForPulling(List<IPMode> authPModes, String reqMPC) throws DatabaseException {
        log.debug("Get list of messages waiting to be pulled");
        List<UserMessage> waitingMU = null;
        waitingMU = MessageUnitDAO.getMessageUnitsForPModesInState(UserMessage.class, authPModes, 
                                                                    ProcessingStates.AWAITING_PULL);
        
        // Are there messages waiting?
        if (waitingMU == null || waitingMU.isEmpty()) {
            return null;
        } else {
            // There is at least one message available, take the oldest one. This is the first one in result list (as
            // the result is ordered)
            // Message must be selected only if their MPC (defined in message meta-data or P-Mode) matches the requested 
            // MPC and its state can be changed to Processing to ensure that message will only be pulled once
            boolean r = false; int i = 0;
            UserMessage pulledMsg = null;
            do {
                pulledMsg = waitingMU.get(i);
                log.debug("Selected message unit with msgId=" + pulledMsg.getMessageId() + " for pulling");
                
                // The usermessage should be on assigned to the requested MPC or a parent MPC
                if (reqMPC.startsWith(pulledMsg.getMPC())) {
                    log.debug("Set processing state to Processing");
                    try {
                        pulledMsg = MessageUnitDAO.startProcessingMessageUnit(pulledMsg);
                    } catch (DatabaseException ex) {
                        log.error("An error occurred while setting processing state! Details: " + ex.getMessage());
                        // Maybe the error is specific for this MU, so continue with others
                    }
                    r = pulledMsg.getCurrentProcessingState().getName().equals(ProcessingStates.PROCESSING);
                    log.debug("Processing state was " + (r ? "" : "not ")  + "changed");
                } else
                    log.debug("MPC value of selected message is different from requested MPC!");                
                i++;                
            } while (!r && i < waitingMU.size());
            
            if (r) 
                return pulledMsg;
            else {
                log.debug("None of the available messages could be set to processing");
                return null;
            }
        }
    }
 }
