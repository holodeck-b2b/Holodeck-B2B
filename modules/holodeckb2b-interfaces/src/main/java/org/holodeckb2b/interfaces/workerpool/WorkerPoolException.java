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

import java.util.Collections;
import java.util.List;

/**
 * Is the exception used to indicate that there was a problem in the configuration of a {@link IWorkerPool} or one of 
 * the <i>workers</i> contained in it. 
 * <p>When the exception is thrown because of a problem with one or more specific workers within it, the workers that 
 * failed can be retrieved using the {@link #getFailedWorkers()} which returns a list of {@link IWorkerConfiguration}s
 * of the failed workers. When there is a generic error this list will be empty and the error is described by the 
 * exception message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.1.0 
 */
public class WorkerPoolException extends Exception {
	/**
	 * The list of workers which (re-)configuration failed
	 */
	private final List<IWorkerConfiguration>	failedWorkers;
	
    /**
     * Creates a new instance of <code>TaskConfigurationException</code> without detail message.
     */
    public WorkerPoolException() {
    	super();
    	this.failedWorkers = null;
    }

    /**
     * Creates a new instance of <code>TaskConfigurationException</code> without detail message but including the
     * exception that caused the problem in processing.
     * 
     * @param cause The exception that caused the error
     */
    public WorkerPoolException(final Throwable cause) {
    	super(cause);
    	this.failedWorkers = null;
    }
    
    /**
     * Constructs a new instance with the specified detail message. This constructor is used on generic errors during 
     * (re-)configuration of the pool. 
     *
     * @param msg  The detailed error message.
     */
    public WorkerPoolException(final String msg) {
        super(msg);
        this.failedWorkers = null;
    }

    /**
     * Constructs a new instance with the specified detail message and an exception that caused the problem in 
     * processing. This constructor is used on generic errors during (re-)configuration of the pool. 
     *
     * @param msg   The detailed error message.
     * @param cause The exception that caused the error
     */
    public WorkerPoolException(final String msg, final Throwable cause) {
    	super(msg, cause);
    	this.failedWorkers = null;
    }    
    
    /**
     * Constructs a new instance with a list of workers that failed during the (re-)configuration of the pool. 
     *
     * @param failed The workers which could not be configured
     */
    public WorkerPoolException(final List<IWorkerConfiguration> failed) {
    	super();
    	this.failedWorkers = Collections.unmodifiableList(failed);
    }
    
    /**
     * @return list of {@link IWorkerConfiguration}s representing the workers which failed to be configured
     */
    public List<IWorkerConfiguration> getFailedWorkers() {
    	return failedWorkers;
    }
}
