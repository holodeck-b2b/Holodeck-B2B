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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.EmptyMessagePartitionChannel;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for retrieving a message unit waiting to be pulled and which can be
 * returned in response to a pull request.
 * <p>The {@link FindPModesForPullRequest} handler has already determined from which P-Modes messages may be
 * selected. This handler will select the user message message unit that is waiting as longest to get pulled.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class GetMessageUnitForPulling extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        // First check whether this flow contained a valid pull request
        log.debug("Check for authenticated pull request");
        final List<IPMode> authPModes = (List<IPMode>) mc.getProperty(MessageContextProperties.PULL_AUTH_PMODES);
        if (Utils.isNullOrEmpty(authPModes)) {
            // Nothing to do, even if request contained a PullRequest no authorized P-modes could be found
            return InvocationResponse.CONTINUE;
        }

        // The request contained a valid PullRequest, indicate start of processing
        final IPullRequestEntity pullRequest =
                                          (IPullRequestEntity) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        log.debug("Starting processing of received pull request");
        if (!HolodeckB2BCore.getStorageManager().setProcessingState(pullRequest, ProcessingState.RECEIVED,
                                                                                ProcessingState.PROCESSING)) {
            // Changing processing state failed, stop processing the pull request
            log.info("Failed to change processing state! Can not process PullRequest in message.");
            return InvocationResponse.CONTINUE;
        }

        log.debug("Get the oldest message that can be pulled for the MPC in pull request");
        final IUserMessageEntity pulledUserMsg = getForPulling(authPModes, pullRequest.getMPC());

        if (pulledUserMsg == null) {
            // No message available -> return Empty MPC error
            log.debug("No message unit available for pulling, return empty MPC error");
            // Create the error and store it in the message context so it can be processed later
            // in the pipeline
            final EmptyMessagePartitionChannel mpcEmptyError = new EmptyMessagePartitionChannel();
            mpcEmptyError.setErrorDetail("The MPC " + pullRequest.getMPC() + " is empty!");
            mpcEmptyError.setRefToMessageInError(pullRequest.getMessageId());
            MessageContextUtils.addGeneratedError(mc, mpcEmptyError);
            log.debug("Set processing state of Pull Request to indicate processing has completed");
            HolodeckB2BCore.getStorageManager().setProcessingState(pullRequest, ProcessingState.DONE);
        } else {
            log.debug("Message selected for pulling, msgId=" + pulledUserMsg.getMessageId());
            mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE, pulledUserMsg);
            log.debug("Message stored in context for processing in out flow");
            mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Helper method to retrieve a User Message waiting for pulling on the requested MPC from the database. The longest
     * waiting message is selected by default. Because the MPC is not always specified in the P-Mode the query based on
     * P-Mode does not guarantee that only messages with the given MPC are returned. Therefore the MPC is checked before
     * selecting the message. Also the message unit's processing state is changed to {@link ProcessingState#PROCESSING}.
     * Only if the state change is successful the message unit is returned. If the state could not be changed the next
     * available message unit is selected.
     *
     * @param authPModes    The list of P-Modes messages may be selected from
     * @param reqMPC        The MPC contained in the pull request
     * @return              The User Message message unit to returned as result for Pull Request or,<br>
     *                      <code>null</code> if no User Message message unit is available for processing
     * @throws PersistenceException When a database error occurs while retrieving the message units waiting to be pulled.
     */
    private IUserMessageEntity getForPulling(final List<IPMode> authPModes, final String reqMPC)
                                                                                        throws PersistenceException {
        log.debug("Get list of messages waiting to be pulled");
        // Query is based on the P-Mode ids so convert given set of P-Modes to id only collection
        Set<String> pmodeIds = new HashSet<>(authPModes.size());
        for (IPMode p : authPModes) pmodeIds.add(p.getId());
        List<IUserMessageEntity> waitingUserMessages =  HolodeckB2BCore.getQueryManager()
                                                            .getMessageUnitsForPModesInState(IUserMessage.class,
                                                                                        pmodeIds,
                                                                                        ProcessingState.AWAITING_PULL);
        // Are there messages waiting?
        if (Utils.isNullOrEmpty(waitingUserMessages))
            return null;
        else {
            // There is at least one message available, take the oldest one. This is the first one in result list (as
            // the result is ordered)
            // Message must be selected only if their MPC (defined in message meta-data or P-Mode) matches the requested
            // MPC and its state can be changed to Processing to ensure that message will only be pulled once
            boolean r = false; int i = 0;
            IUserMessageEntity userMsgToPull = null;
            do {
                userMsgToPull = waitingUserMessages.get(i);
                log.debug("Check if User Message [" + userMsgToPull.getMessageId() + "] can be pulled.");
                // The usermessage should be on assigned to the requested MPC or a parent MPC
                if (reqMPC.startsWith(userMsgToPull.getMPC())) {
                    log.debug("User Message can be pulled, set processing state to Processing");
                    try {
                        r = HolodeckB2BCore.getStorageManager().setProcessingState(userMsgToPull,
                                                                                  ProcessingState.AWAITING_PULL,
                                                                                  ProcessingState.PROCESSING);
                    } catch (final PersistenceException ex) {
                        log.error("An error occurred while setting processing state! Details: " + ex.getMessage());
                        // Maybe the error is specific for this MU, so continue with others
                    }
                    log.debug("Processing state was " + (r ? "" : "not ")  + "changed");
                } else
                    log.debug("MPC value of selected message is different from requested MPC!");
                i++;
            } while (!r && i < waitingUserMessages.size());

            if (r)
                return userMsgToPull;
            else {
                log.debug("None of the available messages is available for pulling!");
                return null;
            }
        }
    }
 }
