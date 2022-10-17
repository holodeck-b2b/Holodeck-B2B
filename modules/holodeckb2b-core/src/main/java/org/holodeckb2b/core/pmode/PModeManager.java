/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.pmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.InvalidPModeException;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

/**
 * Manages the set of deployed P-Mode of a Holodeck B2B installation. It acts as a <i>facade</i> to the actual storage
 * of deployed P-Mode to ensure that a new P-Mode that is being deployed is validated before deployment. For this P-Mode
 * validation another component is used. This ensures that external and internal storage and validation of P-Modes are
 * more loosely coupled and customisable to specific requirements. 
 * <p>The actual storage component to be used can be set in the configuration. The default implementation {@link 
 * InMemoryPModeSet} stores the P-Modes in memory.<br>
 * Which validator should be used for a P-Mode is determined by the P-Mode's type, which is defined by the <b>
 * PMode.MEPBinding</p> parameter. For loading the correct P-Mode validator the Java SPI mechanism is used. This allows
 * to use different validators for different protocols and/or deployments.<br>
 * Whether a P-Mode for which non validator is available should still be loaded or be rejected depends on the 
 * configuration setting {@link InternalConfiguration#acceptNonValidablePMode()}.  
 * <p><b>NOTE: </b>Although in this architecture the internal and external storage is more loosely coupled they still
 * influence each other and changing either implementation may affect the other.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @see InMemoryPModeSet
 * @see org.holodeckb2b.interfaces.pmode.validation
 */
public class PModeManager implements IPModeSet {

    private static final Logger log = LogManager.getLogger(PModeManager.class);

    /**
     * The actual store of deployed P-Modes
     */
    private IPModeSet   deployedPModes;

    /**
     * The P-Mode validators in use to check P-Modes before being deployed
     */
    private List<IPModeValidator> validators;
    
    /**
     * Indicator whether P-Modes for which no validator is available should still be accepted.
     */
    private boolean acceptNonValidable = true;

    /**
     * Initialises a new <code>PModeManager</code> which will use the {@link IPModeSet} implementation as specified in 
     * the configuration for storing the deployed the P-Modes. If not specified the default {@link InMemoryPModeSet} 
     * implementation will be used.
     *
     * @param config   The configuration of this instance 
     * @throws PModeSetException When no P-Mode validators are available (maybe due to Java SPI issue) but validation of
     * 							 P-Modes is required (i.e. non validable P-Modes are configured to be rejected)
     */
    @Override
    public void init(final IConfiguration config) throws PModeSetException {
        log.trace("Load P-Mode storage component");        
        deployedPModes = Utils.getFirstAvailableProvider(IPModeSet.class);
        if (deployedPModes != null) {
            // A specific P-Mode storage implementation is available, try to initialise it
            try {
                log.debug("Initialising P-Mode storage implementation: {}", deployedPModes.getName());
                deployedPModes.init(config);
            } catch (PModeSetException initFailure) {
               // Could not initialise the custom storage implementation
               log.error("Could not initialise the P-Mode storage implementation: {}. Error details: {}",
                          deployedPModes.getName(), initFailure.getMessage());
               throw new PModeSetException("Could not load P-Mode storage implementation!", initFailure);
            }
        } else
            deployedPModes = new InMemoryPModeSet();              

    	log.debug("Load installed P-Mode validators");
    	validators = new ArrayList<>();       
    	ServiceLoader.load(IPModeValidator.class).forEach(v -> validators.add(v));

    	acceptNonValidable = ((InternalConfiguration) config).acceptNonValidablePMode();
        // If no validators were loaded and validation is required we have a problem, otherwise we just log the loaded 
        // validators
        if (!acceptNonValidable && validators.isEmpty()) {
        	log.fatal("No P-Mode validators are available, but validation is required!");
        	throw new PModeSetException("Missing P-Mode validators for required validation");
        } else if (acceptNonValidable)
        	log.warn("To reduce risk of issues during exchanges it's NOT RECOMMENDED to accept non validable P-Modes!");        
        
        // Log configuration
        if (log.isInfoEnabled()) {
	        StringBuilder   logMsg = new StringBuilder("Initialized P-Mode manager:\n")
	           .append("\tP-Mode storage implentation : ").append(deployedPModes.getClass().getName()).append('\n')
	           .append("\tAccept/reject non validable : ").append(acceptNonValidable ? "Accept" : "Reject").append('\n')
	           .append("\tRegistered validators:\n");
	        if (validators != null)
	        	for(IPModeValidator v : validators) 
	        		logMsg.append("\t\t").append(v.getName()).append('\n');
    		else
    			logMsg.append("none\n");	        
	        log.info(logMsg.toString());
        }
    }

    @Override
    public void shutdown() {
    	deployedPModes.shutdown();
    }
    
    @Override
    public IPMode get(String id) {
        return deployedPModes.get(id);
    }

    @Override
    public Collection<IPMode> getAll() {
        return deployedPModes.getAll();
    }

    @Override
    public boolean containsId(String id) {
        return deployedPModes.containsId(id);
    }

    @Override
    public String add(IPMode pmode) throws PModeSetException {
        log.trace("Request to add P-Mode");
        validatePMode(pmode);
        try {
        	log.trace("Adding to deployed set of P-Modes");
        	String pmodeId = deployedPModes.add(pmode);
           	log.info("Successfully deployed P-Mode [{}]", pmodeId);
           	return pmodeId;
        } catch (PModeSetException deploymentException) {
            log.error("Could not deploy new P-Mode due to exception in storage implementation! Error message: {}",
                       deploymentException.getMessage());
            throw deploymentException;
        }        
    }

    @Override
    public void replace(IPMode pmode) throws PModeSetException {
        log.trace("Request to replace P-Mode [{}]", pmode.getId());
        validatePMode(pmode);
        try {
        	log.trace("Replacing current version in the deployed set of P-Modes");
        	deployedPModes.replace(pmode);
        	log.info("Successfully deployed change version of P-Mode [{}]", pmode.getId());
        } catch (PModeSetException deploymentException) {
        	log.error("Could not replace P-Mode due to exception in storage implementation! Error message: {}",
                       deploymentException.getMessage());
        	throw deploymentException;
        }
    }

    /**
     * Validates the given using <b>all</b> available {@link IPModeValidator}s that can handle the P-Mode, i.e. does 
     * support the <b>PMode.MEPBinding</b>. If no validator is available it depends on the configuration whether the
     * P-Mode is accepted or rejected.   
     * 
     * @param pmode		P-Mode to validate
     * @throws InvalidPModeException When the P-Mode is invalid
     * @throws PModeSetException  	 When no validator can handle the P-Mode but validation is required
     */
    private void validatePMode(final IPMode pmode) throws PModeSetException {
        final String mepBinding = pmode.getMepBinding();
        log.trace("Getting validator for P-Mode ({}) based on MEPBinding: {}", pmode.getId(), mepBinding);
        List<IPModeValidator> applicableV = validators.parallelStream()
        											  .filter(v -> v.doesValidate(mepBinding))
        											  .collect(Collectors.toList());                
        if (!applicableV.isEmpty()) {
        	Collection<PModeValidationError> validationErrors = new ArrayList<>();
        	for(IPModeValidator v : applicableV) {
        		log.trace("Using validator ({}) for validation of P-Mode (id={})", v.getName(), pmode.getId());
        		Collection<PModeValidationError> errors = v.validatePMode(pmode);
        		if (!Utils.isNullOrEmpty(errors)) {
        			log.debug("Validator ({}) found {} errors in the new P-Mode", v.getName(), errors.size());
        			validationErrors.addAll(errors);        		
        		}        		        		
	        } 
        	if (!validationErrors.isEmpty()) {
        		log.warn("Found {} errors in new P-Mode ({})!", validationErrors.size(), pmode.getId());
        		throw new InvalidPModeException(validationErrors);
        	} else 
	            log.debug("No errors found in new P-Mode ({})", pmode.getId());	         
        } else {
        	// depending on configuration either reject or accept 
        	if (acceptNonValidable) {
        		log.warn("No validator available for P-Mode ({})", pmode.getId());        		
        	} else {
        		log.error("No validator available for P-Mode ({})", pmode.getId());
        		throw new PModeSetException("No validator available for P-Mode");
        	}
        }	    	
    }
    
    @Override
    public void remove(String id) throws PModeSetException {
    	log.trace("Request to remove P-Mode [{}]", id);
    	deployedPModes.remove(id);
    	log.trace("P-Mode [{}] removed", id);    	
    }

    @Override
    public void removeAll() throws PModeSetException {
    	log.trace("Request to remove all P-Modes");
        deployedPModes.removeAll();
        log.trace("Removed all P-Modes");        
    }
}
