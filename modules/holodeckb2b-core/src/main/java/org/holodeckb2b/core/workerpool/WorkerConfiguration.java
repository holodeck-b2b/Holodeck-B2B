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
package org.holodeckb2b.core.workerpool;

import java.util.Collections;
import java.util.Map;

import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;

/**
 * Represents the current configuration of a worker in a worker pool.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.1.0
 */
class WorkerConfiguration implements IWorkerConfiguration {
	
	private final String name;
	private final String taskClassName;
	private Map<String, ?> parameters;
	private int   delay;
	private int   concurrent;
	private Interval interval;

	WorkerConfiguration(final String name, final String taskClassName, final Map<String, ?> parameters, final int delay, 
			            final int concurrent, final Interval interval) {
		this.name = name;
		this.taskClassName = taskClassName;
		this.parameters = Collections.unmodifiableMap(parameters);
		this.delay = delay;
		this.concurrent = concurrent;
		this.interval = interval;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getWorkerTask() {
		return taskClassName;
	}

	@Override
	public Map<String, ?> getTaskParameters() {
		return parameters;
	}

	public void setParameters(final Map<String, ?> parameters) {
		this.parameters = Collections.unmodifiableMap(parameters);
	}
	
	@Override
	public boolean activate() {
		// a worker that is running in a pool is always active :-)
		return true;
	}

	@Override
	public int getDelay() {
		return delay;
	}

	@Override
	public int getConcurrentExecutions() {
		return concurrent;
	}

	@Override
	public Interval getInterval() {
		return interval;
	}

}
