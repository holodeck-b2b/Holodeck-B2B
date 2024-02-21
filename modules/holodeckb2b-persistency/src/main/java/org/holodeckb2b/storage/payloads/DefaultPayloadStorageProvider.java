/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.storage.payloads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.axis2.description.Parameter;
import org.apache.logging.log4j.LogManager;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the default implementation of the Holodeck B2B <i>Payload Storage Provider</i>. It stores the payload content on
 * the file system. For enterprise gateways that have additional requirements, for example that payload content is
 * encrypted, a different provider should be used.
 * <p>
 * By default the payloads will be stored in the <code>pldata</code> subdirectory of the Holodeck B2B <i>temp</i>
 * directory as specified by {@link IConfiguration#getTempDirectory()}. The directory can be changed by setting the
 * <i>payload-directory</i> parameter in the Holodeck B2B configuration, i.e. in the <code>holodeckb2b.xml</code> file.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public class DefaultPayloadStorageProvider implements IPayloadStorageProvider {
	/**
	 * The directory where the paylaod data is to be stored.
	 */
	private Path	directory;

	@Override
	public String getName() {
		return "HB2B Default Payload Storage Provider/" + VersionInfo.fullVersion;
	}

	@Override
	public void init(IConfiguration config) throws StorageException {
		Parameter dirSetting = config.getParameter("payload-directory");
		if (dirSetting != null && dirSetting.getParameterType() == Parameter.TEXT_PARAMETER)
			directory = Path.of((String) dirSetting.getValue());
		else {
			directory = HolodeckB2BCoreInterface.getConfiguration().getTempDirectory().resolve("pldata");
			if (!Files.exists(directory))
				try {
					Files.createDirectory(directory);
				} catch (IOException ioError) {
					throw new StorageException("Could not create payload directory", ioError);
				}
		}
		if (!FileUtils.isWriteableDirectory(directory))
			throw new StorageException(directory.toString() + " is not a valid directory");

		LogManager.getLogger().info("Base directory for storing payloads = " + directory.toString());
	}

	@Override
	public void shutdown() {
	}

	@Override
	public IPayloadContent createNewPayloadStorage(String payloadId, IPMode pmode, Direction direction)
			throws StorageException {
		return new PayloadContent(payloadId, directory.resolve(payloadId).toFile());
	}

	@Override
	public IPayloadContent getPayloadContent(String payloadId) throws StorageException {
		return new PayloadContent(payloadId, directory.resolve(payloadId).toFile());
	}

	@Override
	public void removePayloadContent(String payloadId) throws StorageException {
		try {
			Files.deleteIfExists(directory.resolve(payloadId));
		} catch (IOException ioError) {
			throw new StorageException("Could not delete content", ioError);
		}
	}
}
