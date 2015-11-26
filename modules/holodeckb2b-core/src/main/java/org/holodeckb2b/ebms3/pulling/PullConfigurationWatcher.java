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
import org.holodeckb2b.common.workerpool.WorkerPool;
import org.holodeckb2b.common.workers.FileWatcher;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is responsible for the configuration of the pull worker pool. The workers in this pull send out the pull request
 * signals. How often en for which P-Modes this must be done is configurable by the user in a configuration file. 
 * This worker detects changes to this configuration file and applies them to the pull worker pool.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see WorkerPool
 * @see PullConfiguration
 */
public class PullConfigurationWatcher extends FileWatcher {

    /**
     * Process a change of the file containing the pull configuration. 
     * <p>The change can also be that the configuration file was created or deleted. In the last case the worker pool
     * is stopped.
     * 
     * @param f         The changed configuration file
     * @param event     Indication what kind of change happened (added, changed or removed)
     */
    @Override
    protected void onChange(File f, Event event) {
        
        log.debug("The pull configuration changed, reconfiguring");        
        if (event == Event.REMOVED) {
            log.debug("Configuration file is removed, stop worker pool");
            HolodeckB2BCore.getPullWorkerPool().stop(0);
            log.warn("Pull workers stopped due to removal of configuration!");
        } else {
            log.debug("Configuration file changed, read new configuration from file");
            PullConfiguration poolCfg = PullConfiguration.loadFromFile(f.getAbsolutePath());
            if (poolCfg != null) {
                log.debug("Read new configuration, reconfigure the worker pool");
                HolodeckB2BCore.getPullWorkerPool().setConfiguration(poolCfg);
                log.info("Pull configuration succesfully changed");
            } else {
                log.error("The changed configuration in " + f.getAbsolutePath() + " could not be read!");
                // Leave the current pool as is
            }
        }
    }
    
}
