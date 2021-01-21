/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.workerpool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;
import org.simpleframework.xml.Root;

/**
 * Is a {@link IWorkerPoolConfiguration} implementation that uses an XML file to specify the configuration of the pool. 
 * The XML structure is defined by XML schema <i>http://holodeck-b2b.org/schemas/2012/12/workers</i>. 
 *
 * @see XMLWorkerConfig
 * @see XMLWorkerPoolConfigurator
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.1.0 added methods related to automatic configuration refresh 
 */
@Root(name = "workers", strict=false)
public class XMLWorkerPoolConfiguration implements IWorkerPoolConfiguration {
	private final Logger log = LogManager.getLogger();
	
	/**
	 * Path to the configuration file 
	 */
	private Path	path; 
	/**
	 * Last read configuration
	 */
	private XMLWorkerPoolCfgDocument lastRead;
	
	/**
	 * Creates a new instance using the configuration file located at the given path. 
	 * 
	 * @param p	path to the configuration file. If this is a relative path it will be resolved against the HB2B home
	 * 			directory
	 * @throws WorkerPoolException 	when the configuration file could not be read from the specified path, for example
	 * 								because it doesn't exist.
	 */
	public XMLWorkerPoolConfiguration(final Path p) throws WorkerPoolException {
		this.path = p.isAbsolute() ? p : HolodeckB2BCore.getConfiguration().getHolodeckB2BHome().resolve(p);
		
		if (!Files.isReadable(path)) {
			log.error("Specified configuration file ({}) does not exist or is not readible", path.toString());
			throw new WorkerPoolException("Configuration file not available");
		}
		lastRead = XMLWorkerPoolCfgDocument.readFromFile(path);
	}

	@Override
	@Deprecated
	public String getName() {
		return null;
	}

	
	@Override
	public List<IWorkerConfiguration> getWorkers() {
		return lastRead.getWorkers();
	}
	
	@Override
	public int getConfigurationRefreshInterval() {
		Integer refreshInterval = lastRead.getRefreshInterval();
		return refreshInterval != null ? refreshInterval.intValue() : -1;
	}	
	
	@Override
	public boolean hasConfigChanged(Instant since) {
		try {
			return Files.getLastModifiedTime(path).compareTo(FileTime.from(since)) > 0;
		} catch (IOException e) {
			log.error("An error occurred while checking status of config file {}. Error was: {}", path.toString(), 
						e.getClass().getSimpleName());
			// Maybe the error is just a glitch and the configuration can still be read, so return true so there will
			// be another read attempt
			return true;
		}
	}
	
	@Override
	public void reload() throws WorkerPoolException {
		lastRead = XMLWorkerPoolCfgDocument.readFromFile(path); 
	}
}
