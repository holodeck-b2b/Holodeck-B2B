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

/**
 * Is thrown by {@link IWorkerTask#setParameters(java.util.Map)} to indicates a problem with the configuration of a task
 * that should executed by a worker in a WorkerPool.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TaskConfigurationException extends Exception {

    /**
     * Creates a new instance of <code>TaskConfigurationException</code> without detail message.
     */
    public TaskConfigurationException() {
    }

    /**
     * Constructs an instance of <code>TaskConfigurationException</code> with the specified detail message.
     *
     * @param msg  The detailed error message.
     */
    public TaskConfigurationException(final String msg) {
        super(msg);
    }
}
