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
package org.holodeckb2b.ebms3.module;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.modules.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.workerpool.WorkerPool;
import org.holodeckb2b.ebms3.pulling.PullConfiguration;
import org.holodeckb2b.ebms3.pulling.PullConfigurationWatcher;
import org.holodeckb2b.ebms3.pulling.PullWorker;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Axis2 module class for the Holodeck B2B ebMS3/AS4 module.
 * <p>This class is responsible for the initialization and shutdown of the ebMS module.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class EbMS3Module implements Module {
    /**
     * The name of the Axis2 Module that contains the Holodeck B2B ebMS3 implementation
     */
    public static final String HOLODECKB2B_EBMS3_MODULE = "holodeckb2b-ebms3as4";

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(EbMS3Module.class);

    /**
     * Pool of worker threads that handle the sending of <i>pull request</i>. The workers in this pool are {@link PullWorker}
     * objects. The pool is configured by the {@link PullConfigurationWatcher} that runs in the normal worker pool and
     * checks the pull configuration for changes and applies them to this pull worker pool.
     */
    private WorkerPool pullWorkers = null;

    /**
     * The installed {@link ISecurityProvider} that will handle the WS-Security header in the ebMS3/AS4 messages. 
     */
    private ISecurityProvider secProvider; 
    
    /**
     * Initializes the Holodeck B2B Core module.
     *
     * @param cc
     * @param am
     * @throws AxisFault
     */
    @Override
    public void init(final ConfigurationContext cc, final AxisModule am) throws AxisFault {
        log.info("Starting Holodeck B2B ebMS3/AS4 module...");

        // Check if module name in module.xml is equal to constant use in code
        if (!am.getName().equals(HOLODECKB2B_EBMS3_MODULE)) {
            // Name is not equal! This is a fatal configuration error, stop loading this module and alert operator
            log.fatal("Invalid Holodeck B2B Core module configuration found! Name in configuration is: "
                        + am.getName() + ", expected was: " + HOLODECKB2B_EBMS3_MODULE);
            throw new AxisFault("Invalid configuration found for module: " + am.getName());
        }
        
        log.trace("Load the ebMS3 Security Provider");
    	Iterator<ISecurityProvider> providers = ServiceLoader.load(ISecurityProvider.class).iterator();
    	secProvider = providers.hasNext() ? providers.next() : null;
    	if (providers.hasNext()) 
    		log.warn("Multiple Security Providers are available, using first one found");
        log.debug("Using security provider: " + secProvider.getName());
        try {
             secProvider.init();
        } catch (SecurityProcessingException initializationFailure) {
            log.fatal("Initialisation of required ebMS3 security provider ({}) failed!\n\tError details: {}",
            		  secProvider.getName(), initializationFailure.getMessage());
            throw new AxisFault("Unable to initialize required security provider!");
        }
        log.info("Succesfully loaded " + secProvider.getName() + " as security provider");
        
        log.trace("Create the pull worker pool");
        // The pull worker pool is only created here, initialization is done by the worker part of the normal
        //  worker pool
        pullWorkers = new WorkerPool(PullConfiguration.PULL_WORKER_POOL_NAME);
        
        log.info("Holodeck B2B ebMS3/AS4 module " + VersionInfo.fullVersion + " STARTED.");
    }
    
    /**
     * Sets the configuration of the <i>pull worker pool</i> which contains the <i>Workers</i> that are responsible for
     * sending the Pull Request signal messages.
     * <p>If no new configuration is provided the worker pool will be stopped. NOTE that this will also stop Holodeck
     * B2B from pulling for User Messages (unless some other worker(s) in the regular worker pool take over, which is
     * <b>not recommended</b>).
     *
     * @param pullConfiguration             The new pool configuration to use. If <code>null</code> the worker pool
     *                                      will be stopped.
     * @throws TaskConfigurationException   When the provided configuration could not be activated. This is probably
     *                                      caused by an issue in the configuration of the workers but it can also be
     *                                      that the worker pool itself could not be started correctly.
     */
    public void setPullWorkerPoolConfiguration(IWorkerPoolConfiguration pullConfiguration) 
    																				throws TaskConfigurationException {
        log.trace("New pull worker configuration provided, reconfiguring Pull Worker Pool");
        if (pullConfiguration == null) {
            log.trace("Configuration is removed, stop worker pool");
            pullWorkers.stop(0);
            log.warn("Pull Worker Pool stopped due to removal of configuration!");
        } else {
            log.trace("New configuration provided, reconfigure the worker pool");
            pullWorkers.setConfiguration(pullConfiguration);
            log.info("Pull configuration succesfully changed");
        }
    }
    
    /**
     * Gets the active <i>Security Provider</i> of this Holodeck B2B instance that will create/process the WS-Security
     * header of the ebMS3/AS4 messages. 
     *
     * @return 	The active security provider
     * @since 	5.0.0
     */
    public ISecurityProvider getSecurityProvider() {
        return secProvider;
    }    

    @Override
    public void engageNotify(final AxisDescription ad) throws AxisFault {
    }

    @Override
    public boolean canSupportAssertion(final Assertion asrtn) {
        return false;
    }

    @Override
    public void applyPolicy(final Policy policy, final AxisDescription ad) throws AxisFault {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutdown(final ConfigurationContext cc) throws AxisFault {
        log.info("Shutting down Holodeck B2B ebMS3/AS4 module...");

        // Stop all the oull workers by shutting down worker pool
        log.trace("Stopping pull worker pool");
        pullWorkers.stop(10);
        log.debug("Pull worker pool stopped");

        log.info("Holodeck B2B ebMS3/AS4 module STOPPED.");
    }

   
}
