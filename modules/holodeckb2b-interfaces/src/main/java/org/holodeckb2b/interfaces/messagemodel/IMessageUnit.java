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
package org.holodeckb2b.interfaces.messagemodel;


import java.util.Date;
import java.util.List;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;

/**
 * Is a general representation for all types of ebMS message units and defines methods to access the information
 * available to all ebMS message units. This is the information contained in the <code>eb:MessageInfo</code> and child
 * elements of the ebMS messaging header. See ebMS V3 Core specification, section 5 for more information on the message
 * header. Added is the relation to the P-Mode that governs the processing of the message unit and, since HB2B_NEXT_VERSION
 * the list of processing states that the message unit is/was in.
 * <p>Descendants of this base interface define how information specific for a type of message unit can be accessed.
 * Together they are used in Holodeck B2B to define the interfaces between the Core and the external <i>business</i>
 * applications. This decoupling allows for more easy extension of both the Core as the external functionality.
 * <p><b>NOTE:</b> The information that is available at some point during runtime depends on the context of processing!
 * If for example a user message is submitted to Holodeck B2B as a response of a Two-Way MEP, only the
 * <i>RefToMessageId</i> might be known.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IUserMessage
 * @see ISignalMessage
 * @see IErrorMessage
 * @see IPullRequest
 * @see IReceipt
 */
public interface IMessageUnit {

    /**
     * Enumeration to define the direction in which the message unit flows.
     *
     * @since HB2B_NEXT_VERSION
     */
    enum Direction { IN, OUT };

    /**
     * Gets the direction in which this message unit is sent, i.e. received or sent by Holodeck B2B.
     *
     * @return The direction in which this message unit flows
     * @since HB2B_NEXT_VERSION
     */
    Direction getDirection();

    /**
     * Gets the timestamp when the message unit was created.
     * <p>Corresponds to the <code>MessageInfo/Timestamp</code> element. See section 5.2.2.1 of the ebMS Core
     * specification.
     *
     * @return  The timestamp when the message unit was created as a {@link Date}
     */
    Date getTimestamp();

    /**
     * Gets the message id of the message unit.
     * <p>Corresponds to the <code>MessageInfo/MessageId</code> element. See section 5.2.2.1 of the ebMS Core
     * specification.
     *
     * @return  The message id as a globally unique identifier conforming to RFC2822.
     */
    String getMessageId();

    /**
     * Get the message id of the message unit to which this message unit is a response.
     * <p>Corresponds to the <code>MessageInfo/RefToMessageId</code> element. See section 5.2.2.1 of the ebMS Core
     * specification.
     *
     * @return  The message id of the message this message unit is a response to
     */
    String getRefToMessageId();

    /**
     * Gets the identifier of the P-Mode that governs the processing of this message unit.
     * <p>Note that the P-Mode may not always be known, for example when a signal message unit is received which can not
     * be related to a sent message.
     *
     * @return  If known, the identifier of the P-Mode that governs processing of this message unit,<br>
     *          otherwise <code>null</code>
     * @since   2.1.0
     */
    String getPModeId();

    /**
     * Gets the list of processing states this message unit was or is in.
     * <p>The order of the processing states as they occur in the list is the same as they applied to the message unit
     * with the last processing state in the list  (i.e. with the highest index) being the current processing state.
     *
     * @return  List of {@link IMessageUnitProcessingState} in the order they applied to this message unit
     * @since  HB2B_NEXT_VERSION
     */
    List<IMessageUnitProcessingState>   getProcessingStates();

    /**
     * Gets the current processing state the message unit is in.
     * <p>Although the current state is the last item in the list that is returned by the {@link #getProcessingStates()}
     * method this method is simpler to use and it also allows implements to optimize the handling of the current
     * processing state.
     *
     * @return  The {@link IMessageUnitProcessingState} the message unit is currently in
     * @since  HB2B_NEXT_VERSION
     */
    IMessageUnitProcessingState getCurrentProcessingState();
}
