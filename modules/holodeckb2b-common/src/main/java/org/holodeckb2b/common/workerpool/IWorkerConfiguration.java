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

import java.util.Map;
import org.holodeckb2b.common.util.Interval;

/**
 * Defines the configuration of a worker in the worker pool. Consists of the task the worker should execute and the
 * parameters needed for successful execution and parameters for scheduling of the task. 
 * 
 * @author Sander Fieten <sander@holodeck-b2b.org>
 * @see WorkerPool
 * @see IWorkerTask
 */
public interface IWorkerConfiguration {
   
    /**
     * @return The name of this worker
     */
    public String getName();
    
    /**
     * Gets the task to be executed by the worker.
     * 
     * @return  Class name of the task this worker should execute. The returned class must implement {@link IWorkerTask}
     */
    public String getWorkerTask();
    
    /**
     * Gets the parameters needed for the successful execution of the workers task. These parameter are only used for
     * configuration of the functionality the task should provide. They are not used for the scheduling configuration
     * by the worker pool.
     *
     * @return      The task parameters as a list of name value pairs 
     */
    public Map<String, ?> getTaskParameters();    
    
    /**
     * Should this worker be activated?
     * 
     * @return <code>true</code> when the worker should be started, <code>false</code> if it should not.
     */
    public boolean activate();
    
    /**
     * @return The number of concurrent executions
     */
    public int getConcurrentExecutions();
        
    /**
     * Gets the time interval between repeated executions of the worker
     * <p>When the task only needs to run once (or runs continuously from start) no interval should be specified.
     * 
     * @return  The interval between repeated executions or <code>null</code> when the worker should only be executed
     *          once.
     */
    public Interval getInterval();
}
