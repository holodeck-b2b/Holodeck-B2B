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
package org.holodeckb2b.core.receptionawareness;

import org.apache.axis2.description.HandlerDescription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractUserMessageHandler;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.StorageException;

/**
 * Is the <i>IN_FLOW</i> handler responsible for detecting and when requested eliminating duplicate <i>user messages</i>.
 * This functionality is based on the <i>Reception Awareness</i> feature specified in the AS4 profile (see section 3.2), 
 * but since it is common in other messaging protocols too, it is applied generically as part of Holodeck B2B's Core
 * functionality.
 * <p>The detection of duplicates is done by checking for an existing {@link IUserMessageEntity} with the same
 * <code>MessageId</code> and which is in {@link ProcessingState#DELIVERED} or {@link ProcessingState#FAILURE} state. 
 * This means the detection window is determined by the time messages stay in the message database.
 * <p>How a duplicate should be handled is normally configured by the P-Mode parameter 
 * <b>ReceptionAwareness.DuplicateDetection.Eliminate</b> which should be set to <i>true</i> for the function to be 
 * executed. Duplicate elimination is also triggered when {@link IMessageProcessingContext#eliminateDuplicates()} 
 * returns <code>true</code>. The second method allows messaging protocol specific handlers executed earlier in the 
 * pipeline to trigger this function based on protocol specific conditions.<br>
 * NOTE that this an <b>or</b>, which means that if either the P-Mode or the processing context indicate that duplicate
 * elimination should be used the function is triggered. 
 * <p>When duplicate elimination is used and a User Message is a duplicate its processing state set to {@link 
 * ProcessingState#DUPLICATE}. This will prevent the User Message from being delivered to the back-end. But as the 
 * duplicate may be a retry due to a missing Receipt signal a new Receipt will still be sent as response (depending on 
 * configuration). 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DetectDuplicateUserMessages extends AbstractUserMessageHandler {

    /**
     * Duplicates will always be logged to a special log. Using the logging
     * configuration users can decide if this logging should be enabled and
     * how duplicates should be logged.
     */
    private static Logger     duplicateLog = LogManager.getLogger("org.holodeckb2b.msgproc.duplicates");

    @Override
	public void init(HandlerDescription handlerdesc) {
    	super.init(handlerdesc);
    }
    
    @Override
    protected InvocationResponse doProcessing(final IUserMessageEntity um, final IMessageProcessingContext procCtx,
    										  final Logger log) throws StorageException {
        // First determine if duplicate check must be executed for this UserMessage
        //
        log.trace("Check if duplicate check must be executed");
        boolean detectDups = procCtx.eliminateDuplicates();
        
        if (!detectDups) {
	        // Get P-Mode configuration
	        ILeg leg = null;
	        try {
		        leg = PModeUtils.getLeg(um);
	        } catch (IllegalStateException pmodeNotAvailable) {
	            // The P-Mode configurations has changed and does not include this P-Mode anymore, assume no receipt
	            // is needed
	            log.error("P-Mode " + um.getPModeId() + " not found in current P-Mode set!"
	                     + "Unable to determine if receipt is needed for message [msgId=" + um.getMessageId() + "]");
	        }        
        
	        // Duplicate detection is part of the AS4 Reception Awareness feature which can only be configured on a leg
	        // of type ILegAS4, so check type
	        if (leg != null) {
	            // Get configuration of Reception Awareness feature
	            final IReceptionAwareness raConfig = leg.getReceptionAwareness();
	            detectDups =  raConfig != null ? raConfig.useDuplicateDetection() : false;
	        }
	    }
	       
        if (!detectDups) {
            log.debug("Duplicate detection not enabled, skipping check.");
            return InvocationResponse.CONTINUE;
        } else {
            final String msgId = um.getMessageId();
            log.trace("Duplicate detection enabled. Check if this Usermessage [msgId=" + msgId
                        + "] has already been delivered");

            boolean isDuplicate = false;
            isDuplicate = HolodeckB2BCore.getQueryManager().isAlreadyProcessed(um);
            if (isDuplicate) {
                log.debug("UserMessage [msgId=" + msgId + "] has already been processed");
                // Also log in special duplicate log
                duplicateLog.info("UserMessage [msgId=" + msgId
                                            + "] is a duplicate of an already processed message");
                log.trace("Update processing state to duplicate");
                HolodeckB2BCore.getStorageManager().setProcessingState(um, ProcessingState.DUPLICATE);
                log.trace("Check if duplicate failed");
                if (HolodeckB2BCore.getQueryManager().getMessageUnitsWithId(msgId, Direction.IN).parallelStream()
                		.map(m -> m.getCurrentProcessingState().getState())
                		.anyMatch(s -> s == ProcessingState.FAILURE)) {
                	log.debug("Duplicate failed, set processing state again to failure");
                	HolodeckB2BCore.getStorageManager().setProcessingState(um, ProcessingState.FAILURE);                			
                }
                // Raise message processing event to inform other components of the duplicate message
                HolodeckB2BCore.getEventProcessor().raiseEvent(new DuplicateReceivedEvent(um));
                
            }

            return InvocationResponse.CONTINUE;
        }
    }
}
