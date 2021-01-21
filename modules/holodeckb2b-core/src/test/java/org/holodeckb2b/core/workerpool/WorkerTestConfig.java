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

import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;

class WorkerTestConfig implements IWorkerConfiguration {
	String name;
	String taskClass = TestTask.class.getName();
	Map<String, Object> parameters = new HashMap<>();
	boolean activate = true;
	int delay = 0;
	int concurrent = 1;
	Interval interval;
	
	public WorkerTestConfig(String name, TaskReporter reporter, Interval interval) {
		this.name = name;
		this.interval = interval;
		parameters.put("reporter", reporter);
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getWorkerTask() {
		return taskClass;
	}

	@Override
	public Map<String, ?> getTaskParameters() {
		return parameters;
	}

	@Override
	public boolean activate() {
		return activate;
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
