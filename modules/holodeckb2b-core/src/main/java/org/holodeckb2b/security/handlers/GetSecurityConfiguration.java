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
package org.holodeckb2b.security.handlers;

import java.util.Collection;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.pmode.PModeUtils;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for determining the security settings that should be applied to the
 * outgoing message.
 * <p>The security settings of a message containing multiple message units are derived from the <i>primary message
 * unit's</i> P-Mode. If bundling of message units from different P-Modes is used the user should ensure that all
 * messages use (or accept) the same security settings.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class GetSecurityConfiguration extends BaseHandler {

    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws AxisFault {

        log.debug("Get the primary message unit for this message");
        final IMessageUnit primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc);
        if (primaryMU == null)
            // No primary message => this is probably an empty response
            return InvocationResponse.CONTINUE;

        log.debug("The primary message unit is a " + MessageUnitUtils.getMessageUnitName(primaryMU)
                                                                        + " with msg-id=" + primaryMU.getMessageId());

        // 2. Get the security settings
        final IPMode pmode = HolodeckB2BCore.getPModeSet().get(primaryMU.getPModeId());

        // It is possible that we can not find a PMode when the primary message unit is an Error signal. In that case
        // no security can be applied
        if (pmode == null) {
            log.warn("No P-Mode found for primary message unit [msgId=" + primaryMU.getMessageId()
                                                                                        + "]. Can not apply security!");
            return InvocationResponse.CONTINUE;
        }

        // We need to determine whether we are the initiator of the MEP or the responder to get the correct settings
        boolean initiator = false;

        // If the primary message is a pull request it can have a specific security configuration
        ISecurityConfiguration  tpSecConfig = null, pullReqSecConfig = null, tpEncSecConfig = null;

        if (primaryMU instanceof IPullRequest) {
            /* When a the message contains a PullRequest we must be the initiator (as we only have One-Way MEPs, if
               there can be Two-Way MEPs it should be checked on the mep binding value or if Pull-Pull the address of
               the legs)
            */
            log.debug("Primary message unit is PullRequest, always initiator");
            initiator = true;
            final IPullRequestFlow prf = PModeUtils.getOutPullRequestFlow(pmode);
            pullReqSecConfig = prf != null ? prf.getSecurityConfiguration() : null;
        } else {
            /* If this message unit is an user message Holodeck B2B acts as the initiator when the http message is a
                request, otherwise it is the response to a PullRequest and Holodeck B2B the responder.
               The Error and Receipt signal messages are always responses to received user message, so we should check
                if that user message was received using Pull or Push as this determins whether we operate as Initiator
                or Responder
            */
            if (primaryMU instanceof IUserMessage) {
                log.debug("Primary message unit is user message, detect initiator or responder");
                initiator = isInFlow(INITIATOR);
            } else {
                initiator = !EbMSConstants.ONE_WAY_PUSH.equals(pmode.getMepBinding());
            }
        }

        // Get security configuration for the trading partner
        final ITradingPartnerConfiguration tradingPartner = initiator ? pmode.getInitiator() : pmode.getResponder();
        tpSecConfig = tradingPartner != null ? tradingPartner.getSecurityConfiguration() : null;


        // Get the security configuration for the second TradingPartner.
        // This is used for encryption only and not for signing, since the encryption settings are specified
        // at the TreadingPartner who RECIEVES the encrypted message.
        // With public key crypto we use the public key of the responder.

        final ITradingPartnerConfiguration tradingPartnerEncryption = initiator ? pmode.getResponder() : pmode.getInitiator();
        tpEncSecConfig = tradingPartnerEncryption != null ? tradingPartnerEncryption.getSecurityConfiguration() : null;


        log.debug("Security is "
                    + (tpSecConfig != null || pullReqSecConfig != null || tpEncSecConfig != null ? "" : "not")
                    + " configured for the primary message unit");

        if (tpSecConfig == null && pullReqSecConfig == null && tpEncSecConfig == null)
            // No security needed, done
            return InvocationResponse.CONTINUE;

        log.debug("Add security configuration to message context");
        // Check if a username token targeted to ebms role must be inserted
        IUsernameTokenConfiguration utConfig = pullReqSecConfig != null ?
                           pullReqSecConfig.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS) :
                           null;
        if (utConfig == null)
            // If no specific UT was defined for the pull request, general settings should be used
            utConfig = tpSecConfig != null ?
                           tpSecConfig.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS) :
                           null;

        if (utConfig != null) {
            addSecurityForMessage(mc, SecurityConstants.EBMS_USERNAMETOKEN, utConfig);
            log.debug("Username token configuration for ebms role added to message context");
        }

        // Check for a username token targeted to default role (can only be from trading partner config)
        utConfig = tpSecConfig != null ?
                            tpSecConfig.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT) :
                            null;
        if (utConfig != null) {
            addSecurityForMessage(mc, SecurityConstants.DEFAULT_USERNAMETOKEN, utConfig);
            log.debug("Username token configuration for default role added to message context");
        }

        // Check if message must be signed
        ISigningConfiguration signConfig = pullReqSecConfig != null ? pullReqSecConfig.getSignatureConfiguration() :
                                                                      null;
        if (signConfig == null)
            // If no specific UT was defined for the pull request, general settings should be used
            signConfig = tpSecConfig != null ? tpSecConfig.getSignatureConfiguration() : null;

        if (signConfig != null) {
            addSecurityForMessage(mc, SecurityConstants.SIGNATURE, signConfig);
            log.debug("Signature configuration added to message context");
        }

        // Check if message must be encrypted. Note this is specified by the SecurityConfiguration
        // of the Responder (and NOT the Initiator). We will only encrypt UserMessage, so we only check this when
        // the primary message unit is a UserMessage
        if (primaryMU instanceof IUserMessage) {
            final IEncryptionConfiguration encryptConfig = tpEncSecConfig != null ?
                                                        tpEncSecConfig.getEncryptionConfiguration() : null;

            if (encryptConfig != null) {
                addSecurityForMessage(mc, SecurityConstants.ENCRYPTION, encryptConfig);
                // Check if this message contains payloads in the body to determine whether body should be encrypted
                boolean includesBodyPl = false;
                final Collection<IPayload> payloads = (Collection<IPayload>) ((IUserMessage) primaryMU).getPayloads();
                if (!Utils.isNullOrEmpty(payloads))
                    for (final IPayload pl : payloads)
                        includesBodyPl = pl.getContainment() == IPayload.Containment.BODY;
                mc.setProperty(SecurityConstants.ENCRYPT_BODY, includesBodyPl);
            }

            log.debug("Encryption configuration added to message context.");
        }

        log.debug("Message context prepared for adding all security headers (signing and encryption)");
        return InvocationResponse.CONTINUE;
    }

   /**
    * Adds the required security configuration to the message context and sets the {@link
    * SecurityConstants#ADD_SECURITY_HEADERS} property to indicate that the security headers must be created. This
    * property is used by the next handler ({@link CreateWSSHeaders}) to determine whether it has to do any work.
    *
    * @param mc                 The message context
    * @param securityTypeId     The message context property name to store the security configuration
    * @param configuration      The security configuration
    */
   private void addSecurityForMessage(final MessageContext mc, final String securityTypeId, final Object configuration) {
       // Add the required settings to the message context
       mc.setProperty(securityTypeId,  configuration);
       // Indicate that the security headers must be created
       mc.setProperty(SecurityConstants.ADD_SECURITY_HEADERS, Boolean.TRUE);
   }
}
