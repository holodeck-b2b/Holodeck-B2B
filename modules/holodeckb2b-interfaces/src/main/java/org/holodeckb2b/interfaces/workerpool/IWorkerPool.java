/**
 * Copyright (C) 2021 The Holodeck B2B Team, Sander Fieten
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

import java.util.List;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Defines the interface of a worker pool. Holodeck B2B uses worker pools for running daemon like threads that manage 
 * messaging (re-)sending, pulling, etc. The functionality of these workers is provided by worker tasks. Management of
 * the worker pool is done by the Holodeck B2B Core. 
 * <p>Worker pools can be created by calling one of the {@link HolodeckB2BCoreInterface#createWorkerPool()} methods 
 * depending on whether the configuration of the pool should be automatically refreshed.
 *
 * @author Sander Fieten
 * @see IWorkerConfiguration
 * @since 5.1.0
 */
public interface IWorkerPool {

    /**
     * @return The name of the worker pool
     */
    String getName();
 
    /**
     * Gets the current list of worker configurations representing the currently active workers in the pool.
     *   
     * @return  configurations of currently active workers in this pool
     * @throws WorkerPoolException when the worker pool is (being) stopped
     */
    List<IWorkerConfiguration>   getCurrentWorkers() throws WorkerPoolException;
    
    /**
     * Gets the interval at which the configuration of this pool is refreshed.
     * 
     * @return	number of seconds at which the configurator is asked for an updated configuration, or</br>
     * 			-1 when auto refreshing of the configuration is disabled
     */
    int getConfigurationRefreshInterval();
        
    /**
     * @return <code>true</code> when this pool is running, <code>false</code> if it is (being) stopped 
     */
    boolean isRunning();
    
    /**
     * Shuts down this worker pool. Implementations may return immediately and perform the shutdown operation in the 
     * back ground.
     * 
     * @param shutdownTime	number of seconds that the pool should wait for tasks to finish their work. If set to 0
     * 						the pool will try to stop running tasks immediately
     */
    void shutdown(int shutdownTime); 
}
