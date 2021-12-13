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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;

class WorkerPoolTestConfiguration implements IWorkerPoolConfiguration {

	List<IWorkerConfiguration> configs = new ArrayList<>();
	int refreshInterval = -1;
	boolean hasChanged = true;
	
	int reloaded = 0;
	
	public WorkerPoolTestConfiguration() {
		this(-1);
	}
	
	public WorkerPoolTestConfiguration(int refresh) {
		refreshInterval = refresh;
	}
	
	@Override
	public List<IWorkerConfiguration> getWorkers() {
		return configs;
	}
	
	public int getConfigurationRefreshInterval() {
		return refreshInterval;
	}
	
	private Instant lastCall = null;
	
	@Override
	public boolean hasConfigChanged(Instant since) {
		if (lastCall != null && since.isBefore(lastCall))
			throw new IllegalArgumentException("Called with older since value than last call");
		else
			lastCall = Instant.now();
		
		return hasChanged;
	}		

	public void reload() {
		reloaded++;
	}

	@Override
	public String getName() {
		return null;
	}


}
