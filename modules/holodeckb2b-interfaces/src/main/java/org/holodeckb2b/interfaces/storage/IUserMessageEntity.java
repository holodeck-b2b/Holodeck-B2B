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
package org.holodeckb2b.interfaces.storage;

import java.util.Collection;

import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Defines the interface of the stored object that is used by the Holodeck B2B to store the User Message specific
 * message unit meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @see   IMessageUnitEntity
 */
public interface IUserMessageEntity extends IMessageUnitEntity, IUserMessage {

	/**
	 * Gets meta-data of the payloads contained in the User Message as a collection of {@link IPayloadEntity} objects.
	 * <p>
	 * As the Holodeck B2B Core will not modify the collection itself, the returned collection may be made unmodifiable.
	 * However the contained {@link IPayloadEntity} objects may be updated by the Core using their respective update
	 * methods. Once updated the Core will use {@link IUpdateManager#updatePayload(IPayloadEntity)} to save the updates.
	 *
	 * @return 	collection of {@link IPayloadEntity} objects representing the payloads contained in this User Message
	 * @since 7.0.0
	 */
	@Override
	Collection<? extends IPayloadEntity> getPayloads();
}
