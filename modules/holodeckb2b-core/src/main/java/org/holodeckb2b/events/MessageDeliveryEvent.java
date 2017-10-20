/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.events;

import org.holodeckb2b.common.events.AbstractMessageProcessingEvent;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.delivery.IMessageDeliveryEvent;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link IMessageDeliveryEvent} to inform external components that an attempt to deliver
 * a message unit to the business application was executed.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class MessageDeliveryEvent extends AbstractMessageProcessingEvent implements IMessageDeliveryEvent {
    /**
     * Indicates whether the message unit was successful delivered
     */
    private final boolean successFulDelivery;
    /**
     * The exception that caused the delivery attempt to fail
     */
    private final MessageDeliveryException failureReason;

    public MessageDeliveryEvent(final IMessageUnit subject, final boolean successFulDelivery,
                                final MessageDeliveryException failureReason) {
        super(subject);
        this.successFulDelivery = successFulDelivery;
        if (successFulDelivery && failureReason != null)
            throw new IllegalArgumentException("Failure reason may only be specified in case of unsuccessful delivery");
        this.failureReason = failureReason;
    }

    @Override
    public boolean isDeliverySuccessful() {
        return successFulDelivery;
    }

    @Override
    public MessageDeliveryException getFailureReason() {
        return failureReason;
    }
}
