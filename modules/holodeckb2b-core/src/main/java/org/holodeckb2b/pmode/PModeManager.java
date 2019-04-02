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
package org.holodeckb2b.pmode;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.util.Utils;
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
 * more loosely coupled and customizable to specific requirements. By default the P-Modes are stored in memory and a
 * basic validation is executed before deployment.
 * <p><b>NOTE: </b>Although in this architecture the internal and external storage is more loosely coupled they still
 * influence each other and changing either implementation may affect the other.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @see InternalConfiguration#getPModeStorageImplClass()
 * @see InMemoryPModeSet
 * @see InternalConfiguration#getPModeValidatorImplClass()
 * @see BasicPModeValidator
 */
public class PModeManager implements IPModeSet {

    private static final Logger log = LogManager.getLogger(PModeManager.class);

    /**
     * The actual store of deployed P-Modes
     */
    private IPModeSet   deployedPModes;

    /**
     * The P-Mode validator in use to check P-Modes before being deployed
     */
    private IPModeValidator validator;

    /**
     * Creates a new <code>PModeManager</code> which will use the {@link IPModeSet} and {@link IPModeValidator}
     * implementations as specified in the configuration for storing the deployed respectively checking the P-Modes. 
     * If not specified the default implementation ({@link InMemoryPModeSet} and {@link BasicPModeValidator}) will be 
     * used.
     *
     * @param config   The configuration of this instance 
     */
    public PModeManager(final InternalConfiguration config) {
        log.trace("Start configuration");
        final String pmodeValidatorClass = config.getPModeValidatorImplClass();        
        if (!Utils.isNullOrEmpty(pmodeValidatorClass)) {
            // A specific P-Mode validator is specified in the configuration, try to load it
            try {
                log.debug("Loading P-Mode validator specified in configuration: {}", pmodeValidatorClass);
                validator = (IPModeValidator) Class.forName(pmodeValidatorClass).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
               // Could not create the specified validator, fall back to default implementation
               log.error("Could not load the specified P-Mode validator: {}. Using default implementation instead.",
                          pmodeValidatorClass);
               validator = new BasicPModeValidator();
            }
        } else
            // No specific validator, use default one
            validator = new BasicPModeValidator();

        final String pmodeStorageClass = config.getPModeStorageImplClass();
        if (!Utils.isNullOrEmpty(pmodeStorageClass)) {
            // A specific P-Mode storage implementation is specified in the configuration, try to load it
            try {
                log.debug("Loading P-Mode storage implementation specified in configuration: {}",
                           pmodeStorageClass);
                deployedPModes = (IPModeSet) Class.forName(pmodeStorageClass).newInstance();
                // Initialize the P-Mode storage
                deployedPModes.init(config.getHolodeckB2BHome());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
               // Could not create the specified storage implementation, fall back to default implementation
               log.error("Could not load the specified P-Mode storage implementation: {}. Using default instead.",
                          pmodeStorageClass);
               deployedPModes = new InMemoryPModeSet();
            }
        } else
            // No specific validator, use default one
            deployedPModes = new InMemoryPModeSet();

        // Log configuration
        StringBuilder   logMsg = new StringBuilder("Initialized P-Mode manager:\n");
        logMsg.append("\tP-Mode validator    : ").append(validator.getClass().getName()).append('\n')
              .append("\tP-Mode storage impl.: ").append(deployedPModes.getClass().getName()).append('\n');
        log.info(logMsg.toString());
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

        // Validate the new P-Mode
        Collection<PModeValidationError> validationErrors = validator.validatePMode(pmode);
        if (Utils.isNullOrEmpty(validationErrors)) {
            log.debug("No errors found in new P-Mode, adding to deployed set of P-Modes");
            try {
                String pmodeId = deployedPModes.add(pmode);
                log.info("Successfully deployed P-Mode [{}]", pmodeId);
                return pmodeId;
            } catch (PModeSetException deploymentException) {
                log.error("Could not deploy new P-Mode due to exception in storage implementation! Error message: {}",
                           deploymentException.getMessage());
                throw deploymentException;
            }
        } else {
            log.warn("The new P-Mode is not valid, validator found {} errors!", validationErrors.size());
            throw new InvalidPModeException(validationErrors);
        }
    }

    @Override
    public void replace(IPMode pmode) throws PModeSetException {
        log.trace("Request to replace P-Mode [{}]", pmode.getId());

        // Validate the new P-Mode
        Collection<PModeValidationError> validationErrors = validator.validatePMode(pmode);
        if (Utils.isNullOrEmpty(validationErrors)) {
            log.debug("No errors found in new version of P-Mode, replacing it in the deployed set of P-Modes");
            try {
                deployedPModes.replace(pmode);
                log.info("Successfully deployed change version of P-Mode [{}]", pmode.getId());
            } catch (PModeSetException deploymentException) {
                log.error("Could not replace P-Mode due to exception in storage implementation! Error message: {}",
                           deploymentException.getMessage());
                throw deploymentException;
            }
        } else {
            log.warn("The new version of the P-Mode is not valid, validator found {} errors!", validationErrors.size());
            throw new InvalidPModeException(validationErrors);
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
