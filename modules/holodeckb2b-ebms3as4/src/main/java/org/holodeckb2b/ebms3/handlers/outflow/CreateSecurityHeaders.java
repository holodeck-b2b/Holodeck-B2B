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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.events.impl.EncryptionFailure;
import org.holodeckb2b.common.events.impl.SignatureCreated;
import org.holodeckb2b.common.events.impl.SigningFailure;
import org.holodeckb2b.common.events.impl.UTCreationFailure;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.ebms3.module.EbMS3Module;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.events.security.ISignatureCreated;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.IEncryptionProcessingResult;
import org.holodeckb2b.interfaces.security.ISecurityHeaderCreator;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

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
 * <p>When the message is successfully signed a {@link ISignatureCreated} message processing event will be raised
 * for each User Message message unit contained in the message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 * @see ISecurityProvider
 * @see ISecurityHeaderCreator
 * @see ISecurityProcessingResult
 */
public class CreateSecurityHeaders extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(IMessageProcessingContext procCtx, final Logger log) throws Exception {
        log.trace("Get the primary message unit for this message");
        final IMessageUnit primaryMU = procCtx.getPrimaryMessageUnit();
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
        ISecurityConfiguration  senderConfig = null, receiverConfig = null;
        // We need to determine if we are the initiator or responder to get the correct settings
        final boolean initiator = PModeUtils.isHolodeckB2BInitiator(pmode);

        // Get the security configuration for signing the message and adding of username tokens. If the primary message
        // is a Pull Request it can have a specific security configuration for this
        if (primaryMU instanceof IPullRequest) {
            final IPullRequestFlow pullReqFlow = PModeUtils.getOutPullRequestFlow(pmode);
            if (pullReqFlow != null) {
                log.trace("Using PullRequest specific settings for Sender's configuration");
                senderConfig = pullReqFlow.getSecurityConfiguration();
            }
        }
        // if not configured specifically for the PullRequest, use the configuration at trading partner level
        if (senderConfig == null) {
            final ITradingPartnerConfiguration hb2bPartner = initiator ? pmode.getInitiator() : pmode.getResponder();
            senderConfig = hb2bPartner != null ? hb2bPartner.getSecurityConfiguration() : null;
        }
        // Get the security configuration for the receiver of the message
        final ITradingPartnerConfiguration tradingPartner = initiator ? pmode.getResponder() : pmode.getInitiator();
        receiverConfig = tradingPartner != null ? tradingPartner.getSecurityConfiguration() : null;

        log.trace("Prepared security configuration based on P-Mode [" + pmode.getId()
                    + "] of the primary message unit [" + primaryMU.getMessageId() + "]");

        // Create security headers using the installed security provider
        log.trace("Get security header creator from security provider");
        ISecurityProvider secProvider;
        try {
        	secProvider = ((EbMS3Module) HolodeckB2BCoreInterface.getModule(EbMS3Module.HOLODECKB2B_EBMS3_MODULE))
        														  			.getSecurityProvider();
        } catch (Exception noSecProvider) {
        	log.fatal("Could not get the required Security Provider! Error: {}", noSecProvider.getMessage());
        	throw new SecurityProcessingException("Security provider not available!");
        }
        ISecurityHeaderCreator hdrCreator = secProvider.getSecurityHeaderCreator();
        log.debug("Create the security headers in the message");
        try {
            Collection<ISecurityProcessingResult> results = hdrCreator.createHeaders(procCtx, 
            																		 senderConfig, receiverConfig);
            log.trace("Security header creation finished, handle results");
            if (!Utils.isNullOrEmpty(results))
                for(ISecurityProcessingResult r : results)
                    handleResult(r, procCtx, log);

            return InvocationResponse.CONTINUE;
        } catch (SecurityProcessingException spe) {
            log.error("An error occurred in the security provider when creating the WSS header(s). Details:"
                     + "\n\tSecurity provider: " + secProvider.getName()
                     + "\n\tError details: " + spe.getMessage());
            throw spe;
        }
    }

    /**
     * Handles the result of the creating a part of the WS-Security header.
     * <p>If the processing failed the processing state of all the message units is set to <i>FAILURE</i> and
     * a message processing event will be raised to signal the problem in creating the header.
     * <p>For a successfully created signature a {@link ISignatureCreated} event will be raised for each User
     * Message contained in the message.
     *
     * @param result    The result of creating a part of the security header
     * @param procCtx   The current message processing context
     * @param log		The log to be used
     */
    private void handleResult(final ISecurityProcessingResult result, final IMessageProcessingContext procCtx,
    						  final Logger log) throws PersistenceException {
        final Collection<IMessageUnitEntity> sentMsgUnits = procCtx.getSendingMessageUnits();
        final IMessageProcessingEventProcessor eventProcessor = HolodeckB2BCore.getEventProcessor();
        if (result.isSuccessful()) {
            if (result instanceof ISignatureProcessingResult) {
                // Raise the event for each user message. This requires collecting all relevant payload digests for
                // each user message
                final Map<IPayload, ISignedPartMetadata> digests =
                                                              ((ISignatureProcessingResult) result).getPayloadDigests();
                sentMsgUnits.parallelStream().filter((mu) -> mu instanceof IUserMessage).forEach((mu) -> {
                    final Map<IPayload, ISignedPartMetadata> msgDigests = new HashMap<>();
                    ((IUserMessage) mu).getPayloads().forEach((pl)
                            -> digests.entrySet().stream().filter((d) -> CompareUtils.areEqual(d.getKey(), pl))
                                    .forEachOrdered((d) -> msgDigests.put(d.getKey(), d.getValue())));
                    eventProcessor.raiseEvent(new SignatureCreated((IUserMessage) mu, msgDigests));
                });
            }
        } else {
            final SecurityProcessingException reason = result.getFailureReason();
            final StorageManager storageManager = HolodeckB2BCore.getStorageManager();
            final boolean encryptionFailure = (result instanceof IEncryptionProcessingResult);
            // Add warning to log so operator is always informed about problem
            log.warn("Creating the "
                    + (result instanceof ISignatureProcessingResult ? "signature"
                    : (result instanceof IEncryptionProcessingResult ? "encryption"
                    : "username token in the " + (result.getTargetedRole() == SecurityHeaderTarget.DEFAULT ? "default"
                                                                                                           : "ebms")
                      ))
                    + " security header failed! Details: " + reason.getMessage());
            for (final IMessageUnitEntity mu : sentMsgUnits) {
                storageManager.setProcessingState(mu, ProcessingState.FAILURE);

                IMessageProcessingEvent event;
                if (result instanceof ISignatureProcessingResult)
                    event = new SigningFailure(mu, reason);
                else if (result instanceof IEncryptionProcessingResult)
                    event = new EncryptionFailure(mu, reason);
                else
                    event = new UTCreationFailure(mu, result.getTargetedRole(), reason);
                eventProcessor.raiseEvent(event);
            }
        }
    }
}
