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

import java.util.Date;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Defines the interface for an event that (can) occur during message processing. These <i>message processing events</i>
 * should be used to provide additional information to the business application about the processing of a message unit.
 * For example to inform the application that a message unit has been (re)sent.
 * <p>Event are generally triggered by components of the Holodeck B2B Core (see package {@link
 * org.holodeckb2b.interfaces.events.types} for overview of events) but handled by extensions to report them to
 * the external applications. These extension should implement the {@link IMessageProcessingEventHandler} interface. The
 * P-Mode is used to configure the handlers as events may be only of interest for certain messages and applications.
 * <p>This interface is very generic and only defines the relation to the <i>message unit</i> to which the event applies
 * and some general meta-data like an identifier, timestamp and optional short description of the event. There is no
 * method defined to get the type of event as each type of event should have its own implementing class or extension of
 * this interface.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IMessageProcessingEventHandler
 * @since 2.1.0
 */
public interface IMessageProcessingEvent {

    /**
     * Gets the <b>unique</b> identifier of this event. It is RECOMMENDED that this identifier is a valid XML ID so it
     * can be easily included in an XML representation of the event.
     *
     * @return  A {@link String} containing the unique identifier of this event
     */
    public String getId();

    /**
     * Gets the timestamp when the event occurred.
     *
     * @return A {@link Date} representing the date and time the event occurred
     */
    public Date getTimestamp();

    /**
     * Gets an <b>optional</b> short description of what happened. It is RECOMMENDED to limit the length of the
     * description to 100 characters.
     *
     * @return  A {@link String} with a short description of the event if available, <code>null</code> otherwise
     */
    public String getMessage();

    /**
     * Gets the message unit that the event applies to.
     * <p>NOTE: An event can only relate to one message unit. If an event occurs that applies to multiple message units
     * the <i>event source component</i> must create multiple <code>IMessageProcessingEvent</code> objects for each
     * message unit.
     *
     * @return The {@link IMessageUnit} this event applies to.
     */
    public IMessageUnit getSubject();
}
