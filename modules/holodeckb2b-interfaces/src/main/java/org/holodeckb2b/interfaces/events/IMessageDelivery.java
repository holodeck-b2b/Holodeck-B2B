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

/**
 * Is the <i>message processing event</i> that indicates that an attempt to deliver a <i>User Message</i> message unit
 * to the business application was executed.
 * <p>When the delivery attempt failed, i.e. a {@link MessageDeliveryException} was thrown in the delivery process, the
 * cause of failure is included in the event.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IMessageDelivery extends IMessageProcessingEvent, IMessageDeliveryEvent {

    /**
     * Indicates whether the delivery attempt was successful or not, i.e. if no {@link MessageDeliveryException}s
     * were thrown.
     *
     * @return  <code>true</code> if the delivery attempt was successful, or<br><code>false</code> otherwise
     */
    @Override
	boolean isDeliverySuccessful();

    /**
     * Gets the {@link MessageDeliveryException} that was thrown during the message delivery attempt.
     * <p>NOTE: This method should only be called when the {@link #isDeliverySuccessful()} return <code>false</code>.
     *
     * @return  The exception that caused the message delivery failure
     */
    @Override
	MessageDeliveryException getFailureReason();
}

