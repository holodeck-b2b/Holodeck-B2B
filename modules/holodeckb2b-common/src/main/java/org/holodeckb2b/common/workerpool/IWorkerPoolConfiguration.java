/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

import java.util.List;

/**
 * Defines the interface for the configuration of a worker pool. Holodeck B2B uses worker pools for running daemon like 
 * threads that manage messaging (re-)sending, pulling, etc. The functionality of these workers is provided by worker 
 * tasks.
 * <p>The configuration of a worker pool consists of a name of the pool and the workers managed by it. Each worker in
 * the pool has its own configuration specifying which task must be executed and how it should be scheduled. 
 * 
 * @author Sander Fieten
 * @see WorkerPool
 * @see IWorkerTask
 * @see IWorkerConfiguration
 */
public interface IWorkerPoolConfiguration {
    
    /**
     * @return The name of the worker pool
     */
    public String getName();
    
    /**
     * @return  The list of worker configurations contained in this pool 
     */
    public List<IWorkerConfiguration>   getWorkers();
}
