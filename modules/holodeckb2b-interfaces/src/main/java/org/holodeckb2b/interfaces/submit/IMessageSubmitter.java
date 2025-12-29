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


import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;

/**
 * Describes the interface the Holodeck B2B Core exposes to create a new ebMS message unit for sending.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0 ability to submit Pull Request messages
 * @since 8.0.0 ability to submit Payload data before submitting the associated User Message
 */
public interface IMessageSubmitter {

    /**
     * Submits the specified <b>User Message</b> to Holodeck B2B for sending.
     * <p>Whether the message will be sent immediately depends on the P-Mode that applies and the MEP being specified
     * therein. If the MEP is Push the Holodeck B2B will try to send the message immediately. When the MEP is Pull the
     * message is stored for retrieval by the receiving MSH.
     * <p>It is REQUIRED that the meta data contains a reference to the P-Mode that should be used to handle the
     * message. If the P-Mode specifies custom validation (in the User Message flow, see {@link
     * IUserMessageFlow#getCustomValidationConfiguration()}) the submitted message will be validated and only accepted
     * when successfully validated.
     * <p><b>NOTE:</b> This method MAY return before the message is actually sent to the receiver. Successful return
     * ONLY GUARANTEES that the message CAN be sent to the receiver and that Holodeck B2B will try to do so.
     *
     * @param submission            The meta data on the user message to be sent to the other trading partner.
     * @return                      The ebMS message-id assigned to the user message.
     * @throws MessageSubmitException   When the user message can not be submitted successfully. Reasons for failure can
     *                                  be that no P-Mode can be found to handle the message or the given P-Mode
     *                                  conflicts with supplied meta-data. When the submitted User Message refers to
     *                                  pre-submitted payloads this could also indicate an issue with the payloads.
     */
    String submitMessage(IUserMessage submission) throws MessageSubmitException;

    /**
     * Submits the specified <b>Payload</b> to Holodeck B2B for inclusion in a User Message that will be submitted
     * afterwards. This allows to store the payload data before submitting the associated User Message which can be
     * useful when the User Message holds multiple payloads.
     *
     * @param payload	the payload to be submitted
     * @param pmodeId	identifier of the P-Mode that governs the message exchange of the User Message the payload
     * 					will be contained in
     * @return a {@link IPayloadEntity} representing the submitted payload. This object MUST be included in the {@link
     * 			IUserMessage} instance that represents the User Message the submitted payload should be included in and
     * 			which will be passed as argument to {@link #submitMessage(IUserMessage)}
     * @throws MessageSubmitException When the payload can not be submitted successfully, for example because the P-Mode
     *                                with the specified identifier can not be found
     * @since 8.0.0
     */
    IPayloadEntity submitPayload(IPayload payload, String pmodeId) throws MessageSubmitException;

    /**
     * Cancels the submission of the specified payload and removes the payload from the Core. This method should be used
     * when the User Message that would contain the payload will not be submitted and therefore the payloads are not
     * needed any more.
     *
     * @param submittedPayload	meta-data of the payload to be cancelled
     * @throws MessageSubmitException When the payload can not be cancelled. Reasons can be an error that occurred in
     * 								  provider or that the payload is already linked to a User Message
     * @since 8.0.0
     */
    void cancelPayloadSubmission(IPayloadEntity submittedPayload) throws MessageSubmitException;

    /**
     * Submits the specified <b>Pull Request</b> to Holodeck B2B for sending.
     * <p>With this submission the business application that expects to receive a User Message, i.e. the <i>Consumer</i>
     * in ebMS specification terminology, can control the moments when the pull operation must be performed. Holodeck
     * B2B will try to send the message directly.
     * <p>The meta-data for the Pull Request MUST contain the P-Mode [id] and MAY contain a MPC and messageId. If no
     * MPC is defined in either the P-Mode and the submitted Pull Request the <i>default MPC</i> will be used.
     *
     * @param pullRequest    The meta-data on the pull request that should be sent.
     * @return               The ebMS message-id assigned to the pull request.
     * @throws MessageSubmitException   When the pull request can not be submitted successfully. Reasons for failure can
     *                                  be that the P-Mode can not be found or the given P-Mode and MPC conflict.
     * @since  2.1.0
     * @since  4.1.0 Checks that the P-Mode specified can be used for pulling and use of default MPC when none specified
     */
    String submitMessage(IPullRequest pullRequest) throws MessageSubmitException;
}
