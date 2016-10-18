/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.submit;


import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Describes the interface the Holodeck B2B Core exposes to create a new ebMS message unit for sending. Since version
 * 2.1.0 the Submitter must also be able to accept a pull request for submission.
 * <p>Note that this is more or less an internal interface intended for use by helper classes that handle the submission
 * of business data from the client application (called the <i>Producer</i> in the ebMS V3 Specification).<br>
 * By decoupling the internal and external interface it is easier to implement different protocols for accepting
 * messages from the client applications without these "acceptors" needing to know about the Holodeck B2B internals.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IMessageSubmitter {

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
     * <p><b>NOTE:<b> This method is <b>DEPRECATED</b> and should not be used anymore for new developments! Use the new
     * method where you can specify handling of the payloads, i.e. whether they should be deleted from their current
     * location or not.
     *
     * @param um    The meta data on the user message to be sent to the other trading partner.
     * @return      The ebMS message-id assigned to the user message.
     * @throws MessageSubmitException   When the user message can not be submitted successfully. Reasons for failure can
     *                                  be that no P-Mode can be found to handle the message or the given P-Mode
     *                                  conflicts with supplied meta-data.
     */
    @Deprecated
    public String submitMessage(IUserMessage um) throws MessageSubmitException;

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
     * @param deletePayloadFiles    Indicator whether the files containing the payload data must be deleted or not
     * @return                      The ebMS message-id assigned to the user message.
     * @throws MessageSubmitException   When the user message can not be submitted successfully. Reasons for failure can
     *                                  be that no P-Mode can be found to handle the message or the given P-Mode
     *                                  conflicts with supplied meta-data.
     */
    public String submitMessage(IUserMessage um, boolean deletePayloadFiles) throws MessageSubmitException;

    /**
     * Submits the specified <b>Pull Request</b> to Holodeck B2B for sending.
     * <p>With this submission the business application that expects to receive a User Message, i.e. the <i>Consumer</i>
     * in ebMS specification terminology, can control the moments when the pull operation must be performed. Holodeck
     * B2B will try to send the message directly.
     * <p>The meta-data for the Pull Request MUST contain both the MPC and P-Mode [id]. A messageId SHOULD NOT be
     * included in the submission as Holodeck B2B will generate one.
     *
     * @param pr    The meta-data on the pull request that should be sent.
     * @return      The ebMS message-id assigned to the pull request.
     * @throws MessageSubmitException   When the pull request can not be submitted successfully. Reasons for failure can
     *                                  be that the P-Mode can not be found or the given P-Mode and MPC conflict.
     * @since  2.1.0
     */
    public String submitMessage(IPullRequest pr) throws MessageSubmitException;
}
