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
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
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
    public String submitMessage(final IUserMessage um) throws MessageSubmitException {
        return submitMessage(um, false);
    }

    /**
     * Submits the specified <b>User Message</b> to Holodeck B2B for sending.
     * <p>Whether the message will be sent immediately depends on the P-Mode that applies and the MEP being specified
     * therein. If the MEP is Push the Holodeck B2B will try to send the message immediately. When the MEP is Pull the
     * message is stored for retrieval by the receiving MSH.
     * <p><b>NOTE:</b> This method MAY return before the message is actually sent to the receiver. Successful return
     * ONLY GUARANTEES that the message CAN be sent to the receiver and that Holodeck B2B will try to do so.
     * <p>It is NOT REQUIRED that the meta data contains a reference to the P-Mode that should be used to handle the
     * message. The first action of a <i>MessageSubmitter</i> is to find the correct P-Mode for the user message. It is
     * however RECOMMENDED to include the P-Mode id to prevent mismatches.
     *
     * @param um                    The meta data on the user message to be sent to the other trading partner.
     * @param movePayloads          Indicator whether the files containing the payload data must be deleted or not
     * @return                      The ebMS message-id assigned to the user message.
     * @throws MessageSubmitException   When the user message can not be submitted successfully. Reasons for failure can
     *                                  be that no P-Mode can be found to handle the message or the given P-Mode
     *                                  conflicts with supplied meta-data.
     */
    @Override
    public String submitMessage(final IUserMessage um, final boolean movePayloads) throws MessageSubmitException {
        log.trace("Start submission of new User Message");

        try {
            log.debug("Get the P-Mode for the message");
            final IPMode  pmode = HolodeckB2BCoreInterface.getPModeSet().get(um.getPModeId());

            if (pmode == null) {
                log.warn("No P-Mode found for submitted message, rejecting message!");
                throw new MessageSubmitException("No P-Mode found for message");
            }
            log.debug("Found P-Mode:" + pmode.getId());

            log.debug("Check for completeness: combined with P-Mode all info must be known");
            final IUserMessage completedMMD = MMDCompleter.complete(um, pmode); // Throws MessageSubmitException if meta-data is not complete

            log.debug("Checking availability of payloads");
            checkPayloads(completedMMD, pmode); // Throws MessageSubmitException if there is a problem with a specified payloads

            log.debug("Add message to database");
            final EntityProxy<UserMessage> newUM = MessageUnitDAO.createOutgoingUserMessage(completedMMD, pmode.getId());

            try {
                moveOrCopyPayloads(newUM, movePayloads);
            } catch (final IOException ex) {
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

            log.info("User Message succesfully submitted");
            return newUM.entity.getMessageId();

        } catch (final DatabaseException dbe) {
            log.error("An error occured when saving user message to database. Details: " + dbe.getMessage());
            throw new MessageSubmitException("Message could not be saved to database", dbe);
        }
    }

    /**
     * Submits the specified <b>Pull Request</b> to Holodeck B2B for sending.
     * <p>With this submission the business application that expects to receive a User Message, i.e. the <i>Consumer</i>
     * in ebMS specification terminology, can control the moments when the pull operation must be performed. Holodeck
     * B2B will try to send the message directly.
     * <p>The meta-data for the Pull Request MUST contain both the MPC and P-Mode [id]. A messageId MAY be included in
     * the submission, but is NOT RECOMMENDED to include one and let Holodeck B2B generate one.
     *
     * @param pullRequest    The meta-data on the pull request that should be sent.
     * @return               The ebMS message-id assigned to the pull request.
     * @throws MessageSubmitException   When the pull request can not be submitted successfully. Reasons for failure can
     *                                  be that the P-Mode can not be found or the given P-Mode and MPC conflict.
     * @since  2.1.0
     */
    @Override
    public String submitMessage(final IPullRequest pullRequest) throws MessageSubmitException {
        log.trace("Start submission of new Pull Request");

        // Check if P-Mode id and MPC are specified
        if (Utils.isNullOrEmpty(pullRequest.getPModeId()))
            throw new MessageSubmitException("P-Mode Id is missing");
        if (Utils.isNullOrEmpty(pullRequest.getMPC()))
            throw new MessageSubmitException("MPC is missing");

        String prMessageId = null;
        try {
            log.debug("Create and add PullRequest to database");
            prMessageId = MessageUnitDAO.createOutgoingPullRequest(pullRequest).entity.getMessageId();
            log.info("Submitted PullRequest, assigned messageId=" + prMessageId);
        } catch (final DatabaseException ex) {
            log.error("Could not create the PullRequest because a error occurred in the database! Details: "
                        + ex.getMessage());
        }

        return prMessageId;
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
    private void checkPayloads(final IUserMessage um, final IPMode pmode) throws MessageSubmitException {
        final Collection<IPayload> payloads = um.getPayloads();
        if (!Utils.isNullOrEmpty(payloads))
            for(final IPayload p : payloads) {
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
    private boolean checkContent(final IPayload payload) {
        if (payload.getContainment() != IPayload.Containment.EXTERNAL) {
            final String contentLocation = payload.getContentLocation();
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
    private boolean checkPayloadRefs(final IPayload p, final Collection<IPayload> payloads) {
        boolean c = true;
        final Iterator<IPayload> it = payloads.iterator();
        do {
            final IPayload p1 = it.next();
            final String   r0 = p.getPayloadURI(), r1 = p1.getPayloadURI();
            c = (p == p1)  // Same object, so always okay
                || p.getContainment() != p1.getContainment() // The containment differs
                    // The containment is attachment, so URI's should be different or both null
                || (p.getContainment() == IPayload.Containment.ATTACHMENT
                    && ((r0 == null && r1 == null) || !Utils.nullSafeEqualIgnoreCase (r0, r1)))
                    // The containment is body or external, URI should be different and not null
                || (r0 != null && r1 != null && !r0.equalsIgnoreCase(r1));
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
    private void moveOrCopyPayloads(final EntityProxy<UserMessage> um, final boolean move) throws IOException, DatabaseException {
        // Path to the "temp" dir where to store payloads during processing
        final String intPlDir = HolodeckB2BCoreInterface.getConfiguration().getTempDirectory() + "plcout";
        // Create the directory if needed
        final Path pathPlDir = Paths.get(intPlDir);
        if (!Files.exists(pathPlDir)) {
            log.debug("Create the directory [" + intPlDir + "] for storing payload files");
            Files.createDirectories(pathPlDir);
        }

        final Collection<IPayload> payloads = um.entity.getPayloads();
        if (!Utils.isNullOrEmpty(payloads)) {
            for (final IPayload ip : payloads) {
                final Payload p = (Payload) ip;
                final Path srcPath = Paths.get(p.getContentLocation());
                // Ensure that the filename in the temp directory is unique
                final Path destPath = Utils.createFileWithUniqueName(intPlDir + "/" + srcPath.getFileName());
                try {
                    if (move) {
                        log.debug("Moving payload [" + p.getContentLocation() + "] to internal directory");
                        Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        log.debug("Copying payload [" + p.getContentLocation() + "] to internal directory");
                        Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log.debug("Payload moved/copied to internal directory");
                    p.setContentLocation(destPath.toString());
                } catch (IOException io) {
                    log.error("Could not copy/move the payload [" + p.getContentLocation() + "] to internal directory"
                             + " [" + intPlDir + "].\n\tError details: " + io.getMessage());
                    // Remove the already created file for storing the payload
                    try {
                        Files.deleteIfExists(destPath);
                    } catch (IOException removeFailure) {
                        log.error("Could not remove the temporary payload file [" + destPath.toString() + "]!" +
                                  " Please remove manually.");
                    }
                    throw io;
                }
            }
            // Update the database with new locations
            MessageUnitDAO.updateMessageUnitInfo(um);
        }
    }

}
