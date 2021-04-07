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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.modules.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.ebms3.pulling.PullConfiguration;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;

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
     * The name of the worker pool that contains the workers that execute the PullRequests
     */
    public static final String PULL_WORKER_POOL_NAME = "hb2b-ebms3-pullers";

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(EbMS3Module.class);

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
    	secProvider = Utils.getFirstAvailableProvider(ISecurityProvider.class);
    	if (secProvider != null) {
	    	log.debug("Using security provider: " + secProvider.getName());
	        try {
	             secProvider.init();
	        } catch (SecurityProcessingException initializationFailure) {
	            log.fatal("Initialisation of required ebMS3 security provider ({}) failed!\n\tError details: {}",
	            		  secProvider.getName(), initializationFailure.getMessage());
	            throw new AxisFault("Unable to initialize required security provider!");
	        }
	        log.info("Succesfully loaded " + secProvider.getName() + " as security provider");        
    	} else {
    		log.fatal("No ebMS3 security provider available!");
    		throw new AxisFault("Missing required security provider!");
    	}
    	
        final Parameter cfgFileParam = HolodeckB2BCore.getConfiguration().getParameter("EbMSPullConfigFile");
        Path cfgFilePath;
        if (cfgFileParam == null)
        	cfgFilePath = HolodeckB2BCore.getConfiguration().getHolodeckB2BHome()
        																.resolve("conf/pulling_configuration.xml");
        else {
        	cfgFilePath = Paths.get((String) cfgFileParam.getValue());
        	if (!cfgFilePath.isAbsolute())
        		cfgFilePath = HolodeckB2BCore.getConfiguration().getHolodeckB2BHome().resolve(cfgFilePath);
        }									 
        log.trace("Check availability of pull configuration file: {}", cfgFilePath.toString());
        PullConfiguration pullConfig = new PullConfiguration(cfgFilePath);
        if (pullConfig.isAvailable()) {
        	log.trace("Create the pull worker pool");
        	try {
				HolodeckB2BCore.createWorkerPool(PULL_WORKER_POOL_NAME, pullConfig);
			} catch (WorkerPoolException poolError) {
				log.error("The pull worker pool could not be started! Error details:\n{}", 
							Utils.getExceptionTrace(poolError, true));
				throw new AxisFault("Unable to start pull worker pool", poolError);
			}        	
        } else {
        	log.warn("Pulling disabled, as no configuration is provided.");
        }
        
        log.info("Holodeck B2B ebMS3/AS4 module " + VersionInfo.fullVersion + " STARTED.");
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
        log.info("Holodeck B2B ebMS3/AS4 module STOPPED.");
    }

   
}
