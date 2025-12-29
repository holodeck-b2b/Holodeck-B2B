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
package org.holodeckb2b.interfaces.storage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines the interface of the object that is used by the Holodeck B2B Core to store the payload meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public interface IPayloadContent {

	/**
	 * Gets the <i>payloadId</i> that uniquely identifies this payload and which is used to link the payload's meta-data
	 * managed by the {@link IMetadataStorageProvider} and its content as managed by the {@link IPayloadStorageProvider}.
	 *
	 * @return	<i>payloadId</i> that identifies the oayload
	 */
	String	getPayloadId();

	/**
	 * Indicates whether the payload data is available.
	 *
	 * @return	<code>true</code> when the payload data is available,<br><code>false</code> otherwise
	 * @since 8.0.0
	 */
	boolean isContentAvailable();

	/**
	 * Reads the payload content from storage.
	 * <p>
	 * NOTE: The caller of this method MUST ensure that the returned stream is closed when it has completed processing
	 * the payload data.
	 *
	 * @return an {@link InputStream} to read the payload content or <code>null</code> when no content is available
	 * @throws StorageException	when an error occurs accessing the stored payload content
	 */
	 InputStream getContent() throws StorageException;

	/**
	 * Opens the payload storage for writing the content to it.
	 * <p>
	 * NOTE: The caller of this method MUST ensure that the returned stream is closed when it has completed writing
	 * the payload data.
	 *
	 * @return	an {@link OutputStream} to write the payload content to storage.
	 * @throws StorageException when an error occurs opening the stream to the payload storage or if the payload content
	 * 							has already been written to storage, i.e. the output stream returned by a previous call
	 * 							to this method has been closed.
	 */
	OutputStream openStorage() throws StorageException;
}
