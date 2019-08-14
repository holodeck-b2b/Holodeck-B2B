/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.handlers.inflow;

import org.apache.commons.logging.Log;
import org.holodeckb2b.common.errors.PolicyNoncompliance;
import org.holodeckb2b.common.handlers.AbstractUserMessageHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;

/**
 * Is the Core <i>IN_FLOW</i> handler that checks whether the <i>User Message</i> should be signed according to its 
 * P-Mode and when it should it is also signed by the sender. The message should be signed when the {@link 
 * ISecurityConfiguration#getSignatureConfiguration()} returns a non <code>null</code> value. If the handler detects 
 * that the received message is not signed while it should have been according to the P-Mode it generates a 
 * <i>PolicyNonCompliance</i> error. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class CheckSignatureRequirement extends AbstractUserMessageHandler {

    @Override
    protected InvocationResponse doProcessing(final IUserMessageEntity um, final MessageProcessingContext procCtx, 
    										  final Log log) throws Exception {

    	final IPMode pmode = HolodeckB2BCore.getPModeSet().get(um.getPModeId());    	
    	final ITradingPartnerConfiguration senderCfg = PModeUtils.isHolodeckB2BInitiator(pmode) ? pmode.getResponder() 
    																						    : pmode.getInitiator();
    	
    	// Check if sender's security config includes signing
    	final ISecurityConfiguration secCfg = senderCfg != null ? senderCfg.getSecurityConfiguration() : null;
    	if (secCfg == null || secCfg.getSignatureConfiguration() == null) {
    		log.trace("P-Mode does not require signing, nothing to check");
    		return InvocationResponse.CONTINUE;
    	}
	    
    	log.trace("P-Mode requires User Message to be signed, check it was");
		if (Utils.isNullOrEmpty(procCtx.getSecurityProcessingResults(ISignatureProcessingResult.class))) {
			log.error("User message [" + um.getMessageId() + "] was not signed");
            final PolicyNoncompliance error = new PolicyNoncompliance("User Message must be signed");
            error.setRefToMessageInError(um.getMessageId());
            procCtx.addGeneratedError(error);
            HolodeckB2BCore.getStorageManager().setProcessingState(um, ProcessingState.FAILURE);
        }

        return InvocationResponse.CONTINUE;
    }
}
