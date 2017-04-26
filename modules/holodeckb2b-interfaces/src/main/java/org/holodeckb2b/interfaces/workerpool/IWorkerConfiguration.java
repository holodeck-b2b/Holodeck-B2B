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

import java.util.Map;

import org.holodeckb2b.interfaces.general.Interval;

/**
 * Defines the configuration of a worker in the worker pool. Consists of the task the worker should execute and the
 * parameters needed for successful execution and parameters for scheduling of the task.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
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
     * Get the indicator whether this worker should be activated?
     *
     * @return <code>true</code> when the worker should be started, <code>false</code> if it should not.
     */
    public boolean activate();

    /**
     * Gets the delay to wait before starting the worker.
     *
     * @return The delay to wait before starting the worker, in milliseconds
     */
    public int getDelay();

    /**
     * Gets the number of workers that should be run concurrently.
     * <p>NOTE: As there needs to be at least one worker, values less than one will result in one worker being started.
     * If the worker should not be started at all, the configuration must return <i>false</i> from the <code>activate()
     * </code>method.
     *
     * @return The number of concurrent executions.
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
