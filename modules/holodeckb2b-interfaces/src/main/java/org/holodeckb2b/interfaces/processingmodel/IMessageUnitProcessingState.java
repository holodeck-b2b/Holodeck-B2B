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
package org.holodeckb2b.interfaces.processingmodel;

import java.util.Date;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Represents an actual processing state a message unit is or has been in.
 * <p>This interface only defines which processing state applied to a message unit from a certain point in time. The
 * order of the states and what is the <i>current</i> state is maintained through the list of states in the
 * {@link IMessageUnit} itself.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see    ProcessingState
 * @since  HB2B_NEXT_VERSION
 */
public interface IMessageUnitProcessingState {

    /**
     * Gets the processing state that applied to the message from {@link #getStartTime()}.
     *
     * @return {@link ProcessingState}
     */
    ProcessingState getState();

    /**
     * Returns the moment when the processing state applied to the message unit.
     *
     * @return The {@link Date} the message unit entered this processing state
     */
    Date    getStartTime();
}
