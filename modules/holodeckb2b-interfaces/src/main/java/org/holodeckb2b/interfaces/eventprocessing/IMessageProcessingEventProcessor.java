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
package org.holodeckb2b.interfaces.eventprocessing;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.security.ISecurityCreationFailure;
import org.holodeckb2b.interfaces.events.security.ISigningFailure;
import org.holodeckb2b.interfaces.pmode.ILeg;

/**
 * Defines the interface of the Holodeck B2B Core component that is responsible for the processing of {@link
 * IMessageProcessingEvent}s. When a component want to inform others about an event that occurred in the processing of a
 * message unit it MUST use the event processor to <i>"raise"</i> the event. The event processor will then ensure that
 * the event is handled as configured in either the global configuration (as registered with the Core) or the P-Mode 
 * governing the processing of the message unit to which the event applies. When both the P-Mode and the global 
 * configuration define a handler for the same event the one in the P-Mode takes precedence.   
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 * @since 4.1.0 Requirement to take the global event configuration into account when processing the event.
 * @see ILeg#getMessageProcessingEventConfiguration()
 * @see HolodeckB2BCoreInterface#getMessageProcessingEventConfiguration()
 */
public interface IMessageProcessingEventProcessor {

    /**
     * Raises an event for processing by the configured event handler.
     * <p>Because the event is only to inform about the message processing but not part of it the implementation MUST
     * ensure that message processing is not affected, i.e. not change any information of the referenced message unit
     * and not throw any exception. 
     * <p>NOTE: Only the first event handler in the configured set able to handle the given event is executed. Therefore
     * configure the handlers for specific events first before the ones handling more generic events, e.g. 
     * {@link ISigningFailure} before {@link ISecurityCreationFailure}.
     *
     * @param event         The event that occurred while processing the message unit and that should be processed
     */
    public void raiseEvent(IMessageProcessingEvent event);
}
