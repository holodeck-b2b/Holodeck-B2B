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
import java.util.Arrays;
import java.util.Collection;
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
import org.holodeckb2b.interfaces.pmode.IPModeSetListener;
import org.holodeckb2b.interfaces.pmode.IPModeStorage;
import org.holodeckb2b.interfaces.pmode.PModeSetEvent;
import org.holodeckb2b.interfaces.pmode.PModeSetEvent.PModeSetAction;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.InvalidPModeException;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

/**
 * Manages the set of deployed P-Mode of a Holodeck B2B installation. It acts as a <i>facade</i> to the actual storage
 * of deployed P-Mode to ensure that a new P-Mode that is being deployed is validated before deployment and changes in
 * the set of registered P-Modes are notified to interested components.
 * <p>
 * The actual storage component is loaded through the Java <i>Service Provider Interface</i> mechanism by loading the
 * first available implementation of {@link IPModeStorage}. If no implementation is available the default implementation
 * {@link InMemoryPModeSet} is used to store the P-Modes in memory.
 * <p>
 * Also for the P-Mode validation external components are used so validation can be customised to specific requirements.
 * Again validators are loaded through the Java SPI mechanism, but here all implementations are loaded. Which ones are
 * used to validate a specific P-Mode is determined by the P-Mode's type, defined by the <b>PMode.MEPBinding</p>
 * parameter.<br/>
 * Whether a P-Mode for which no validator is available should still be loaded or be rejected depends on the
 * configuration setting {@link InternalConfiguration#acceptNonValidablePMode()}.
 * <p>
 *
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @see IPModeStorage
 * @see org.holodeckb2b.interfaces.pmode.validation
 * @see IPModeSetListener
 */
public class PModeManager implements IPModeSet {
    private static final Logger log = LogManager.getLogger(PModeManager.class);

    private static final List<PModeSetEvent.PModeSetAction> ALL_ACTIONS = List.of(PModeSetAction.ADD,
    																	PModeSetAction.UPDATE, PModeSetAction.REMOVE);
    /**
     * The actual store of deployed P-Modes
     */
    private IPModeStorage   deployedPModes;

    /**
     * The P-Mode validators in use to check P-Modes before being deployed
     */
    private List<IPModeValidator> validators;

    /**
     * Indicator whether P-Modes for which no validator is available should still be accepted.
     */
    private boolean acceptNonValidable = true;

    /**
     * The list of event listeners to inform when a P-Mode is added
     */
    private List<IPModeSetListener>		addEventListeners = new ArrayList<>();
    /**
     * The list of event listeners to inform when a P-Mode is updated
     */
    private List<IPModeSetListener>		updateEventListeners = new ArrayList<>();
    /**
     * The list of event listeners to inform when a P-Mode is removed
     */
    private List<IPModeSetListener>		removeEventListeners = new ArrayList<>();

    /**
     * Initialises a new <code>PModeManager</code> which will use the {@link IPModeSet} implementation as specified in
     * the configuration for storing the deployed the P-Modes. If not specified the default {@link InMemoryPModeSet}
     * implementation will be used.
     *
     * @param config   The configuration of this instance
     * @throws PModeSetException When no P-Mode validators are available (maybe due to Java SPI issue) but validation of
     * 							 P-Modes is required (i.e. non validable P-Modes are configured to be rejected)
     */
    public PModeManager(final IConfiguration config) throws PModeSetException {
        log.trace("Load P-Mode storage component");
        deployedPModes = Utils.getFirstAvailableProvider(IPModeStorage.class);
        String storageClsName;
        if (deployedPModes != null) {
            // A specific P-Mode storage implementation is available, try to initialise it
        	try {
                log.debug("Initialising P-Mode storage implementation: {}", deployedPModes.getClass().getName());
                deployedPModes.init(config);
            } catch (PModeSetException initFailure) {
               // Could not initialise the custom storage implementation
               log.error("Could not initialise the P-Mode storage implementation: {}. Error details: {}",
            		   	 deployedPModes.getClass().getName(), initFailure.getMessage());
               throw new PModeSetException("Could not load P-Mode storage implementation!", initFailure);
            }
        } else
            deployedPModes = new InMemoryPModeSet();
    	log.debug("Load installed P-Mode validators");
    	validators = new ArrayList<>();
    	ServiceLoader.load(IPModeValidator.class).forEach(v -> validators.add(v));

    	acceptNonValidable = ((InternalConfiguration) config).acceptNonValidablePMode();
        // If only the TLS config validator is loaded and validation is required we have a problem, otherwise we just
    	// log the loaded validators
        if (!acceptNonValidable && validators.size() == 1) {
        	log.fatal("No specific P-Mode validators are available, but validation is required!");
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
        try {
        	log.trace("Request to add P-Mode");
        	validatePMode(pmode);
        	log.trace("Adding to deployed set of P-Modes");
        	String pmodeId = deployedPModes.add(pmode);
           	log.info("Successfully deployed P-Mode [{}]", pmodeId);
           	informListeners(addEventListeners, new PModeSetEvent(pmode, PModeSetAction.ADD));
           	return pmodeId;
        } catch (PModeSetException deploymentException) {
            log.error("Could not deploy new P-Mode [{}] due to exception : {}", pmode.getId(),
            			deploymentException.getMessage());
            informListeners(addEventListeners, new PModeSetEvent(pmode, PModeSetAction.ADD, deploymentException));
            throw deploymentException;
        }
    }

    @Override
    public void replace(IPMode pmode) throws PModeSetException {
        try {
        	log.trace("Request to replace P-Mode [{}]", pmode.getId());
        	validatePMode(pmode);
        	log.trace("Replacing current version in the deployed set of P-Modes");
        	deployedPModes.replace(pmode);
        	log.info("Successfully deployed change version of P-Mode [{}]", pmode.getId());
        	informListeners(updateEventListeners, new PModeSetEvent(pmode, PModeSetAction.UPDATE));
        } catch (PModeSetException deploymentException) {
        	log.error("Could not replace P-Mode [{}] due to exception : {}", pmode.getId(),
        			deploymentException.getMessage());
        	informListeners(updateEventListeners, new PModeSetEvent(pmode, PModeSetAction.UPDATE, deploymentException));
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
    	IPMode removed = deployedPModes.get(id);
    	try {
	    	log.trace("Request to remove P-Mode [{}]", id);
	    	deployedPModes.remove(id);
	    	log.trace("P-Mode [{}] removed", id);
	    	if (removed != null)
	    		informListeners(removeEventListeners, new PModeSetEvent(removed, PModeSetAction.REMOVE));
    	} catch (PModeSetException pmse) {
    		log.error("Error removing P-Mode [{}] from storage : {}", id, pmse.getMessage());
    		if (removed != null)
    			informListeners(removeEventListeners, new PModeSetEvent(removed, PModeSetAction.REMOVE, pmse));
    		throw pmse;
    	}
    }

    @Override
    public void removeAll() throws PModeSetException {
    	Collection<IPMode> removed = deployedPModes.getAll();
    	log.trace("Request to remove all P-Modes");
        deployedPModes.removeAll();
        log.trace("Removed all P-Modes");
        // Note that we don't inform event listener on an exception as we don't know to which P-Mode the problem
        // relates
        if (!Utils.isNullOrEmpty(removed))
        	removed.forEach(pm -> informListeners(removeEventListeners, new PModeSetEvent(pm, PModeSetAction.REMOVE)));
    }

    @Override
    public void registerEventListener(IPModeSetListener listener, PModeSetAction... actions) {
    	if (listener == null)
    		throw new IllegalArgumentException("Listener to add must be specified");

    	List<PModeSetAction> applyTo = actions != null && actions.length > 0 ? Arrays.asList(actions) : ALL_ACTIONS;

    	final String lstnrClsName = listener.getClass().getName();
    	if (applyTo.contains(PModeSetAction.ADD)) {
    		log.debug("Adding event listener ({}) for add events", lstnrClsName);
    		addEventListeners.add(listener);
    	}
    	if (applyTo.contains(PModeSetAction.UPDATE)) {
    		log.debug("Adding event listener ({}) for update events", lstnrClsName);
    		updateEventListeners.add(listener);
    	}
    	if (applyTo.contains(PModeSetAction.REMOVE)) {
    		log.debug("Adding event listener ({}) for remove events", lstnrClsName);
    		removeEventListeners.add(listener);
    	}
    }

    @Override
    public void unregisterEventListener(IPModeSetListener listener, PModeSetAction... actions) {
    	if (listener == null)
    		throw new IllegalArgumentException("Listener to remove must be specified");

    	List<PModeSetAction> applyTo = actions != null && actions.length > 0 ? Arrays.asList(actions) : ALL_ACTIONS;

    	final String lstnrClsName = listener.getClass().getName();
    	if (applyTo.contains(PModeSetAction.ADD)) {
    		log.debug("Removing event listener ({}) for add events", lstnrClsName);
    		addEventListeners.remove(listener);
    	}
    	if (applyTo.contains(PModeSetAction.UPDATE)) {
    		log.debug("Removing event listener ({}) for update events", lstnrClsName);
    		updateEventListeners.remove(listener);
    	}
    	if (applyTo.contains(PModeSetAction.REMOVE)) {
    		log.debug("Removing event listener ({}) for remove events", lstnrClsName);
    		removeEventListeners.remove(listener);
    	}
    }

    /**
     * Helper method to inform all registered listeners on an event. Will catch and log any exception that occurs during
     * event handling.
     *
     * @param listeners list of listeners to be informed about the event
     * @param event		the event that occured and must be notified to the listeners
     */
    private void informListeners(List<IPModeSetListener> listeners, PModeSetEvent event) {
    	for(IPModeSetListener l : listeners)
    		try {
    			log.trace("Calling {} listener to inform about {} event on P-Mode ({})", l.getClass().getName(),
    						event.getEventType().name(), event.getSource().getId());
    			l.handleEvent(event);
    			log.trace("Event handled by listener");
    		} catch (Throwable t) {
    			log.warn("An exception occurred in listener ({}) while handling {} event for P-Mode ({}) : {}",
    					 l.getClass().getName(), event.getEventType().name(), event.getSource().getId(),
    					 Utils.getExceptionTrace(t));
    		}
    }
}
