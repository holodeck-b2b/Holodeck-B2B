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

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.ProcessingModeMismatch;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.ebms3.pmode.PModeFinder;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is the <i>IN_FLOW</i> handler responsible for determining the P-Modes that may apply to the received Pull Request.
 * <p>Finding of the P-Mode for a Pull Request is done separately from the general P-Mode finding (in {@link
 * FindPModes}) as it requires knowledge of information contained in the WS-Security header.
 * <p>Based on only the MPC and authentication info it may not be possible to uniquely identify a P-Mode, so the result
 * of P-Mode finding for a Pull Request is a collection of P-Modes that a User Message may be pulled from. Finding the
 * P-Mode for a User Message is done by the {@link PModeFinder} utility class.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class FindPModesForPullRequest extends AbstractBaseHandler {
	/**
	 * Name of the message processing context property that will hold the found P-Modes for the received Pull Request
	 */
	static final String FOUND_PULL_PMODES = "found-pull-pmodes";
	
    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) throws Exception {
        final IPullRequestEntity pullRequest = procCtx.getReceivedPullRequest();
        if (pullRequest != null) {
            /* A PullRequest can at this point be handled by multiple P-Modes because P-Modes can "share" an MPC.
             * Only when a specific message is pulled it the exact P-Mode is known. For finding the P-Mode both the
             * MPC and given authentication info are used.
             */
            log.trace("Find P-Modes matching the pull request");
            final Collection<IPMode> pmodes = PModeFinder.findForPulling(procCtx.getSecurityProcessingResults(),
            															 pullRequest.getMPC());
            if (Utils.isNullOrEmpty(pmodes)) {
                // No P-Modes found for the MPC and authentication info provided in pull request
                log.info("No P-Mode found for PullRequest [" + pullRequest.getMessageId() + "]");
                                              
                procCtx.addGeneratedError(new ProcessingModeMismatch(
                										"Can not process pull request because no P-Mode was found!", 
                										pullRequest.getMessageId()));
                log.trace("Set the processing state of this PullRequest to failure");
                HolodeckB2BCore.getStorageManager().setProcessingState(pullRequest, ProcessingState.FAILURE);
            } else {
                log.debug("Store the list of " + pmodes.size()
                            + " authorized PModes so next handler can retrieve message unit to return");
                procCtx.setProperty(FOUND_PULL_PMODES, pmodes);
            }
        } 

        return InvocationResponse.CONTINUE;
    }
}
