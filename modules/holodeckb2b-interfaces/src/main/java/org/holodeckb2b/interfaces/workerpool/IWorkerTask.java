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

/**
 * Is an interface defining a task to be executed by a worker in a WorkerPool.
 * <p>Tasks implement the functionality that should be executed by a worker. They do not need to manage the execution
 * itself. This is done by the WorkerPool. By separating thread management and task functionality the worker tasks only
 * needs to focus on the task on hand.
 * <p><b>NOTES:</b><ul>
 * <li>Implementations MUST handle interrupts correctly and SHOULD stop processing as soon as possible when an interrupt
 *     is received.</li>
 * <li>Implementations SHOULD be thread safe as the task can be run in parallel!</li>
 * <li>When the task should executed more than once implementations SHOULD prevent unhandled exceptions to occur in the
 *     {@link #run()} method as these will stop the thread and prevent repeated executing!</li>
 * </ul>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IWorkerTask extends Runnable {

    /**
     * Sets the name of this worker task
     *
     * @param name  The name for this task
     */
    public void setName(String name);

    /**
     * Gets the name of this worker task
     *
     * @return  The name for this task
     */
    public String getName();

    /**
     * Set the parameters for correctly executing the workers task.
     *
     * @param  parameters    The parameters to configure this worker
     * @throws TaskConfigurationException   When the task can not be configured correctly based on the supplied parameters
     */
    public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException;
}
