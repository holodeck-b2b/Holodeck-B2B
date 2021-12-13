/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

import java.util.Map;

import org.holodeckb2b.common.workers.AbstractWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Simple task used in test of the Worker pool
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestTask extends AbstractWorkerTask {

	TaskReporter 	reporter;
    
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
    	reporter = (TaskReporter) parameters.get("reporter");
    	reporter.reportParams(this.name, parameters);
    	
    	if (parameters.containsKey("REJECT"))
    		throw new TaskConfigurationException("REJECTED on request");
    }

    @Override
    public void doProcessing() {        
        reporter.reportRun(this.name);
    }

}
