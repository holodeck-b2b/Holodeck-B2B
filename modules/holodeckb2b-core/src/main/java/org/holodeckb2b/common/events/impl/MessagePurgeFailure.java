/**
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.common.events.impl;

import org.holodeckb2b.interfaces.events.IMessagePurgeFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.storage.StorageException;

/**
 * Is the implementation class of {@link IMessagePurgeFailure} to indicate that a message unit could not be to deleted
 * from the Holodeck B2B Core storage because one ore more errors occurred during the removal process.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class MessagePurgeFailure extends AbstractMessageProcessingFailureEvent<StorageException>
																					implements IMessagePurgeFailure {

	public MessagePurgeFailure(IMessageUnit subject, StorageException reason) {
		super(subject, reason);
	}
}
