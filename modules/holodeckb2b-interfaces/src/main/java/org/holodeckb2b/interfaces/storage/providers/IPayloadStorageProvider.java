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
package org.holodeckb2b.interfaces.storage.providers;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;

/**
 * Defines the interface of a Holodeck B2B <i>Payload Storage Provider</i> that allows the Holodeck B2B Core to persist
 * the payload content of processed message units.
 * <p>
 * There can always be just one <i>Payload Storage Provider</i> active in an Holodeck B2B instance. The implementation
 * to use is loaded using the Java <i>Service Prover Interface</i> mechanism. Therefore the JAR file containing the
 * <i>Meta-data Storage Provider</i> implementation must contain a file in the <code>META-INF/services/</code> directory
 * named <code>org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider</code> that contains the class name
 * of the provider implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public interface IPayloadStorageProvider {

    /**
     * Gets the name of this provider to identify it in logging. This name is only used for logging purposes and it is
     * recommended to include a version number of the implementation. If no name is specified by the implementation the
     * class name will be used.
     *
     * @return  The name of the persistency provider.
     */
    default String getName() { return this.getClass().getName(); }

    /**
     * Initialises the provider. This method is called once at startup of the Holodeck B2B instance. Since the message
     * processing depends on the correct functioning of the provider this method MUST ensure that all required
     * configuration and data is available.
     *
     * @param config	the Holodeck B2B configuration
     * @throws StorageException     When the initialization of the provider can not be completed. The exception
     *                                  message SHOULD include a clear indication of what caused the init failure.
     */
	void init(final IConfiguration config) throws StorageException;

	/**
	 * Shuts down the provider.
	 * <p>This method is called by the Holodeck B2B Core when the instance is shut down. Implementations should use it
	 * to release resources held for storing the message meta-data.
	 */
	void shutdown();

	/**
	 * Creates a new {@link IPayloadContent} object that can be used to store the payload contents. The payload
	 * meta-data, e.g. P-Mode governing processing of the containing User Message, may specify or provide specific
	 * parameters how the payload content must be stored, for example encrypted.
	 *
	 * @param payloadInfo	meta-data of the payload which content are to be stored
	 * @return {@link IPayloadContent} object to use for storing the payload content
	 * @throws StorageException when an error occurs storing the payload content
	 */
	IPayloadContent createNewPayloadStorage(final IPayloadEntity payloadInfo) throws StorageException;

	/**
	 * Retrieves the stored content of a payload.
	 *
	 * @param payloadInfo	meta-data of the payload which content should be retrieved
	 * @return {@link IPayloadContent} object to use for retrieving the payload content, or<br/>
	 * 		   <code>null</code> if there is no content for the given meta-data
	 * @throws StorageException when an error occurs retrieving the payload content
	 */
	IPayloadContent getPayloadContent(final IPayloadEntity payloadInfo) throws StorageException;

	/**
	 * Removes the stored content of a payload.
	 *
	 * @param payloadInfo	meta-data of the payload which content should be removed
	 * @throws StorageException	when an error occurs that prevents the <i>Payload Storage Provider</i> to remove
	 * 							the payload content.
	 */
	void removePayloadContent(final IPayloadEntity payloadInfo) throws StorageException;
}
