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
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.ProcessingModeMismatch;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.pmode.PModeFinder;

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
public class FindPModesForPullRequest extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws Exception {
        final IPullRequestEntity pullRequest = (IPullRequestEntity)
                                                            mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        if (pullRequest != null) {
            /* A PullRequest can at this point be handled by multiple P-Modes because P-Modes can "share" an MPC.
             * Only when a specific message is pulled it the exact P-Mode is known. For finding the P-Mode both the
             * MPC and given authentication info are used.
             */
            log.trace("Get available authentication info contained in pull request [" + pullRequest.getMessageId() 
            																		+ "]");
            Map<String, ISecurityProcessingResult>    authInfo = new HashMap<>();
            ISecurityProcessingResult signature = (ISecurityProcessingResult)
                                                       mc.getProperty(MessageContextProperties.SIG_VERIFICATION_RESULT);
            if (signature != null)
                authInfo.put(MessageContextProperties.SIG_VERIFICATION_RESULT, signature);
            ISecurityProcessingResult defaultUT = (ISecurityProcessingResult)
                                                       mc.getProperty(MessageContextProperties.DEFAULT_UT_RESULT);
            if (defaultUT != null)
                authInfo.put(MessageContextProperties.DEFAULT_UT_RESULT, defaultUT);
            ISecurityProcessingResult ebmsUT = (ISecurityProcessingResult)
                                                       mc.getProperty(MessageContextProperties.EBMS_UT_RESULT);
            if (ebmsUT != null)
                authInfo.put(MessageContextProperties.EBMS_UT_RESULT, ebmsUT);
            log.trace("Find P-Modes matching the pull request");
            final Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo, pullRequest.getMPC());
            if (Utils.isNullOrEmpty(pmodes)) {
                // No P-Modes found for the MPC and authentication info provided in pull request
                log.info("No P-Mode found for PullRequest [" + pullRequest.getMessageId() + "]");
                final ProcessingModeMismatch   noPmodeIdError =
                              new ProcessingModeMismatch("Can not process pull request because no P-Mode was found!");
                noPmodeIdError.setRefToMessageInError(pullRequest.getMessageId());
                MessageContextUtils.addGeneratedError(mc, noPmodeIdError);
                log.trace("Set the processing state of this PullRequest to failure");
                HolodeckB2BCore.getStorageManager().setProcessingState(pullRequest, ProcessingState.FAILURE);
            } else {
                log.debug("Store the list of " + pmodes.size()
                            + " authorized PModes so next handler can retrieve message unit to return");
                mc.setProperty(MessageContextProperties.PULL_AUTH_PMODES, pmodes);
            }
        } 

        return InvocationResponse.CONTINUE;
    }
}
