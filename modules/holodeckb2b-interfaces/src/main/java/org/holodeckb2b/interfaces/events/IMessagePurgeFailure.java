/*
 * Copyright (C) 2024 The Holodeck B2B Team.
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

import java.util.Collection;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the <i>message processing event</i> that indicates that a problem occurred when deleting the message unit from the 
 * Holodeck B2B Core storage.  
 * <p>This event is triggered by the Holodeck B2B Core's <i>Storage Manager</i> when either the meta-data or, 
 * for User Messages, the payload content could not be removed from storage. When this event is raised, the state of the 
 * message unit is undefined and it may not be possible to process it further.    
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public interface IMessagePurgeFailure extends IMessageProcessingEvent {

	/**
	 * Gets a list of exceptions that caused the removal of the message unit to fail.
	 * 
	 * @return	one or more {@link StorageException}s indicating the errors that occurred 
	 */
	Collection<StorageException> getFailures();
}
