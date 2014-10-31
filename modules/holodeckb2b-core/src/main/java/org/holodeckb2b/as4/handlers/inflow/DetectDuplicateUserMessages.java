/*
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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.as4.pmode.ILegAS4;
import org.holodeckb2b.common.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for detecting and when requested eliminating duplicate <i>user messages</i>.
 * This functionality is part of the <i>Reception Awareness</i> feature specified in the AS4 profile (see section 3.2).
 * <p>The detection of duplicates is done by checking for an existing {@link UserMessage} with the same 
 * <code>MessageId</code> and which is in {@link ProcessingStates#DELIVERED} state. This means the detection window
 * is determined by the time messages stay in the message log.
 * <p>How a duplicate should be handled is configured by the P-Mode parameter <b>ReceptionAwareness.DuplicateDetection.Eliminate</b>.
 * When set to <code>true</code> the duplicate will not be processed and its processing state set to {@link ProcessingStates#DUPLICATE}.
 * Because the duplicate may be a retry due to a missing Receipt signal a new Receipt will be sent as response. This is
 * done by marking this message unit as delivered.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DetectDuplicateUserMessages extends AbstractUserMessageHandler {

    /**
     * Duplicates will always be logged to a special log. Using the logging
     * configuration users can decide if this logging should be enabled and
     * how duplicates should be logged.
     */
    private Log     duplicateLog = LogFactory.getLog("org.holodeckb2b.msgproc.duplicates");
    
    @Override
    protected byte inFlows() {
        return IN_FLOW;
    }
    
    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws AxisFault {
        
        // First determine if duplicate check must be executed for this UserMessage 
        //
        boolean detectDups = false;
        log.debug("Check if duplicate check must be executed");
        
        // Get P-Mode configuration
        IPMode pmode = HolodeckB2BCore.getPModeSet().get(um.getPMode());
        if (pmode == null) {
            // The P-Mode configurations has changed and does not include this P-Mode anymore, assume no receipt
            // is needed
            log.error("P-Mode " + um.getPMode() + " not found in current P-Mode set!" 
                        + "Unable to determine if receipt is needed for message [msgId=" + um.getMessageId() + "]");
            return InvocationResponse.CONTINUE;
        }
        // Currently we only support one-way MEPs so the leg is always the first one
        ILeg leg = pmode.getLegs().iterator().next();        
        
        // Duplicate detection is part of the AS4 Reception Awareness feature which can only be configured on a leg
        // of type ILegAS4, so check type
        if (!(leg instanceof ILegAS4)) 
            // Not an AS4 leg, so no duplicate detection
            detectDups = false;
        else {
            // Get configuration of Reception Awareness feature
            IReceptionAwareness raConfig = ((ILegAS4) leg).getReceptionAwareness();
            if (raConfig != null)
                detectDups = raConfig.useDuplicateDetection();
            else
                detectDups = false;
        }
        
        if (!detectDups) {
            log.debug("Duplicate detection not enabled, skipping check.");
            return InvocationResponse.CONTINUE;
        } else {
            String msgId = um.getMessageId();
            log.debug("Duplicate detection enabled. Check if this Usermessage [msgId=" + msgId 
                        + "] has already been delivered");
            
            boolean isDuplicate = false;
            try {
                isDuplicate = MessageUnitDAO.isUserMsgDelivered(msgId);
            
                if (isDuplicate) {
                    log.debug("UserMessage [msgId=" + msgId + "] has already been delivered");
                    // Also log in special duplicate log
                    duplicateLog.info("UserMessage [msgId=" + msgId 
                                                + "] is a duplicate of an already delivered message");

                    log.debug("Update processing state to duplicate");
                    MessageUnitDAO.setDuplicate(um);
                    
                    // To prevent repeated delivery but still send a receipt set message as delivered
                    mc.setProperty(MessageContextProperties.DELIVERED_USER_MSG, true);
                }
            } catch (DatabaseException ex) {
                // Oops, something went wrong saving the data
                log.error("A database error occurred when checking for duplicates. Details: " + ex.getMessage());
                
                // Continue processing, as other parts of the message may be processed succesfully
            }

            return InvocationResponse.CONTINUE;
        }
    }
}
