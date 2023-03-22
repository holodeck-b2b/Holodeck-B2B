/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.workerpool;

import java.time.Instant;
import java.util.List;

/**
 * Defines the interface for the configuration of a worker pool. Holodeck B2B uses worker pools to manager daemon like
 * tasks that for example manage messaging (re-)sending, clean-up etc. The functionality of these workers is provided
 * by worker tasks.
 * <p>The configuration of a worker pool consists of the workers managed by it and optionally an interval at which the
 * configuration should be refreshed. Each worker has its own configuration specifying which task must be executed and
 * how it should be scheduled.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IWorkerTask
 * @see IWorkerConfiguration
 */
public interface IWorkerPoolConfiguration {

    /**
     * @return The name of the worker pool
	 * @deprecated As the management of worker pools has changed the name is now set programmatically
     */
	@Deprecated
    String getName();

	/**
	 * Gets the worker pool configuration to use.
	 *
	 * @return List of {@link IWorkerConfiguration}s to use for the configuration of the pool.
	 */
	List<IWorkerConfiguration>   getWorkers();

	 /**
     * Gets the interval at which the configuration of the pool should be refreshed.
     *
     * @return	number of seconds at which this configurator is asked for an updated configuration, or<br>
     * 			-1 when auto refreshing of the configuration should be disabled
     * @since 5.1.0
     */
    default int getConfigurationRefreshInterval() {
    	return -1;
    }

	/**
	 * Indicates whether the configuration has changed since the given time stamp. This is used to optimise the
	 * reloading of the configuration by the worker pool. Support for this functionality is optionally. By default
	 * <code>true</code> is returned so the pool will ask for the latest configuration.
	 *
	 * @param  since  time stamp to check against
	 * @return <code>true</code> (default) if the configuration has changed and the worker pool should reload it,<br>
	 * 		   <code>false</code> otherwise
	 * @since 5.1.0
	 */
	default boolean hasConfigChanged(Instant since) {
		return false;
	}

	/**
	 * Reloads the configuration.
	 *
	 * @throws WorkerPoolException  when the configuration cannot be reloaded
	 */
	default void reload() throws WorkerPoolException {}
}
