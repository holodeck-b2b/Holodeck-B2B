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
package org.holodeckb2b.ebms3.handlers.inflow;

import java.util.Collection;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.FailedAuthentication;
import org.holodeckb2b.ebms3.errors.FailedDecryption;
import org.holodeckb2b.ebms3.errors.PolicyNoncompliance;
import org.holodeckb2b.events.security.DecryptionFailedEvent;
import org.holodeckb2b.events.security.SignatureVerificationFailedEvent;
import org.holodeckb2b.events.security.SignatureVerifiedEvent;
import org.holodeckb2b.events.security.UTProcessingFailureEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.IEncryptionProcessingResult;
import org.holodeckb2b.interfaces.security.ISecurityHeaderProcessor;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.events.security.ISignatureVerifiedEvent;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.PModeUtils;
import org.holodeckb2b.security.util.SecurityConfig;

/**
 * Is the <i>IN_FLOW</i> handler responsible for processing the relevant WS-Security headers contained in the message.
 * As described in the ebMS V3 Core Specification there can be two WS-Security headers in the message that hold relevant
 * information. The first is the <i>default</i> header which is used for signing, encryption and can contain a username
 * token for authentication. Additionally a second header targeted to the <i>ebms</i> role/actor may be added to the
 * message for authorization based on a second username token.
 * <p>The handler does not process the headers itself but uses the deployed <i>Holodeck B2B Security Provider</i> to do
 * the actual processing of the header. If the security provider detects an error in the security header the handler
 * will generate a corresponding ebMS Error for all message units contained in the message and set their processing
 * state to <i>FAILURE</i>. To inform the back-end application (or an extension/connector) the handler also raises a
 * <i>message processing event</i> corresponding to the failed action.
 * <p>If the headers are processed successfully by the provider the results are stored in the message context so they
 * can be used for validation and authorization purposes by later handlers. Note however that the security provider may
 * not complete the decryption of attached payloads and a decryption error may occur when saving the payloads.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see ISecurityProvider
 * @see ISecurityHeaderProcessor
 * @see ISecurityProcessingResult
 */
public class ProcessSecurityHeaders extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {

        log.debug("Get P-Mode of primary message unit for settings");
        IMessageUnit primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc);
        if (primaryMU == null) {
            log.debug("Message does not contain a message unit, nothing to do");
            return InvocationResponse.CONTINUE;
        }
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(primaryMU.getPModeId());
        SecurityConfig configuredSec = null;
        // PullReq have not been assigned a P-Mode yet because information from the security headers is needed,
        if (pmode == null && !(primaryMU instanceof IPullRequest))
            log.warn("No P-Mode available for security setting.");
        else if (!(primaryMU instanceof IPullRequest)) {
            log.debug("PMode to use for security processing : " + pmode.getId());
            /* Now get the security config that apply to the message, may be used by the security provider to check on
            /  security policies. This is taken from the other trading partner's security configuration since these
            /  settings are specified at the TreadingPartner who SENDS the message. */
            configuredSec = new SecurityConfig();
            // We need to determine whether we are the initiator of the MEP or the responder to get the correct settings
            final boolean initiator = PModeUtils.isHolodeckB2BInitiator(pmode);

            // Get the security configuration related to signing the message and adding of username tokens.
            final ITradingPartnerConfiguration tradingPartner = initiator ? pmode.getResponder() : pmode.getInitiator();
            final ISecurityConfiguration partnerConfig = tradingPartner != null ?
                                                                       tradingPartner.getSecurityConfiguration() : null;
            if (partnerConfig != null) {
                configuredSec.setSignatureConfiguration(partnerConfig.getSignatureConfiguration());
                configuredSec.setUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT,
                                             partnerConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT));
                configuredSec.setUsernameTokenConfiguration(SecurityHeaderTarget.EBMS,
                                             partnerConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS));
            }
            /* Get the security configuration for encryption of the message. This is taken from the "Holodeck B2B"
            / trading partner's security configuration since the encryption settings are specified at the TradingPartner
            / who RECEIVES the encrypted message. */
            final ITradingPartnerConfiguration hb2bPartner = initiator ? pmode.getInitiator() : pmode.getResponder();
            configuredSec.setEncryptionConfiguration(hb2bPartner != null
                                                     && hb2bPartner.getSecurityConfiguration() != null ?
                                                     hb2bPartner.getSecurityConfiguration().getEncryptionConfiguration()
                                                   : null);
            log.debug("Prepared security configuration based on P-Mode [" + pmode.getId()
                        + "] of the primary message unit [" + primaryMU.getMessageId() + "]");
        }
        // Get all User Message message units from the message
        Collection<IUserMessage> userMessages = MessageContextUtils.getUserMessagesFromMessage(mc);

        // Process the available security headers using the installed security provider
        log.debug("Get security header processor from security provider");
        ISecurityHeaderProcessor hdrProcessor = HolodeckB2BCore.getSecurityProvider().getSecurityHeaderProcessor();
        log.debug("Process the available security headers in the message");
        Collection<ISecurityProcessingResult> results = hdrProcessor.processHeaders(mc, userMessages, configuredSec);
        log.debug("Security header processing finished, handle results");
        if (!Utils.isNullOrEmpty(results))
            for(ISecurityProcessingResult r : results)
                handleProcessingResult(r, mc);
        else
            log.debug("Message did not contain security headers");

        return InvocationResponse.CONTINUE;
    }

    /**
     * Handles the result of the processing a part of the WS-Security header.
     * <p>When the the part is successfully processed, the processing result is added to the message context so the next
     * handlers can use it for policy conformance, authentication or authorization. In case the result applies to the
     * verification of the signature also a {@link SignatureVerifiedEvent} is raised.
     * <p>If the processing failed, an ebMS Error will be generated for all the received message units, their processing
     * state is set to <i>FAILURE</i> and a message processing event will be raised. The ebMS Error generated depends on
     * the reason of failure and the part which failed to process; if the error indicate a security policy violation a
     * <i>PolicyNoncompliance</i> Error is genereated, otherwise for signature and username tokens a <i>
     * FailedAuthentication</i> Error and for decryption problems a <i>FailedDecryption</i>.
     *
     * @param result    The result of processing a part of the security header
     * @param mc        The current message context
     */
    private void handleProcessingResult(final ISecurityProcessingResult result, final MessageContext mc)
                                                                                        throws PersistenceException {
        final IMessageProcessingEventProcessor eventProcessor = HolodeckB2BCore.getEventProcessor();
        final Collection<IMessageUnitEntity> rcvdMsgUnits = MessageContextUtils.getReceivedMessageUnits(mc);
        if (result.isSuccessful()) {
            if (result instanceof ISignatureProcessingResult) {
                mc.setProperty(MessageContextProperties.SIG_VERIFICATION_RESULT, result);
                ISignatureProcessingResult sigResult = (ISignatureProcessingResult) result;
                for (final IMessageUnitEntity mu : rcvdMsgUnits) {
                    ISignatureVerifiedEvent event;
                    if (mu instanceof IUserMessage)
                        event = new SignatureVerifiedEvent((IUserMessage) mu, sigResult.getEbMSHeaderDigest(),
                                                           sigResult.getPayloadDigests());
                    else
                        event = new SignatureVerifiedEvent((ISignalMessage) mu, sigResult.getEbMSHeaderDigest());
                    eventProcessor.raiseEvent(event, mc);
                }
            } else if (result instanceof IEncryptionProcessingResult)
                mc.setProperty(MessageContextProperties.DECRYPTION_RESULT, result);
            else if (result.getTargetedRole() == SecurityHeaderTarget.DEFAULT)
                mc.setProperty(MessageContextProperties.DEFAULT_UT_RESULT, result);
            else
                mc.setProperty(MessageContextProperties.EBMS_UT_RESULT, result);
        } else {
            final SecurityProcessingException reason = result.getFailureReason();
            final StorageManager storageManager = HolodeckB2BCore.getStorageManager();
            final boolean decryptionFailure = (result instanceof IEncryptionProcessingResult);
            // Add warning to log so operator is always informed about problem
            log.warn("The processing of the "
                    + (result instanceof ISignatureProcessingResult ? "signature"
                    : (result instanceof IEncryptionProcessingResult ? "encryption"
                    : "username token in the " + (result.getTargetedRole() == SecurityHeaderTarget.DEFAULT ? "default"
                                                                                                           : "ebms")
                      ))
                    + " security header failed! Details: " + reason.getMessage());
            for (final IMessageUnitEntity mu : rcvdMsgUnits) {
                EbmsError error;
                if (reason.isPolicyViolation())
                    error = new PolicyNoncompliance(reason.getMessage());
                else if (decryptionFailure)
                    error = new FailedDecryption("Decryption of the message [unit] failed!");
                else
                    error = new FailedAuthentication("Authentication of message unit failed!");
                error.setRefToMessageInError(mu.getMessageId());
                MessageContextUtils.addGeneratedError(mc, error);
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
}
