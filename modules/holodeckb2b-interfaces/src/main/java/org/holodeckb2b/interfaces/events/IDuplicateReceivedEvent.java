/*
 * Copyright (C) 2017 The Holodeck B2B Team.
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

import org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;

/**
 * Is the <i>message processing event</i> that indicates that Holodeck B2B determined that the received <i>User Message
 * </i> is a duplicate and was already processed successfully. Duplicate detection and elimination is part of the <i>AS4
 * Reception Awareness Feature</i> and therefore this event will only be triggered when the feature is enabled in the
 * P-Mode of the message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see IReceptionAwareness
 */
public interface IDuplicateReceivedEvent extends IMessageProcessingEvent {

    /**
     * Indicates whether this duplicate instance was eliminated, i.e. not delivered to the <i>Consumer</i> business
     * application.
     *
     * @return <code>true</code> when the duplicate is eliminated,<br><code>false</code> otherwise
     */
    boolean isEliminated();
}
