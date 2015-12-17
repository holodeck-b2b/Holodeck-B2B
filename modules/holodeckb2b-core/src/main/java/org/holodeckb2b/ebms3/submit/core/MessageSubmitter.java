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
package org.holodeckb2b.ebms3.submit.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.PModeFinder;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;

/**
 * Is the default implementation of {@see IMessageSubmitter}.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageSubmitter implements IMessageSubmitter {
    
    private static final Log log = LogFactory.getLog(MessageSubmitter.class.getName());
    
    @Override
    public String submitMessage(IUserMessage um) throws MessageSubmitException {
        
        try {
            log.debug("Find the P-Mode for the message");
            IPMode  pmode = PModeFinder.forSubmitted(um);
            
            if (pmode == null) {
                log.warn("No P-Mode found for submitted message, rejecting message!");
                throw new MessageSubmitException("No P-Mode found for message");
            }
            
            log.debug("Check for completeness: combined with P-Mode all info must be known");
            IUserMessage completedMMD = MMDCompleter.complete(um, pmode); // Throws MessageSubmitException if meta-data is not complete
            
            log.debug("Checking availability of payloads");
            checkPayloads(completedMMD, pmode); // Throws MessageSubmitException if there is a problem with a specified payloads
            
            log.debug("Add message to database");
            UserMessage newUM = MessageUnitDAO.createOutgoingUserMessage(completedMMD, pmode.getId());
            
            //Use P-Mode to find out if this message is to be pulled or pushed to receiver
            if (EbMSConstants.ONE_WAY_PULL.equalsIgnoreCase(pmode.getMepBinding())) {
                log.debug("Message is to be pulled by receiver, change ProcessingState to wait for pull");
                MessageUnitDAO.setWaitForPull(newUM);
            } else {
                log.debug("Message is to be pushed to receiver, change ProcessingState to trigger push");
                MessageUnitDAO.setReadyToPush(newUM);
            }
            
            log.info("Message succesfully submitted");
            return newUM.getMessageId();
            
        } catch (DatabaseException dbe) {
            log.error("An error occured when saving user message to database. Details: " + dbe.getMessage());
            throw new MessageSubmitException("Message could not be saved to database", dbe);
        }
    }

    /**
     * Helper method to check availability of the payloads.
     * @todo: Also check compliance with payload profile of PMode!
     * 
     * @param um     The meta data on the submitted user message
     * @param pmode  The P-Mode that governs the processing this user message
     * @throws MessageSubmitException When one of the specified payloads can not be found or when the specified path is
     *                                is not a regular file
     */
    private void checkPayloads(IUserMessage um, IPMode pmode) throws MessageSubmitException {        
        if (!Utils.isNullOrEmpty(um.getPayloads()))
            for(IPayload p : um.getPayloads()) {
                String contentLocation = p.getContentLocation();
                // The location must be specified
                if (contentLocation == null || contentLocation.isEmpty())
                    throw new MessageSubmitException("No location specified for payload [uri=" + p.getPayloadURI() + "]!");
                try { 
                    // It most point to an existing normal file
                    if (!Files.isRegularFile(Paths.get(contentLocation))) 
                        throw new Exception("Not a regular file");
                } catch (Exception e) {
                    // This will only be reach if either an exception occurred while checking the location or when the
                    // given location does not point to a regular file 
                    throw new MessageSubmitException("Specified location of payload [uri=" + p.getPayloadURI() 
                                + "] content [" + contentLocation + "] does not exist or is not a regular file!");
                }
            }
    }


    
}
