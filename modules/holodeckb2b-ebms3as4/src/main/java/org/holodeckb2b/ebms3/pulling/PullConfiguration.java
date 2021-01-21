/**
 * Copyright (C) 2021 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.pulling;

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

/**
 * Is a implementation of {@link IWorkerPoolConfiguration} specifically for managing a pool of <i>pull workers</i> that
 * send out the <i>Pull Requests</i>.
 * <p>The pull worker pool exists of one default pull worker and zero or more specific pull workers. The specific
 * pull workers handle the pulling for a given set of P-Modes. The default pull worker will handle all other pulling.
 * For each worker an interval is specified to wait between sending of the pull request. If the interval equals 0 the
 * pull worker will not be activated and pulling will be disabled.<br/>
 * The configuration also includes a parameter that indicates the interval at which it should be refreshed and the 
 * worker pool should be reconfigured. If this parameter is set to zero the pulling will be statically configured and
 * Holodeck B2B needs to be restarted for configuration changes to take effect.<br/>
 * Pulling can be disabled both permanently or temporarily. To disable pulling permanently just don't provide a pulling 
 * configuration file. To temporarily disable pulling set the interval of the default puller to '0' and have no other 
 * pullers defined. Pulling then can later be enabled by either changing the interval of the default puller or adding 
 * specific pullers.
 * <p>The configuration of the pool is read from an XML document defined by the schema 
 * <i>http://holodeck-b2b.org/schemas/2014/05/pullconfiguration</i>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see PullWorker
 * @see PullerConfigElement
 */
public class PullConfiguration implements IWorkerPoolConfiguration {
	static final Logger log = LogManager.getLogger();
	
	/**
	 * Path to the configuration file 
	 */
	private Path	path; 
	/**
	 * Last read configuration
	 */
	private PullConfigDocument lastRead;

	/**
	 * Creates a new instance using the configuration file located at the given path. 
	 * 
	 * @param p	path to the configuration file. If this is a relative path it will be resolved against the HB2B home
	 * 			directory
	 */
	public PullConfiguration(final Path p) {
		this.path = p.isAbsolute() ? p : HolodeckB2BCore.getConfiguration().getHolodeckB2BHome().resolve(p);		
		lastRead = PullConfigDocument.loadFromFile(path);
	}
	
	/**
	 * @return whether a configuration is available 
	 */
	public boolean isAvailable() {
		return lastRead != null;
	}
	
	/**
	 * @deprecated The puller worker pool is named by the module
	 */
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
		return lastRead.getRefreshInterval();
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
	public void reload() {
		final PullConfigDocument newConfig = PullConfigDocument.loadFromFile(path);
		if (newConfig == null) 
			log.warn("Could not read new configuration from file ({}). Keep using current.", path.toString());
		else
			lastRead = newConfig;
	}	
}
