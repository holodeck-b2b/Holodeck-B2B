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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.FailedAuthentication;
import org.holodeckb2b.ebms3.errors.ValueNotRecognized;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.ebms3.util.PModeFinder;

/**
 * Is the handler responsible for finding the correct P-Mode(s) for and authorizing a received pull request signal.
 * <p>For finding the P-Modes that are configured for pulling on the MPC given by the pull request the {@link PModeFinder}
 * utility class is used. If no P-Mode is found for the MPC from the message, an <i>Error</i> signal must be returned 
 * as response to the pull request. We use the <i>ValueNotRecognized</i> error as the given MPC value is unknown to the
 * current configuration.
 * <p>To check whether the pull request is authorized to pull, the authorization info included in the pull request is 
 * checked with each of the P-Modes found for the given MPC. If this results in that the request is not authorized for
 * any of the P-Modes an <i>Error</i> signal is returned as response. The error will be <i>FailedAuthentication</i>.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CheckAndAuthorizePullRequest extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) {
        // First check if this message contains a pull request
        log.debug("Check if MessageContext contains a PullRequest");
        PullRequest pullrequest = (PullRequest) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);

        if (pullrequest == null) {
            log.debug("MessageContext does not contain a PullRequest message unit, continue flow");
            return InvocationResponse.CONTINUE;
        }

        // First step is to indicate we started processing the pull request
        log.debug("Starting processing of received pull request");
        try {
            pullrequest = MessageUnitDAO.startProcessingMessageUnit(pullrequest);
        } catch (DatabaseException dbe) {
            log.error("A database error occurred while changing the processing state! Error details: " 
                                                                                                + dbe.getMessage());
            pullrequest = null;
        }
        if (pullrequest == null) {
            // Changing processing state failed, stop processing the pull request, but continue 
            //  message processing as other parts may be processed succesfully
            log.info("Failed to change processing state! Can not process PullRequest in message.");
            mc.removeProperty(MessageContextProperties.IN_PULL_REQUEST);
            return InvocationResponse.CONTINUE;
        }

        /*
         * Message contains a PullRequest, so check if a P-Mode can be found for
         * the given MPC and authentication info. This is a two step process as
         * failure to find a P-Mode with given MPC must result in an other error
         * than failure to authorize the pull request.
         */
        log.debug("Find P-Modes for MPC of pull request");
        Collection<IPMode> pmodesForMPC = PModeFinder.findForPulling(pullrequest.getMPC());

        if (pmodesForMPC == null || pmodesForMPC.isEmpty()) {
            // No P-Modes found for this MPC, this should be reported to sender of
            //  the request using specific ebMS error
            log.warn("PullRequest [msgId=" + pullrequest.getMessageId() + "] received for unknown MPC: " + pullrequest.getMPC());

            // We use the ValueNotRecognized error to indicate an unknown MPC
            ValueNotRecognized  unknownMPCError = new ValueNotRecognized();
            unknownMPCError.setErrorDetail("The MPC " + pullrequest.getMPC() + " is unknown!");
            unknownMPCError.setRefToMessageInError(pullrequest.getMessageId());
            // Store the error in the message context. How the error will be reported is decided later in the pipeline
            MessageContextUtils.addGeneratedError(mc, unknownMPCError);
            // Message processing can continue because other parts of the message may be processed succesfully
        } else {
            // One or more P-Modes found, check each if there is a sending leg 
            //  with matching authorization
            log.debug("Check whether pull request is authorized for any of " + pmodesForMPC.size() + " found P-Modes");

            List<IPMode> authPmodes = new ArrayList<IPMode>();
            for (IPMode pmode : pmodesForMPC) {
                //@todo: Check the found PModes for authorization
                authPmodes.add(pmode);
            }
            // Check if there is at least one PMode the PullRequest is authorized to pull from
            if (authPmodes.isEmpty()) {
                // No authorized PMode
                log.warn("Unauthorized PullRequest received [msgId=" + pullrequest.getMessageId() 
                                                                        + "][MPC=" + pullrequest.getMPC() + "]");
                FailedAuthentication notAuthorized = new FailedAuthentication();
                notAuthorized.setErrorDetail("Not authorized for pulling");
                notAuthorized.setRefToMessageInError(pullrequest.getMessageId());
                // Store the error in the message context. Error reporting is done later in the pipeline
                MessageContextUtils.addGeneratedError(mc, notAuthorized);                
                // Message processing can continue because other parts of the message may be processed succesfully
            } else {
                log.debug("Store the list of " + authPmodes.size() 
                            + " authorized PModes so next handler can retrieve message unit to return");
                mc.setProperty(MessageContextProperties.PULL_AUTH_PMODES, authPmodes);
            }
        }

        return InvocationResponse.CONTINUE;
    }

}
