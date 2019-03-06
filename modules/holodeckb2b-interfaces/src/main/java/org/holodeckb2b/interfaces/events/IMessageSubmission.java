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

import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;

/**
 * Is the <i>message processing event</i> that indicates that the business application submitted a message unit to the 
 * Holodeck B2B Core for sending. The event is raised for both <i>User Message</i> and <i>Pull Request</i> messages. 
 * <p>When the submission failed, i.e. a {@link MessageSubmitException} was thrown during the submission process, the
 * cause of failure is included in the event.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IMessageSubmission extends IMessageProcessingEvent {

    /**
     * Indicates whether the submission was successful or not, i.e. that no {@link MessageDeliveryException}s
     * were thrown during submission and the message can be send to the trading partner 
     *
     * @return  <code>true</code> if the submission was successful, or<br><code>false</code> otherwise
     */
	boolean isSubmissionSuccessful();

    /**
     * Gets the {@link MessageSubmitException} that was thrown during the submission process.
     * <p>NOTE: This method should only be called when the {@link #isSubmissionSuccessful()} return <code>false</code>.
     *
     * @return  The exception that caused the message submission failure
     */
	MessageSubmitException getFailureReason();
}

