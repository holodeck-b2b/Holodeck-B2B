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
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.FailedAuthentication;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.security.util.VerificationUtils;

/**
 * Is the <i>IN_FLOW</i> handler responsible for authorizing the processing of a received message (and by that, the
 * message units included in it).
 * <p>Authorization of message units is specified in section 7.10 of the ebMS V3 Core Specification. It uses a
 * WS-Security UsernameToken element in a WSS header targeted to the "ebms" role/actor. The configuration is done using
 * P-Mode parameters in the <code>PMode.Initiator|Responder.Authorization</code> group depending on the role Holodeck
 * B2B acts in. In the Holodeck B2B P-Mode interface this is <code>IPMode.getInitiator().getSecurityConfiguration().
 * getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS)</code>.<br>
 * If the P-Mode does not include a configuration setting for username tokens there is no authorization performed. This
 * also implies that a message containing an unexpected username token is not rejected.
 * <p>As the authorization of the Pull Request signal is performed when searching the applicable P-Mode this handler
 * will only process the other type of message units.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class AuthorizeMessage extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) throws Exception {

        Collection<IMessageUnitEntity> msgUnits = procCtx.getReceivedMessageUnits();
        if (Utils.isNullOrEmpty(msgUnits)) {
            // No message units in message, nothing to do
            log.trace("Message does not contain a message unit, nothing to do");
            return InvocationResponse.CONTINUE;
        }

        final IPModeSet pmodes = HolodeckB2BCoreInterface.getPModeSet();
        // Check authorization for each message unit
        for (IMessageUnitEntity mu : msgUnits) {
            if (mu instanceof IPullRequest)
                // Authorization of Pull Request is handled separately
                continue;

            log.trace("Get P-Mode for message unit [" + mu.getMessageId() + "] and check if authorization is used");
            final IPMode pmode = pmodes.get(mu.getPModeId());
            if (pmode == null) {
                // This can happen for general Error signals that do not have a RefToMessageId and can not be linked to
                // a sent message unit
                log.debug("No P-Mode found for message unit [" + mu.getMessageId() + "], nothing to check!");
                continue;
            }

            /* To determine whether the message must be authorized we need to know if Holodeck B2B acts as the
            Initiator or Responder in this MEP. The security configuration that defines the authorization however
            is that of the other role as we authorize the other party and not ourselves!
            */
            log.trace("Check security configuration for received message");
            final ITradingPartnerConfiguration tradingPartner = PModeUtils.isHolodeckB2BInitiator(pmode) ?
                                                                            pmode.getResponder() : pmode.getInitiator();
            final ISecurityConfiguration tpSecConfig = tradingPartner == null ? null :
                                                                              tradingPartner.getSecurityConfiguration();

            /* Authorization of user messages is only based on a user name token that should be included in the WSS
            header targeted to "ebms". If there is no UT configuration for the "ebms" entity we do not authorize the
            message. An UT included in the message will be ignored.
            */
            final IUsernameTokenConfiguration utConfig = tpSecConfig == null ? null :
                                                   tpSecConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS);
            if (utConfig == null) {
                log.debug("No authorization of message unit [" + mu.getMessageId() + "] required");
                continue;
            }

            log.debug("Message must be authorized, get authentication info from message");

            final Optional<ISecurityProcessingResult> utToken = 
            									 procCtx.getSecurityProcessingResults()
            											.parallelStream()
														.filter(t -> t instanceof IUsernameTokenProcessingResult 
																 && t.getTargetedRole() == SecurityHeaderTarget.EBMS)
														.findFirst();

            if (!VerificationUtils.verifyUsernameToken(utConfig, 
            										   utToken.isPresent() ? (IUsernameTokenProcessingResult) utToken.get()
            												   			   : null)) {
                log.info("Message unit [" + mu.getMessageId() + "] could not be authorized!");
                procCtx.addGeneratedError(new FailedAuthentication("Authentication of message unit failed!",
                												   mu.getMessageId()));
                HolodeckB2BCore.getStorageManager().setProcessingState(mu, ProcessingState.FAILURE);                
            } else {
                log.info("Message unit [" + mu.getMessageId() + "] successfully authorized");
            }
        }

        return InvocationResponse.CONTINUE;
    }
}
