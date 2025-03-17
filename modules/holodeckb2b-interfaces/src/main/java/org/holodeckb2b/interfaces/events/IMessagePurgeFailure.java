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

import org.holodeckb2b.interfaces.storage.StorageException;

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
public interface IMessagePurgeFailure extends IMessageProcessingFailure {

	/**
	 * Gets a {@linkplain StorageException} that caused the removal of the message unit to fail. When the message to be
	 * removed is a User Message the issue can be that one or more of its payloads could not be removed. {@link
	 * StorageException}s related to the payload removal issues can be retrieved using the
	 * {@link StorageException#getSuppressed()} method.
	 *
	 * @return	one or more {@link StorageException}s indicating the errors that occurred
	 */
	@Override
	StorageException getFailureReason();
}
