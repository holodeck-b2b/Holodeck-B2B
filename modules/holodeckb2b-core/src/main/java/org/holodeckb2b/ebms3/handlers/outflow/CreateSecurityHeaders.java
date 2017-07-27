/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.handlers.outflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.CompareUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.events.DecryptionFailedEvent;
import org.holodeckb2b.events.SignatureCreatedEvent;
import org.holodeckb2b.events.SignatureVerificationFailedEvent;
import org.holodeckb2b.events.UTProcessingFailureEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.events.types.ISignatureCreatedEvent;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.IEncryptionProcessingResult;
import org.holodeckb2b.interfaces.security.IPayloadDigest;
import org.holodeckb2b.interfaces.security.ISecurityHeaderCreator;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.PModeUtils;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the required WS-Security headers in the message. As described
 * in the ebMS V3 Core Specification there can be two WS-Security headers in an ebMS message. The first is the
 * <i>default</i> header which is used for signing, encryption and a username token for authentication. Additionally a
 * second header targeted to the <i>ebms</i> role/actor may be added to the message for authorization based on a second
 * username token. Which headers should be created is defined by the P-Mode of the <i>primary message unit</i> contained
 * in the message. The handler does not create the headers itself but uses the deployed <i>Holodeck B2B Security
 * Provider</i> for the actual creation of the header(s).
 * <p>If an error occurs in the handler will set the processing state for all message units contained in the message to
 * <i>FAILURE</i> and raise the applicable <i>message processing event</i> to inform the back-end application (or an
 * extension/connector) about the problem. Note however that the security provider may not complete the encryption of
 * attached payloads at this time but only when creating the final message, so a problem may occur when sending.
 * <p>When the message is successfully signed a {@link ISignatureCreatedEvent} message processing event will be raised
 * for each User Message message unit contained in the message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see ISecurityProvider
 * @see ISecurityHeaderCreator
 * @see ISecurityProcessingResult
 */
public class CreateSecurityHeaders extends BaseHandler {

    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        log.debug("Get the primary message unit for this message");
        final IMessageUnit primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc);
        if (primaryMU == null)
            // No primary message => this is probably an empty response
            return InvocationResponse.CONTINUE;

        IPMode pmode = HolodeckB2BCore.getPModeSet().get(primaryMU.getPModeId());
        if (pmode != null)
            log.debug("Using P-Mode [" + pmode.getId() + "] of primary message unit [" + primaryMU.getMessageId()
                      + "] to create security headers");
        else {
            if (primaryMU instanceof IErrorMessage) {
                log.warn("No P-Mode available for Error Message [" + primaryMU.getMessageId()
                         + "], can not create security headers!");
                return InvocationResponse.CONTINUE;
            } else {
                log.error("No P-Mode available for primary message unit [" + primaryMU.getMessageId()
                         + "]. Can not send message!");
                throw new SecurityProcessingException("No P-Mode available to create security headers!");
            }
        }
        //
        // Now get the security config to apply to the message.
        //
        SecurityConfig securityToUse = new SecurityConfig();
        // We need to determine whether we are the initiator of the MEP or the responder to get the correct settings
        final boolean initiator = PModeUtils.isHolodeckB2BInitiator(pmode);

        // Get the security configuration for signing the message and adding of username tokens. If the primary message
        // is a Pull Request it can have a specific security configuration for this
        final ITradingPartnerConfiguration hb2bPartner = initiator ? pmode.getInitiator() : pmode.getResponder();
        final ISecurityConfiguration partnerConfig = hb2bPartner != null ? hb2bPartner.getSecurityConfiguration() :null;
        if (partnerConfig != null) {
            securityToUse.setSignatureConfiguration(partnerConfig.getSignatureConfiguration());
            securityToUse.setUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT,
                                             partnerConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT));
            securityToUse.setUsernameTokenConfiguration(SecurityHeaderTarget.EBMS,
                                             partnerConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS));
        }
        if (primaryMU instanceof IPullRequest) {
            final IPullRequestFlow pullReqFlow = PModeUtils.getOutPullRequestFlow(pmode);
            final ISecurityConfiguration pullRequestConfig = pullReqFlow != null ?
                                                                         pullReqFlow.getSecurityConfiguration() : null;
            if (pullRequestConfig != null) {
                if (pullRequestConfig.getSignatureConfiguration() != null)
                    securityToUse.setSignatureConfiguration(pullRequestConfig.getSignatureConfiguration());
                if (pullRequestConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT) != null)
                    securityToUse.setUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT,
                                         pullRequestConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT));
                if (pullRequestConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS) != null)
                    securityToUse.setUsernameTokenConfiguration(SecurityHeaderTarget.EBMS,
                                         pullRequestConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS));
            }
        }

        // Get the security configuration for encryption of the message. This is taken from the other trading partner's
        // security configuration since the encryption settings are specified at the TreadingPartner who RECIEVES the
        // encrypted message.
        final ITradingPartnerConfiguration tradingPartner = initiator ? pmode.getResponder() : pmode.getInitiator();
        securityToUse.setEncryptionConfiguration(tradingPartner != null
                                                 && tradingPartner.getSecurityConfiguration() != null
                                                ? tradingPartner.getSecurityConfiguration().getEncryptionConfiguration()
                                                : null);

        log.debug("Prepared security configuration based on P-Mode [" + pmode.getId()
                    + "] of the primary message unit [" + primaryMU.getMessageId() + "]");

        // Create security headers using the installed security provider
        log.debug("Get security header creator from security provider");
        ISecurityHeaderCreator hdrCreator = HolodeckB2BCore.getSecurityProvider().getSecurityHeaderCreator();
        log.debug("Create the security headers in the message");
        Collection<ISecurityProcessingResult> results = hdrCreator.createHeaders(mc,
                                                                    MessageContextUtils.getUserMessagesFromMessage(mc),
                                                                    securityToUse);
        log.debug("Security header creation finished, handle results");
        if (!Utils.isNullOrEmpty(results))
            for(ISecurityProcessingResult r : results)
                handleResult(r, mc);

        return InvocationResponse.CONTINUE;
    }

    /**
     * Handles the result of the creating a part of the WS-Security header.
     * <p>If the processing failed the processing of all the received message units state is set to <i>FAILURE</i> and
     * a message processing event will be raised to signal the problem in creating the header.
     * <p>For a successfully created signature a {@link ISignatureCreatedEvent} event will be raised for each User
     * Message contained in the message.
     *
     * @param result    The result of creating a part of the security header
     * @param mc        The current message context
     */
    private void handleResult(final ISecurityProcessingResult result, final MessageContext mc)
                                                                                        throws PersistenceException {
        final Collection<IMessageUnitEntity> rcvdMsgUnits = MessageContextUtils.getReceivedMessageUnits(mc);
        final IMessageProcessingEventProcessor eventProcessor = HolodeckB2BCore.getEventProcessor();
        if (result.isSuccessful()) {
            if (result instanceof ISignatureProcessingResult) {
                // Raise the event for each user message. This requires collecting all relevant payload digests for
                // each user message
                final Collection<IPayloadDigest> digests = ((ISignatureProcessingResult) result).getPayloadDigests();
                rcvdMsgUnits.parallelStream().filter((mu) -> mu instanceof IUserMessage).forEach((mu) -> {
                    final Collection<IPayloadDigest> msgDigests = new ArrayList<>();
                    ((IUserMessage) mu).getPayloads().forEach((pl)
                            -> digests.stream().filter((d) -> CompareUtils.areEqual(d.getPayload(), pl))
                                    .forEachOrdered((d) -> msgDigests.add(d)));
                    eventProcessor.raiseEvent(new SignatureCreatedEvent((IUserMessage) mu, msgDigests), mc);
                });
            }
        } else {
            final SecurityProcessingException reason = result.getFailureReason();
            final StorageManager storageManager = HolodeckB2BCore.getStorageManager();
            final boolean decryptionFailure = (result instanceof IEncryptionProcessingResult);
            // Add warning to log so operator is always informed about problem
            log.warn("Creating the "
                    + (result instanceof ISignatureProcessingResult ? "signature"
                    : (result instanceof IEncryptionProcessingResult ? "encryption"
                    : "username token in the " + (result.getTargetedRole() == SecurityHeaderTarget.DEFAULT ? "default"
                                                                                                           : "ebms")
                      ))
                    + " security header failed! Details: " + reason.getMessage());
            for (final IMessageUnitEntity mu : rcvdMsgUnits) {
                storageManager.setProcessingState(mu, ProcessingState.FAILURE);

                IMessageProcessingEvent event;
                if (result instanceof ISignatureProcessingResult)
                    event = new SignatureVerificationFailedEvent(mu, reason);
                else if (result instanceof IEncryptionProcessingResult)
                    event = new DecryptionFailedEvent(mu, reason);
                else
                    event = new UTProcessingFailureEvent(mu, result.getTargetedRole(), reason);
                eventProcessor.raiseEvent(event, mc);
            }
        }
    }

    /**
     * Is a helper class to collect the security settings that must be applied to the outgoing message.
     */
    private static class SecurityConfig implements ISecurityConfiguration {

        private Map<SecurityHeaderTarget, IUsernameTokenConfiguration> usernameTokenConfig = new HashMap<>(2);
        private ISigningConfiguration       signingConfig;
        private IEncryptionConfiguration    encryptionConfig;

        private SecurityConfig() {
        }

        private void setUsernameTokenConfiguration(SecurityHeaderTarget target, IUsernameTokenConfiguration utConfig) {
            usernameTokenConfig.put(target, utConfig);
        }

        @Override
        public IUsernameTokenConfiguration getUsernameTokenConfiguration(SecurityHeaderTarget target) {
            return usernameTokenConfig.get(target);
        }

        private void setSignatureConfiguration(ISigningConfiguration signConfig) {
            signingConfig = signConfig;
        }

        @Override
        public ISigningConfiguration getSignatureConfiguration() {
            return signingConfig;
        }

        private void setEncryptionConfiguration(IEncryptionConfiguration encConfig) {
            encryptionConfig = encConfig;
        }

        @Override
        public IEncryptionConfiguration getEncryptionConfiguration() {
            return encryptionConfig;
        }
    }
}
