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
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.common.security.ISecurityConfiguration;
import org.holodeckb2b.common.security.IUsernameTokenConfiguration;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.errors.FailedAuthentication;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.security.tokens.UsernameToken;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is the handler responsible for authorizing the processing of a received message (and by that, the message units 
 * included in it).
 * <p>Authorization of message units is specified in section 7.10 of the ebMS V3 Core Specification. It uses a 
 * WS-Security UsernameToken element in a WSS header targeted to the "ebms" role/actor. The configuration is done using 
 * P-Mode parameters in the <code>PMode.Initiator|Responder.Authorization</code> group depending on the role Holodeck 
 * B2B acts in. In the Holodeck B2B P-Mode interface this is <code>IPMode.getInitiator().getSecurityConfiguration().
 * getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS)</code>.<br>
 * If the P-Mode does not include a configuration setting for username tokens there is no authorization performed. This
 * also implies that a message containing an unexpected username token is not rejected.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class AuthorizeMessage extends BaseHandler {

    /**
     * This handler runs in both in flows
     * 
     * @return Fixed value: <code>IN_FLOW | IN_FAULT_FLOW</code>
     */
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }
    
    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        
        log.debug("Get the primary message unit");
        MessageUnit mu = MessageContextUtils.getPrimaryMessageUnit(mc);
        
        if (mu == null || mu instanceof PullRequest) {
            // Primary message unit is PullRequest => authorization checked separately
            return InvocationResponse.CONTINUE;
        }
        
        log.debug("Get the P-Mode for the primary message unit and check if authorization is used");
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(mu.getPMode());
        
        if (pmode == null) {
            // This can happen for general Error signals that do not have a RefToMessageId and can not be linked to
            // a sent message unit 
            log.warn("No P-Mode found for primary message unit, nothing to check!");
            return InvocationResponse.CONTINUE;
        }
        
        /* To determine whether the message must be authorized we need to know if Holodeck B2B acts as the
        Initiator or Responder in this MEP. As we only have One-Way MEPs this equivalent to the HTTP role Holodeck B2B
        plays.        
        The security configuration that defines the authorization however is that of the other role as we authorize the
        other party and not ourselves!
        */
        log.debug("Check security configuration for received message");
        ITradingPartnerConfiguration tradingPartner = isInFlow(INITIATOR) ? pmode.getResponder() : pmode.getInitiator(); 
        ISecurityConfiguration tpSecConfig = tradingPartner != null ? tradingPartner.getSecurityConfiguration() : null;

        /* Authorization of user messages is only based on a user name token that should be included in the WSS header
        targeted to "ebms". If there is no UT configuration for the "ebms" target we do not authorize the message. An 
        UT included in the message will be ignored.
        */
        IUsernameTokenConfiguration utConfig = tpSecConfig == null ? null :
                            tpSecConfig.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS);        
        if (utConfig == null) {
            log.debug("No authorization of the message required");
            return InvocationResponse.CONTINUE;            
        }
        
        log.debug("Message must be authorized, get authentication info from message");
        Map<String, IAuthenticationInfo> authInfo = (Map<String, IAuthenticationInfo>) 
                                                            mc.getProperty(SecurityConstants.MC_AUTHENTICATION_INFO);
        UsernameToken   ebmsUT = authInfo != null ? (UsernameToken) authInfo.get(SecurityConstants.EBMS_USERNAMETOKEN)
                                                  : null;
        
        if (!SecurityUtils.verifyUsernameToken(utConfig, ebmsUT)) {
            log.warn("Message [Primary msg msgId=" + mu.getMessageId() + "] could not be authorized!");
            // Generate error and set all message units (except PullRequest) to failed
            failMsgUnits(mc);            
        } else {
            log.info("Message [Primary msg msgId=" + mu.getMessageId() + "] successfully authorized");
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Generates a <i>FailedAuthentication</i> Error signal for all message units, except the PullRequest, contained in 
     * the received message. Also set the processing state to FAILED to prevent further processing of the message units.
     * 
     * @param mc The message context of the message that failed
     */
    protected void failMsgUnits(MessageContext mc) throws DatabaseException {
        
        log.debug("Get all message units contained in message");
        Collection<MessageUnit> rcvdMsgUnits = MessageContextUtils.getRcvdMessageUnits(mc);
        
        for (MessageUnit mu : rcvdMsgUnits) {
            // PullRequest are authenticated seperately, so process only other message unit types
            if (! (mu instanceof PullRequest)) {
                log.debug("Authentication of message unit [msgId=" + mu.getMessageId() + "] failed!");                
                FailedAuthentication authError = new FailedAuthentication();
                authError.setRefToMessageInError(mu.getMessageId());
                authError.setErrorDetail("Authentication of message unit failed!");
                MessageContextUtils.addGeneratedError(mc, authError);
                MessageUnitDAO.setFailed(mu);
            } 
        }
    }
}
