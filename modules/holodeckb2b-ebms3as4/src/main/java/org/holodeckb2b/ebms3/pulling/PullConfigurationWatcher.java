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
package org.holodeckb2b.ebms3.pulling;

import java.io.File;
import java.util.Map;

import org.holodeckb2b.common.workerpool.WorkerPool;
import org.holodeckb2b.common.workers.AbstractFileWatcher;
import org.holodeckb2b.ebms3.module.EbMS3Module;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is responsible for the configuration of the default <i>pull worker pool</i>. The workers in this pool submit <i>Pull 
 * Request</i> signals to the Holodeck B2B. How often and for which P-Modes this must be done is configurable by the 
 * user in an XML configuration file defined by XML Schema <i>http://holodeck-b2b.org/schemas/2014/05/pullconfiguration
 * </i> (see <code>pullingconfig.xsd</code>). This worker detects changes to this configuration file and applies them to 
 * the pull worker pool.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see WorkerPool
 * @see PullConfiguration
 */
public class PullConfigurationWatcher extends AbstractFileWatcher {
	/**
	 * The ebMS3 Module managing the pull worker pool
	 */
	private EbMS3Module		ebms3Module;
	
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
    	super.setParameters(parameters);
    	
    	// Make sure that the ebMS3/AS4 module is loaded
    	try {
    		ebms3Module = (EbMS3Module) HolodeckB2BCoreInterface.getConfiguration().getAxisConfigurationContext()
	    											   			.getAxisConfiguration()
	    											   			.getModule(EbMS3Module.HOLODECKB2B_EBMS3_MODULE)
	    											   			.getModule();
    	} catch (Exception noEbMS3Module) {
    		log.fatal("The required ebMS3/AS4 module is not loaded!");
    		throw new TaskConfigurationException("Missing required ebMS3/AS4 module");
    	}    
    }
	
    /**
     * Process a change of the file containing the pull configuration.
     * <p>The change can also be that the configuration file was created or deleted. In the last case the worker pool
     * is stopped.
     *
     * @param f         The changed configuration file
     * @param event     Indication what kind of change happened (added, changed or removed)
     */
    @Override
    protected void onChange(final File f, final Event event) {

        log.debug("The pull configuration file changed, reconfiguring");
        if (event == Event.REMOVED) {
            log.debug("Configuration file is removed, stop worker pool");
            try {
                ebms3Module.setPullWorkerPoolConfiguration(null);
                log.warn("Pull workers stopped due to removal of configuration!");
            } catch (final TaskConfigurationException ex) {
                log.error("An error occurred when stopping the Pull Worker Pool. Details: " + ex.getMessage());
            }
        } else {
            log.trace("Configuration file changed, read new configuration from file");
            final PullConfiguration poolCfg = PullConfiguration.loadFromFile(f.getAbsolutePath());
            if (poolCfg != null) {
                log.trace("Read new configuration, reconfigure the worker pool");
                try {
                    ebms3Module.setPullWorkerPoolConfiguration(poolCfg);
                    log.info("Pull configuration succesfully changed");
                } catch (final TaskConfigurationException ex) {
                    log.error("An error occurred when reconfiguring the Pull Worker Pool. Details: " + ex.getMessage());
                }
            } else {
                log.error("The changed configuration in " + f.getAbsolutePath() + " could not be read!");
                // Leave the current pool as is
            }
        }
    }
}
