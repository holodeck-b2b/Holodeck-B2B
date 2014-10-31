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

package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.ValueNotRecognized;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.ebms3.util.PModeFinder;

/**
 * Is the <i>IN_FLOW</i> handler responsible for determining the P-Mode that defines how the received user message
 * should be processed. Without a P-Mode messages can not be processed by Holodeck B2B, so when no P-Mode is found
 * message processing is stopped; the processing state of the message unit is set to <i>FAILED</i> and the message
 * unit is removed from the message context to prevent further processing.
 * <p>NOTE: The actual P-Mode finding is done in {@link PModeFinder}.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class FindPModeForUserMsg extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW;
    }
    
    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws AxisFault {
        
        log.debug("Find the P-Mode for this message");
        IPMode pmode = PModeFinder.find(um);
        
        if (pmode == null) {
            // No matching P-Mode could be found for this message, return error
            log.error("No P-Mode found for message [" + um.getMessageId() + "], unable to process it!");
            ValueNotRecognized   noPmodeIdError = new ValueNotRecognized();
            noPmodeIdError.setRefToMessageInError(um.getMessageId());
            noPmodeIdError.setErrorDetail("Can not process message [msgId=" + um.getMessageId() + "] because no P-Mode"
                                            + " was found for the message!");
            MessageContextUtils.addGeneratedError(mc, noPmodeIdError);
            
            log.debug("Set the processing state of this user message to failure");
            try {
                MessageUnitDAO.setFailed(um);
            } catch (DatabaseException ex) {
                // Oops, something went wrong saving the data
                log.error("A error occurred when updating the meta data in the database. Details: " + ex.getMessage());
            }
            log.debug("Removing user message object from context");
            mc.removeProperty(MessageContextProperties.IN_USER_MESSAGE);
            
            return InvocationResponse.CONTINUE;
        }
        
        log.debug("Found P-Mode [" + pmode.getId() + "] for message [" + um.getMessageId() + "]");
        
        try {
            MessageUnitDAO.setPMode(um, pmode);
        } catch (DatabaseException ex) {
            // Oops, something went wrong saving the data. 
            log.error("A error occurred when updating the meta data in the database. Details: " + ex.getMessage());
        }
        
        return InvocationResponse.CONTINUE;
    }

}
