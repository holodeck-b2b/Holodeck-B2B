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

package org.holodeckb2b.deliverymethod.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.delivery.MessageDeliveryException;
import org.holodeckb2b.common.messagemodel.IMessageUnit;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.common.messagemodel.IUserMessage;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.mmd.xml.PartInfo;

/**
 * Is an abstract {@link IMessageDeliverer} implementation that implements the delivery of user messages by writing the
 * the message unit info and payload contents to file. The format of the message meta data file has to be implemented in 
 * the subclass.
 * <p>This deliverer also does not implement the delivery of signal messages. If signals have to be delivered to the 
 * business application this should also be implemented in the subclass by overriding {@link #deliverSignalMessage(org.holodeckb2b.common.messagemodel.IMessageUnit)}
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
abstract class AbstractFileDeliverer implements IMessageDeliverer {

    /**
     * The where the files should be stored
     */
    protected String  directory = null;
    
    /**
     * Logger
     */
    protected Log   log = LogFactory.getLog(AbstractFileDeliverer.class);
    
    /**
     * Constructs a new deliverer which will write the files to the given directory.
     * 
     * @param dir   The directory where file should be written to.
     */
    AbstractFileDeliverer(String dir) {
        this.directory = dir;
    }
    
    @Override
    public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
        if (rcvdMsgUnit instanceof IUserMessage) 
            deliverUserMessage(rcvdMsgUnit);
        else // message unit is a signal
            deliverSignalMessage(rcvdMsgUnit);
    }

    /**
     * Delivers the user message to business application.
     * 
     * @param usrMsgUnit        The user message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the user message to the business 
     *                                  application
     */
    protected void deliverUserMessage(IMessageUnit usrMsgUnit) throws MessageDeliveryException {
        log.debug("Delivering user message with msgId=" + usrMsgUnit.getMessageId());

        // We first convert the user message into a MMD document
        MessageMetaData mmd = new MessageMetaData((IUserMessage) usrMsgUnit);

        // Copy the payload files to specified directory and create new list of payload info 
        //  to reflect new locations
        Collection<IPayload>    copiedPLs = new ArrayList<IPayload>();
        try {
            log.debug("Copy all payload files to delivery directory");
            for(IPayload p : mmd.getPayloads()) {
                PartInfo newPLInfo = new PartInfo(p);
                Path newPath = copyPayloadFile(p, mmd.getMessageId());
                if (newPath != null) 
                    newPLInfo.setContentLocation(newPath.toString());
                copiedPLs.add(newPLInfo);
            }
            log.debug("Copied all payload files, set as new payload info in MMD");
            mmd.setPayloads(copiedPLs);
            log.debug("Information complete, write message meta data to file");    
            writeUserMessageInfoToFile(mmd);
            log.info("User message with msgID=" + mmd.getMessageId() + " successfully delivered");
        } catch (IOException ex) {
            log.error("An error occurred while delivering the user message [" + mmd.getMessageId() 
                                                                    + "]\n\tError details: " + ex.getMessage());
            // Something went wrong writing files to the delivery directory, but some payload files
            // may already been copied and should be deleted
            if (!copiedPLs.isEmpty()) {
                log.debug("Remove already copied payload files from delivery directory");
                for(IPayload p : copiedPLs)
                    (new File(p.getContentLocation())).delete();
            }
            // And signal failure
            throw new MessageDeliveryException("Unable to deliver user message [" + mmd.getMessageId() 
                                                    + "]. Error details: " + ex.getMessage());
        }   
    }

    /**
     * Delivers the signal message (Error or Receipt) to business application.
     * 
     * @param sigMsgUnit        The signal message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the signal message to the business 
     *                                  application
     */
    protected abstract void deliverSignalMessage(IMessageUnit sigMsgUnit) throws MessageDeliveryException;

    /**
     * Helper method to copy a the payload content to <i>delivery directory</i>.
     * 
     * @param p         The payload for which the content must be copied
     * @param msgId     The message-id of the message that contains the payload, used for name the file
     * @return          The path where the payload content is now stored
     * @throws IOException  When the payload content could not be copied to the <i>delivery directory</i>
     */
    private Path copyPayloadFile(IPayload p, String msgId) throws IOException {
        // If payload was external to message, it is not processed by Holodeck B2B, so no content to move
        if (IPayload.Containment.EXTERNAL == p.getContainment())
            return null;
        
        // Compose a file name for the payload file, based on msgId and href 
        String plRef = p.getPayloadURI();
        plRef = (plRef == null || plRef.isEmpty() ? "body" : plRef);
        // If this was a attachment the reference is a a MIME Content-id. As these are also quite lengthy we shorten 
        // it to the left part
        if (plRef.indexOf("@") > 0)
            plRef = plRef.substring(0, plRef.indexOf("@"));
        
        Path sourcePath = Paths.get(p.getContentLocation());
        // Try to set nice extension based on MIME Type of payload
        String mimeType = p.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            // No MIME type given in message, try to detect from content
            try { mimeType = Utils.detectMimeType(sourcePath.toFile()); } 
            catch (IOException ex) { mimeType = null; } // Unable to detect the MIME Type                        
        }
        String ext = Utils.getExtension(mimeType);
        
        Path targetPath = Paths.get(Utils.preventDuplicateFileName(directory + "pl-" 
                                                       + (msgId + "-" + plRef).replaceAll("[^a-zA-Z0-9.-]", "_")
                                                       + (ext != null ? ext : "")));
        
        try {
            Files.copy(sourcePath, targetPath);
        } catch (Exception ex) {
            // Can not move payload file -> delivery not possible
            throw new IOException("Unable to deliver message because payload file [" 
                            + p.getContentLocation() + "] can not be moved!", ex);
        }
        
        return targetPath.toAbsolutePath();
    }

    /**
     * Helper method to write the user message meta data to a file.
     * 
     * @param mmd           The user message unit meta data.
     * @throws IOException  When the information could not be written to disk.
     */
    protected abstract void writeUserMessageInfoToFile(MessageMetaData mmd) throws IOException;
    
}