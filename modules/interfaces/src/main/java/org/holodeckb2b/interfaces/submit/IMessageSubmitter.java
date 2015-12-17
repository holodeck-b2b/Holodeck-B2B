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
package org.holodeckb2b.interfaces.submit;

/*
 * #%L
 * Holodeck B2B - Interfaces
 * %%
 * Copyright (C) 2015 The Holodeck B2B Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Describes the interface that the Holodeck B2B core exposes to create a new 
 * ebMS User Message message unit for sending.
 * <p>Note that this is more or less an internal interface intended for use by 
 * helper classes that handle the submission of business data from the client
 * application.<br> 
 * By decoupling the internal and external interface it is easier to implement
 * different protocols for accepting message from the client applications without
 * these "acceptors" to know about the Holodeck B2B internals.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IMessageSubmitter {

    /**
     * Submits the specified user message to Holodeck B2B for sending. 
     * <p>Whether the message will be sent immediately depends on the P-Mode that
     * applies and the MEP being specified therein. If the MEP is Push the Holodeck B2B
     * will try to send the message immediately. When the MEP is Pull the message
     * is stored for retrieval.
     * <p><b>NOTE:</b> This method MAY return before the message is actually sent
     * to the receiver. Successful return ONLY GUARANTEES that the message CAN
     * be sent to the receiver and that Holodeck B2B will try to do so. 
     * <p>It is NOT REQUIRED that the meta data contains a reference to the P-Mode 
     * that should be used to handle the message. The first action of a <i>MessageSubmitter</i>
     * is to find the correct P-Mode for the user message.
     * 
     * @param um    The meta data on the user message to be sent to the other
     *              trading partner.
     * @return      The ebMS message-id assigned to the user message. 
     * @throws MessageSubmitException   When the user message can not be submitted
     *                                  successfully. 
     *                                  Reasons for failure can be that no P-Mode
     *                                  can be found to handle the message or the
     *                                  given P-Mode conflicts with the one found.
     */
    public String submitMessage(IUserMessage um) throws MessageSubmitException;
}
