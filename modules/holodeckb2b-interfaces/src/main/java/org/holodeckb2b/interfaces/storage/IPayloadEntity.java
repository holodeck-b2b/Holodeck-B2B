/*
 * Copyright (C) 2023 The Holodeck B2B Team.
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

import java.io.IOException;
import java.io.InputStream;

import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Defines the interface of the object that is used by the Holodeck B2B Core to store the payload meta-data. Note that
 * this interface extends {@link IPayload} and therefore also has a {@link #getContent()} method. Because the
 * <i>Metadata Storage Provider</i> doesn't handle the content it doesn't need to implement this method and providing
 * access to the payload's content is managed by the Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public interface IPayloadEntity extends IPayload {

	/**
	 * Gets the <i>payloadId</i> that uniquely identifies this payload and which is used to link the payload's meta-data
	 * managed by the {@link IMetadataStorageProvider} and its content as managed by the {@link IPayloadStorageProvider}.
	 *
	 * @return	<i>payloadId</i> that identifies the oayload
	 */
	String	getPayloadId();

	/**
	 * Gets the <i>CoreId</i> of the User Message that this payload is contained in.
	 *
	 * @return	the <i>CoreId</i> of the User Message that this payload is contained in, or <code>null</code> if the
	 * 			paylaod is not yet assigned to a User Message
	 */
	String getParentCoreId();

	/**
	 * Gets the identifier of the P-Mode that governs the message exchange of the User Message unit the payload is or
	 * will be contained in. When the payload is linked to a User Message, i.e. <code>getParentCoreId() != null</code>,
	 * the returned identifier must match the one set in the User Message.
	 *
	 * @return	P-Mode identifier
	 */
	String getPModeId();

	/**
	 * Gets the direction of the payload, i.e. whether it is received or sent. When the payload is linked to a User
	 * Message, i.e. <code>getParentCoreId() != null</code>, the value must match the one set in the User Message.
	 *
	 * @return indicates whether the payload is incoming or outgoing
	 */
	Direction	getDirection();

	/**
	 * Sets the Mime type of the payload.
	 *
	 * @param mt	the Mime type of the payload
	 */
	void setMimeType(String mt);

	/**
	 * Sets the reference to the payload content in the message.
	 * <p>Corresponds to the <code>href</code> attribute of the <code>//eb:UserMessage/eb:IPayloadInfo/eb:PartInfo</code>
	 * element in the ebMS header of the message.
	 *
	 * @param uri 	the URI that references the payload content within the context of the message
	 */
	void setPayloadURI(String uri);

	/**
	 * Adds the given <i>Part Property</i> to the payload meta-data.
	 *
	 * @param p	property to add as part property
	 */
	void addProperty(IProperty p);

	/**
	 * Removes the given property from the set of <i>Part Properties</i> of this payload.
	 *
	 * @param p	property to remove as part property
	 */
	void removeProperty(IProperty p);

	/**
	 * As this method does not need to be implemented by the <i>Metadata Storage Provider</i> a default implementation
	 * is provided.
	 */
	@Override
	default InputStream getContent() throws IOException {
		return null;
	}
}
