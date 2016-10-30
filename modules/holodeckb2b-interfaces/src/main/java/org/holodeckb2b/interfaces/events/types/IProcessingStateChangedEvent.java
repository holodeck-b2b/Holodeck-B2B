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
package org.holodeckb2b.interfaces.events.types;

import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;

/**
 * Is the <i>message processing event</i> that indicates that the <i>processing state</i> of a message unit has changed.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
public interface IProcessingStateChangedEvent extends IMessageProcessingEvent {

    /**
     * Gets the processing state that applied to the message unit before the change.
     *
     * @return  {@link IMessageUnitProcessingState} representing the old processing state of the message unit
     */
    IMessageUnitProcessingState getOldState();

    /**
     * Gets the processing state that applies to the message unit after the change.
     * <p><b>NOTE: </b>Because state changes can occur quickly after each other it is not guaranteed that the new state
     * reported in this event is also the current state.
     *
     * @return  {@link IMessageUnitProcessingState} representing the new processing state of the message unit
     */
    IMessageUnitProcessingState getNewState();
}
