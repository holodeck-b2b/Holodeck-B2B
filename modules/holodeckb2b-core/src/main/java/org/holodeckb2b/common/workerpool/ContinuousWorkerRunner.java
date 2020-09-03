/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.interfaces.workerpool.IWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is a "decorator" of an {@link IWorkerTask} instance used by the {@link WorkerPool} implementation to allow task to be
 * run continuously.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
class ContinuousWorkerRunner implements IWorkerTask {
    /**
     * Logging facility, will create a log named like the class name
     */
    private Log             log;

    /**
     * The "real" worker task to be performed
     */
    private IWorkerTask     actualTask;

    /**
     * Creates a new instance for run the given worker task continuously.
     *
     * @param actualTask The actual worker task to be performed
     */
    ContinuousWorkerRunner(IWorkerTask actualTask) {
        this.actualTask = actualTask;
        this.log = LogFactory.getLog(actualTask.getClass().getName());
    }

    @Override
    public void setName(String name) {
        actualTask.setName(name);
    }

    @Override
    public String getName() {
        return actualTask.getName();
    }

    @Override
    public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {
        actualTask.setParameters(parameters);
    }

    @Override
    public void run() {
        try {
            do {
                actualTask.run();
            } while (!Thread.currentThread().isInterrupted());
        } catch (Throwable t) {
            // Uncaught exceptions will stop the execution of the task. This is probably not intended, so log an error
            log.error("Task is STOPPED due to uncaught exception! Details: " + t.getMessage());
        }
    }
}
