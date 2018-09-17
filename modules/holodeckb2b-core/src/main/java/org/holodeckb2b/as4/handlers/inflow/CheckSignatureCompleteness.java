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
package org.holodeckb2b.as4.handlers.inflow;

import java.util.Set;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.PolicyNoncompliance;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler that checks whether all payloads in the user message are signed. That all payloads
 * must be signed follows from profiling rule (b) defined in section 5.1.8 of the AS4 Profile:<br>
 * <i>"When signed receipts are requested in AS4 that make use of default conventions, the Sending message handler
 * (i.e. the MSH sending messages for which signed receipts are expected) MUST identify message parts (referenced in
 * eb:PartInfo elements in the received User Message) and MUST sign the SOAP body and all attachments"</i>
 * <p>If a payload is not signed a <i>PolicyNoncompliance</i> error will be generated and reported to the sender of the
 * user message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CheckSignatureCompleteness extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc, final IUserMessageEntity um) throws Exception {
        // First check if this message needs a Receipt and is signed
        final IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(um.getPModeId());
        if (pmode == null) {
            // The P-Mode configurations has changed and does not include this P-Mode anymore, assume no receipt
            // is needed
            log.error("P-Mode " + um.getPModeId() + " not found in current P-Mode set!"
                     + "Unable to determine if receipt is needed for message [msgId=" + um.getMessageId() + "]");
            return InvocationResponse.CONTINUE;
        }

        // Currently we only support one-way MEPs so the leg is always the first one
        if (pmode.getLegs().iterator().next().getReceiptConfiguration() == null) {
            // No receipt requested for this message
            return InvocationResponse.CONTINUE;
        }

        // Check if received message was signed
        final ISignatureProcessingResult signatureResult =
                          (ISignatureProcessingResult) mc.getProperty(MessageContextProperties.SIG_VERIFICATION_RESULT);        
        if (signatureResult != null && !Utils.isNullOrEmpty(um.getPayloads())) {
        	log.trace("Message is signed, check that each payload has been included in the Signature");
            Set<IPayload> signedPayloads = signatureResult.getPayloadDigests().keySet();
            if (!um.getPayloads().parallelStream().allMatch(
                            (mp) -> signedPayloads.parallelStream()
                                                  .anyMatch((sp) -> areSamePayloadRef(mp, sp)))) {
                log.info("Not all payloads in user message [" + um.getMessageId() + "] are signed!");
                // If not all payloads of  the message are signum.getMessageIded it does not conform to the AS4 requirements
                // that all payloads should be signed. Therefore create the PolicyNoncompliance error
                final PolicyNoncompliance   error = new PolicyNoncompliance("Not all payloads are signed");
                error.setRefToMessageInError(um.getMessageId());
                MessageContextUtils.addGeneratedError(mc, error);
                HolodeckB2BCore.getStorageManager().setProcessingState(um, ProcessingState.FAILURE);
            }
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Helper method to check whether two payload references point to the same payload in the message.
     *
     * @param plRef1    The first reference
     * @param plRef2    The second reference
     * @return          <code>true</code> if both reference have same URI and containment type,<br>
     *                  <code>false</code> otherwise.
     */
    private boolean areSamePayloadRef(IPayload plRef1, IPayload plRef2) {
        return Utils.nullSafeEqual(plRef1.getContainment(), plRef2.getContainment()) &&
               Utils.nullSafeEqual(plRef1.getPayloadURI(), plRef2.getPayloadURI());
    }
}
