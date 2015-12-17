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
import java.util.Map;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.errors.ProcessingModeMismatch;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.util.PModeFinder;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;

/**
 * Is the <i>IN_FLOW</i> handler responsible for determining the P-Modes that may apply to the received PullRequest. 
 * <p>Identifying the P-Modes that may apply to a PullRequest requires knowledge of information contained in the 
 * WS-Security header of the message. Therefor finding the P-Mode for the PullRequest is done seperately from the 
 * general P-Mode finding (in {@link FindPModes}).
 * <p>Based on only the MPC and authentication info it may not be possible to uniquely identify a P-Mode, so the result
 * of P-Mode finding for a PullRequest is a collection of P-Mode that a user message may be pulled from.
 * <p>Finding the P-Mode for a User Message is done by the {@link PModeFinder} utility class.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class FindPModesForPullRequest extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {        
        log.debug("Check if MessageContext contains a PullRequest");
        PullRequest pullrequest = (PullRequest) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        if (pullrequest != null) {
            /* A PullRequest can at this point be handled by multiple P-Modes because P-Modes can "share" an MPC.
             * Only when a specific message is pulled it the exact P-Mode is known. For finding the P-Mode both the
             * MPC and given authentication info are used.
             */
            log.debug("Get available authentication info contained in pull request");
            Map<String, IAuthenticationInfo> authInfo = (Map<String, IAuthenticationInfo>) 
                                                               mc.getProperty(SecurityConstants.MC_AUTHENTICATION_INFO);
            log.debug("Find P-Modes matching the pull request");
            Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo, pullrequest.getMPC());
            if (pmodes == null || pmodes.isEmpty()) {
                // No P-Modes found for the MPC and authentication info provided in pull request
                log.error("No P-Mode found for PullRequest [" + pullrequest.getMessageId() 
                                                                                         + "], unable to process it!");
                ProcessingModeMismatch   noPmodeIdError = new ProcessingModeMismatch();
                noPmodeIdError.setRefToMessageInError(pullrequest.getMessageId());
                noPmodeIdError.setErrorDetail("Can not process pull request because no P-Mode was found!");
                MessageContextUtils.addGeneratedError(mc, noPmodeIdError);
                log.debug("Set the processing state of this PullRequest to failure");
                MessageUnitDAO.setFailed(pullrequest);
            } else {
                log.debug("Store the list of " + pmodes.size() 
                            + " authorized PModes so next handler can retrieve message unit to return");
                mc.setProperty(MessageContextProperties.PULL_AUTH_PMODES, pmodes);
            } 
        } else {
            log.debug("MessageContext does not contain a PullRequest message unit, continue flow");
        }
        
        return InvocationResponse.CONTINUE;
    }    
}
