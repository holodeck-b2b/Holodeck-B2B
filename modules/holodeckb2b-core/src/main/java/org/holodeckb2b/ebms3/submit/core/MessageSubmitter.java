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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.persistency.entities.Payload;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.util.PModeFinder;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
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
    @Deprecated
    public String submitMessage(IUserMessage um) throws MessageSubmitException {
        return submitMessage(um, false);
    }
    
    @Override
    public String submitMessage(IUserMessage um, boolean movePayloads) throws MessageSubmitException {
        
        try {
            log.debug("Find the P-Mode for the message");
            IPMode  pmode = PModeFinder.forSubmitted(um);
            
            if (pmode == null) {
                log.warn("No P-Mode found for submitted message, rejecting message!");
                throw new MessageSubmitException("No P-Mode found for message");
            }
            log.debug("Found P-Mode:" + pmode.getId());
            
            log.debug("Check for completeness: combined with P-Mode all info must be known");
            IUserMessage completedMMD = MMDCompleter.complete(um, pmode); // Throws MessageSubmitException if meta-data is not complete
            
            log.debug("Checking availability of payloads");
            checkPayloads(completedMMD, pmode); // Throws MessageSubmitException if there is a problem with a specified payloads
            
            log.debug("Add message to database");
            EntityProxy<UserMessage> newUM = MessageUnitDAO.createOutgoingUserMessage(completedMMD, pmode.getId());
            
            try {
                moveOrCopyPayloads(newUM, movePayloads);
            } catch (IOException ex) {
                log.error("Could not move/copy payload(s) to the internal storage! Unable to process message!" 
                            + "\n\tError details: " + ex.getMessage());
                throw new MessageSubmitException("Could not move/copy payload(s) to the internal storage!", ex);
            }
            
            //Use P-Mode to find out if this message is to be pulled or pushed to receiver
            if (EbMSConstants.ONE_WAY_PULL.equalsIgnoreCase(pmode.getMepBinding())) {
                log.debug("Message is to be pulled by receiver, change ProcessingState to wait for pull");
                MessageUnitDAO.setWaitForPull(newUM);
            } else {
                log.debug("Message is to be pushed to receiver, change ProcessingState to trigger push");
                MessageUnitDAO.setReadyToPush(newUM);
            }
            
            log.info("Message succesfully submitted");
            return newUM.entity.getMessageId();
            
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
        Collection<IPayload> payloads = um.getPayloads();
        if (!Utils.isNullOrEmpty(payloads))
            for(IPayload p : payloads) {
                // Check that content is available
                if (!checkContent(p))
                    throw new MessageSubmitException("Specified location of payload [uri=" + p.getPayloadURI() 
                                + "] content [" + p.getContentLocation() 
                                + "] is invalid (non existing or not a regular file!");
                // Check references
                if (!checkPayloadRefs(p, payloads))
                    throw new MessageSubmitException("Specified location of payload [uri=" + p.getPayloadURI() 
                                + "] content [" + p.getContentLocation() 
                                + "] is invalid (non existing or not a regular file!");                                    
            }
    }
    
    /**
     * Helper method to check that the specified file for the payload content is available if the payload should be
     * included in the message (containment is either BODY or ATTACHMENT)
     * 
     * @param payload   The payload meta-data
     * @return          <code>true</code> when the payload should be contained in the message and a file is available 
     *                  at the specified path or when the payload is externally referenced, <br>
     *                  <code>false</code> otherwise
     */
    private boolean checkContent(IPayload payload) {
        if (payload.getContainment() != IPayload.Containment.EXTERNAL) {
            String contentLocation = payload.getContentLocation();
            // The location must be specified
            if (Utils.isNullOrEmpty(contentLocation))
                return false;
            else 
                // It most point to an existing normal file
                if (Files.isRegularFile(Paths.get(contentLocation))) 
                    return true;
                else 
                    return false;
        } else
            // For external payload content location is not relevant
            return true;
    }

    /**
     * Helper method to check the URI reference for the payloads to include in the message.
     * <p>The references must be unique within the set of simularly included payloads, so there must be no other payload 
     * with the same containment and reference. 
     * <p>Payloads that should be included as attachment however don't need to have a reference assigned by the 
     * Producing application, so here we accept that multiple <code>null</code> values exist.
     * 
     * @param p         The meta-data on the payload to check the reference for
     * @param paylaods  The meta-data on all payloads included in the message 
     * @return          <code>true</code> if the references are unique for each payload,<br>
     *                  <code>false</code> if duplicates exists
     */
    private boolean checkPayloadRefs(IPayload p, Collection<IPayload> payloads) {
        boolean c = true;
        Iterator<IPayload> it = payloads.iterator();
        do {
            IPayload p1 = it.next();
            String   r0 = p.getPayloadURI(), r1 = p1.getPayloadURI();
            c = (p == p1)  // Same object, so always okay
                || p.getContainment() != p1.getContainment() // The containment differs
                    // The containment is attachment, so URI's should be different or both null
                || (p.getContainment() == IPayload.Containment.ATTACHMENT 
                    && (r0 == r1 || !r0.equalsIgnoreCase(r1)))  
                    // The containment is body or external, URI should be different and not null
                || (r0 != r1 && !r0.equalsIgnoreCase(r1));                
        } while (c && it.hasNext());       
        return c;
    }
    
    /**
     * Helper method to copy or move the payloads to an internal directory so they will be kept available during the
     * processing of the message (which may include resending).
     * 
     * @param um     The meta data on the submitted user message
     * @param pmode  The P-Mode that governs the processing this user message
     * @throws IOException  When the payload could not be moved/copied to the internal payload storage
     * @throws DatabaseException When the new payload locations couldn't be saved to the database
     */
    private void moveOrCopyPayloads(EntityProxy<UserMessage> um, boolean move) throws IOException, DatabaseException {
        // Path to the "temp" dir where to store payloads during processing
        String intPlDir = HolodeckB2BCoreInterface.getConfiguration().getTempDirectory() + "plcout";
        // Create the directory if needed
        Path pathPlDir = Paths.get(intPlDir);
        if (!Files.exists(pathPlDir)) {
            log.debug("Create the directory [" + intPlDir + "] for storing payload files");
            Files.createDirectories(pathPlDir);
        }

        Collection<IPayload> payloads = um.entity.getPayloads();
        if (!Utils.isNullOrEmpty(payloads)) {
            for (IPayload ip : payloads) {
                Payload p = (Payload) ip;                
                Path srcPath = Paths.get(p.getContentLocation());
                // Ensure that the filename in the temp directory is unique
                Path destPath = Paths.get(Utils.preventDuplicateFileName(intPlDir + "/" + srcPath.getFileName()));
                if (move) {
                    log.debug("Moving payload [" + p.getContentLocation() + "] to internal directory");
                    Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    log.debug("Copying payload [" + p.getContentLocation() + "] to internal directory");
                    Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
                log.debug("Payload moved/copied to internal directory");
                p.setContentLocation(destPath.toString());
            }
            // Update the database with new locations
            MessageUnitDAO.updateMessageUnitInfo(um);
        }
    }
    
}
