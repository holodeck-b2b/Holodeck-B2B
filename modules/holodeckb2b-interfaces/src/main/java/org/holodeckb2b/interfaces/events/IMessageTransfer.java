/*
 * Copyright (C) 2018 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.events;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;

/**
 * Is the <i>message processing event</i> that indicates that there was an attempt to transfer a message unit to the
 * other MSH, either by sending it as a request or including it as a response. The event is raised for all message units
 * and for both successful and unsuccessful attempts.
 * <p>NOTE: The indication whether the transfer is successful or not is from the perspective of Holodeck B2B and does
 * not guarantee that the message is also received correctly by the other MSH. To detect this the AS4 Reception
 * Awareness feature should be used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IMessageTransfer extends IMessageProcessingEvent, ISendMessageProcessingFailure {

    /**
     * Indicates whether the transfer of the message unit was successful.
     * <p>If there was a problem during the transfer the {@link #getFailureReason()} method can be used to get details
     * on the problem that occurred.
     *
     * @return  <code>true</code> if the transfer was successful,<br>
     *          <code>false</code> if an exception occurred in the exchange.
     */
	boolean isTransferSuccessful();

    /**
     * Gets the exception that caused the transfer to fail.
     * <p>This method should only be called in case {@link #isTransferSuccessful()} returns <code>false</code>.
     *
     * @return  The exception that caused the transfer to fail
     */
	Exception getFailureReason();
}
