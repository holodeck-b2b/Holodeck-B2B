/*
 * Copyright (C) 2016 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.events.processing;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;

/**
 * Defines the interface of the Holodeck B2B Core component that is responsible for the processing of {@link
 * IMessageProcessingEvent}s. When a component want to inform others about an event that occurred in the processing of a
 * message unit it MUST use the event processor to <i>"raise"</i> the event. The event processor will then ensure that
 * the event is handled as configured in the P-Mode that governs the processing of the message unit. To get access to
 * the running event processor use the {@link HolodeckB2BCoreInterface#getEventProcessor()} method.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 */
public interface IMessageProcessingEventProcessor {

    /**
     * Raises an event for processing.
     * <p>Because the event is only to inform about the message processing but not part of it the implementation will
     * ensure that message processing is not affected, i.e. not change any information of the referenced message unit
     * and not throw any exception.
     *
     * @param event         The event that occurred while processing the message unit and that should be processed
     * @param msgContext    The Axis2 {@link MessageContext} of the message unit the event applies to, if available.
     */
    public void raiseEvent(IMessageProcessingEvent event, MessageContext msgContext);
}
