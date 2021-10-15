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
package org.holodeckb2b.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisError;
import org.apache.axis2.modules.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.events.SyncEventProcessor;
import org.holodeckb2b.common.workerpool.XMLWorkerPoolConfiguration;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.pmode.PModeManager;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.core.submission.MessageSubmitter;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.core.validation.IValidationExecutor;
import org.holodeckb2b.core.workerpool.WorkerPool;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.general.IVersionInfo;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPool;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;

/**
 * Axis2 module class for the Holodeck B2B Core module.
 * <p>This class is responsible for the initialization and shutdown of the ebMS module. This includes
 * starting and stopping the workers needed to drive the message exchanges.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class HolodeckB2BCoreImpl implements Module, IHolodeckB2BCore {
    private static final class SubmitterSingletonHolder {
        static final IMessageSubmitter instance = new MessageSubmitter();
    }

    /**
     * The name of the Axis2 Module that contains the Holodeck B2B Core implementation
     */
    public static final String HOLODECKB2B_CORE_MODULE = "holodeckb2b-core";

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(HolodeckB2BCoreImpl.class);

    /**
     * The configuration of this Holodeck B2B instance
     */
    private InternalConfiguration  instanceConfiguration = null;

    /**
     * The list of worker pools running in this Holodeck B2B instance
     */
    private HashMap<String, WorkerPool>      workerPools = null;

    /**
     * Collection of active message delivery methods mapped by the <i>id</i> of the {@link IDeliverySpecification} that
     * defined their configuration.
     * <p>For each unique delivery specification id Holodeck B2B will create factory class that creates the actual
     * {@link IMessageDeliverer} objects that are used to deliver messages to the business application.
     */
    private Map<String, IMessageDelivererFactory>    msgDeliveryFactories = null;

    /**
     * The P-Mode manager that maintains the set of deployed P-Modes
     */
    private PModeManager pmodeManager = null;

    /**
     * The component responsible for processing of events that occur while processing a message. The processor will
     * pass the events on to the configured event handlers.
     * @since 2.1.0
     */
    private IMessageProcessingEventProcessor eventProcessor = null;

    /**
     * The persistency provider that manages the storage of the meta-data on processed message units.
     * @since  5.0.0 In previous versions the DAOFactory was referenced. 
     */
    private IPersistencyProvider    persistencyProvider = null;
    
    /**
     * The installed certificate manager that manages and checks certificates used in the message processing
     * @since 5.0.0
     */
    private ICertificateManager certManager = null;
    
    /**
     * The list of globally configured event handlers 
     * 
     */
    private List<IMessageProcessingEventConfiguration>	eventConfigurations = null;

    /**
     * Initializes the Holodeck B2B Core module.
     *
     * @param cc
     * @param am
     * @throws AxisFault
     */
    @Override
    public void init(final ConfigurationContext cc, final AxisModule am) throws AxisFault {
        log.info("Starting Holodeck B2B Core module...");

        System.out.println("Starting Holodeck B2B Core module...");

        // Check if module name in module.xml is equal to constant use in code
        if (!am.getName().equals(HOLODECKB2B_CORE_MODULE)) {
            // Name is not equal! This is a fatal configuration error, stop loading this module and alert operator
            log.fatal("Invalid Holodeck B2B Core module configuration! Name in configuration is: {}, expected was: {}",
            			am.getName(), HOLODECKB2B_CORE_MODULE);
            throw new AxisFault("Invalid configuration found for module: " + am.getName());
        }

        try {
            instanceConfiguration = (InternalConfiguration) cc.getAxisConfiguration();
        } catch (ClassCastException nonH2BConfig) {
            log.fatal("Incorrect configuration found. Found {} instead of {}", 
            			cc.getAxisConfiguration().getClass().getName(), InternalConfiguration.class.getName());
            throw new AxisFault("Could not initialize Holodeck B2B module!", nonH2BConfig);
        }

        try {
        	log.trace("Initialize the P-Mode manager");
			pmodeManager = new PModeManager(instanceConfiguration);
		} catch (PModeSetException e) {
			log.fatal("Cannot start Holodeck B2B because P-Mode manager couldn't be initialised!\n\tError details: {}", 
						e.getMessage());
			throw new AxisFault("Could not initialize Holodeck B2B module!", e);
		}

        log.trace("Load the event processor");
    	eventProcessor = Utils.getFirstAvailableProvider(IMessageProcessingEventProcessor.class);
    	if (eventProcessor == null && instanceConfiguration.eventProcessorFallback()) 
    		eventProcessor = new SyncEventProcessor();    	
    	if (eventProcessor != null) {
	        try {
	        	log.trace("Initialising event processor : {}", eventProcessor.getName());
	        	eventProcessor.init(instanceConfiguration.getHolodeckB2BHome());
	        } catch (MessageProccesingEventHandlingException initializationFailure) {
	        	log.error("Could not initialize the event processor - {} : {}", eventProcessor.getName(),
	        				initializationFailure.getMessage());
	        	eventProcessor = null;
	        }
    	}
    	if (eventProcessor == null) {
    		log.fatal("No event processor available, cannot start Holodeck B2B!");
    		throw new AxisError("No event processor available!");
    	}
        log.info("Loaded event processor : {}", eventProcessor.getName());

        log.debug("Load the persistency provider for storing meta-data on message units");
        persistencyProvider = Utils.getFirstAvailableProvider(IPersistencyProvider.class);
        if (persistencyProvider != null) {	        	
        	log.debug("Using Persistency Provider: {}", persistencyProvider.getName());
        	try {
        		persistencyProvider.init(instanceConfiguration.getHolodeckB2BHome());
        	} catch (PersistenceException initializationFailure) {
        		log.error("Could not initialize the persistency provider - {} : {}", persistencyProvider.getName(),
        				initializationFailure.getMessage());
        		persistencyProvider = null;
        	}
        }
        if (persistencyProvider == null) {
        	log.fatal("Cannot start Holodeck B2B because required Persistency Provider is not available!");
        	throw new AxisFault("Unable to load required persistency provider!");
        }
        log.info("Loaded Persistency Provider : {}", persistencyProvider.getName());
        
        log.trace("Load the certificate manager");
    	certManager = Utils.getFirstAvailableProvider(ICertificateManager.class); 
    	if (certManager != null) {	        	
	        log.debug("Using certificate manager: {}", certManager.getName());
	        try {
	        	certManager.init(instanceConfiguration.getHolodeckB2BHome());
	        } catch (SecurityProcessingException initializationFailure) {
	        	log.error("Could not initialize the certificate manager - {} : {}", certManager.getName(),
	        				initializationFailure.getMessage());
	        	certManager = null;
	        }
    	}
    	if (certManager == null) {
    		log.fatal("Cannot starrt Holodeck B2B because required Certificate Manager is not available!");
        	throw new AxisFault("Unable to load required certificate manager!");
        }
        log.info("Loaded Certficate Manager : {}", certManager.getName());
        
        log.trace("Create list of available message delivery methods");
        msgDeliveryFactories = new HashMap<>();
        log.trace("Create list of globally configured event handlers");
        eventConfigurations = new ArrayList<>();        
        log.trace("Create list of managed worker pools");
        workerPools = new HashMap<>();
        
        // From this point on other components can be started which need access to the Core
        log.debug("Make Core available to outside world");
        HolodeckB2BCore.setImplementation(this);

        log.trace("Initialize Core worker pool");
        
        XMLWorkerPoolConfiguration corePoolCfg = null;
        try {
        	createWorkerPool("hb2b-core", new XMLWorkerPoolConfiguration(instanceConfiguration.getWorkerPoolCfgFile()));
        } catch (WorkerPoolException corePoolCfgError) {        	
        	// As the workers are needed for correct functioning of Holodeck B2B, failure to either
            // load the configuration or start the pool is fatal.
            log.fatal("Could not load workers from file {}. Error details: {}", 
            			instanceConfiguration.getWorkerPoolCfgFile(), Utils.getExceptionTrace(corePoolCfgError));
            throw new AxisFault("Unable to start Holodeck B2B. Could not load workers from file "
                                + instanceConfiguration.getWorkerPoolCfgFile());
        }

        log.info("Holodeck B2B Core " + VersionInfo.fullVersion + " STARTED.");
        System.out.println("Holodeck B2B Core module started.");      
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
        log.info("Shutting down Holodeck B2B Core module...");

        log.trace("Stopping worker pools");
        workerPools.forEach((n, p) -> { log.trace("Stopping worker pool: {}", n);
        								p.shutdown(10);
        							  });
        log.debug("Worker pools stopped");
        log.info("Holodeck B2B Core module STOPPED.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalConfiguration getConfiguration() {
        if (instanceConfiguration == null) {
            log.fatal("Missing configuration for this Holodeck B2B instance!");
            throw new IllegalStateException("Missing configuration for this Holodeck B2B instance!");
        } else
            return instanceConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IMessageSubmitter getMessageSubmitter() {
        return SubmitterSingletonHolder.instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IMessageDeliverer getMessageDeliverer(final IDeliverySpecification deliverySpec)
                                                        throws MessageDeliveryException {
        if (deliverySpec == null) {
            log.error("No delivery specification given!");
            throw new MessageDeliveryException("No delivery specification given!");
        }

        log.trace("Check if there is a factory available for this specification [{}]", deliverySpec.getId());
        IMessageDelivererFactory mdf = msgDeliveryFactories.get(deliverySpec.getId());

        if (mdf == null) {
            try {
                log.trace("No factory available yet for this specification [{}]", deliverySpec.getId());
                final String factoryClassName = deliverySpec.getFactory();
                log.debug("Create a factory [{}] for delivery specification [{}]",factoryClassName,deliverySpec.getId());
                mdf = (IMessageDelivererFactory) Class.forName(factoryClassName).newInstance();
                // Initialize the new factory with the settings from the delivery spec
                mdf.init(deliverySpec.getSettings());
                log.debug("Created factory [{}] for delivery specification [{}]",factoryClassName,deliverySpec.getId());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | ClassCastException | MessageDeliveryException ex) {
                // Somehow the factory class failed to load
                log.error("The factory for delivery specification [{}] could not be created! Error details: {}",
                			deliverySpec.getId(), ex.getMessage());
                throw new MessageDeliveryException("Factory class not available!", ex);
            }
            // Add the new factory to the list of available factories
            //@todo: Synchronizing here does not prevent that multiple instances of a factory can be created, should we change this?
            synchronized (msgDeliveryFactories) {
                if (msgDeliveryFactories.get(deliverySpec.getId()) == null)
                    msgDeliveryFactories.put(deliverySpec.getId(), mdf);
            }
            log.trace("Added new factory to list of available delivery methods");
        }

        log.trace("Get and return deliverer from the factory");
        return mdf.createMessageDeliverer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPModeSet getPModeSet() {
        return pmodeManager;
    }

    /**
     * {@inheritDoc}
     * @since 2.1.0
     */
    @Override
    public IMessageProcessingEventProcessor getEventProcessor() {
        return eventProcessor;
    }

    /**
     * Gets the data access object that should be used to store and update the meta-data on processed message units.
     * <p>The returned data access object is a facade to the one provided by the persistency provider to ensure that
     * changes in the message unit meta-data are managed correctly.
     *
     * @return  The {@link StorageManager} that Core classes should use to update meta-data of message units
     * @since  3.0.0
     */
    public StorageManager getStorageManager() {
        return new StorageManager(persistencyProvider.getUpdateManager());
    }

    /**
     * {@inheritDoc}
     * @since  3.0.0
     */
    @Override
    public IQueryManager getQueryManager() {
        return persistencyProvider.getQueryManager();
    }

    /**
     * Gets the {@link IValidationExecutor} implementation that should be used for the execution of the custom
     * message validations.<br>
     * Currently the executor is not configurable and a simple, i.e. non-optimized, executor is used, see {@link
     * DefaultValidationExecutor}
     *
     * @return  The component responsible for execution of the custom validations.
     * @since 4.0.0
     */
    public IValidationExecutor getValidationExecutor() {
        return new DefaultValidationExecutor();
    }

    /**
     * {@inheritDoc}
     * @since 4.0.0
     */
    @Override
    public ICertificateManager getCertificateManager() {
        return certManager;
    }

    /**
     * Registers a <i>global</i> event handler for handling {@link IMessageProcessingEvent}s that occur during the 
     * processing of messages. If there is already a configuration registered with the same <code>id</code> it will be
     * replaced by the new configuration.
     * <p>NOTE: When the P-Mode of a message also defines an event handler for an event for which also a global 
     * configuration exists the one in the P-Mode takes precedence over the global configuration.
     * 
     * @param eventConfiguration	The event handler's configuration  	
     * @return 						<code>true</code> if an existing event configuration was replaced,
     * 								<code>false</code> if this was a new registration 
     * @throws MessageProccesingEventHandlingException When the given event handler configuration cannot be registered,
     * 												   for example because the handler class is not available or no id
     * 												   is specified
     * @since 4.1.0
     */
    @Override
	public boolean registerEventHandler(IMessageProcessingEventConfiguration eventConfiguration) 
    																	throws MessageProccesingEventHandlingException {
    	final String id = eventConfiguration.getId();
    	if (Utils.isNullOrEmpty(id)) {
    		log.error("Event configuration must have an id to register!");
    		throw new MessageProccesingEventHandlingException("No id specified");
    	}
    	int i; boolean exists = false;
    	for(i = 0; i < eventConfigurations.size() && !exists; i++)
    		exists = eventConfigurations.get(i).getId().equals(id);
    	if (exists) {
    		log.trace("Replacing existing event handler configuration [id={}]", id);
    		eventConfigurations.set(i, eventConfiguration);
    	} else 
    		eventConfigurations.add(eventConfiguration);
    	log.info("Registered event handler configuration [id={}]", id);
    	return exists;
    }
    
    /**
     * Removes a <i>global</i> event handler configuration.
     * 
     * @param id	The id of the event handler configuration to remove
     * @since 4.1.0 
     */
    @Override
	public void removeEventHandler(String id) {
    	int i; boolean exists = false;
    	for(i = 0; i < eventConfigurations.size() && !exists; i++)
    		exists = eventConfigurations.get(i).getId().equals(id);
    	if (exists) {    		
    		eventConfigurations.remove(i);
    		log.info("Removing event handler configuration [id=]", id);
    	} else
    		log.warn("No event handler configuration registered for id=[{}]", id);
    }
    
    /**
     * Gets the list of globally configured event handlers. 
     *  
     * @return		The list of event handler configurations 
     * @since 4.1.0
     */
    @Override
	public List<IMessageProcessingEventConfiguration> getEventHandlerConfiguration() {
    	return eventConfigurations;
    }
    
    /**
     * Gets information about the version of the Holodeck B2B Core of this instance. 
     *   
     * @return	The version info
     * @since 5.0.0
     */
    @Override
	public IVersionInfo getVersion() {
    	return VersionInfo.getInstance();
    }
    
    /**
     * Gets the active Axis2 Module with the given name. This can for example be used by protocol extension to get 
     * access to "their" module for protocol specific settings.  
     * 
     * @param name	the requested module's name
     * @return 		the active Axis2 module if it exists in this Holodeck B2B instance,<br><code>null</code> otherwise
     * @since 5.0.0
     */
    @Override
    public Module getModule(final String name) {
		final AxisModule module = instanceConfiguration.getModule(name);
		// The AxisModule is only meta-data on the module, we need to get the actual implementing class from it		
		return module != null ? module.getModule() : null;
    }
    
    /**
     * Creates a new worker pool using the provided name and configuration. 
     * 
     * @param name 				name to identify the new pool
     * @param configuration		the configuration for the new pool
     * @return the created worker pool 
     * @throws WorkerPoolException when the worker pool cannot be created because of an issue in the provided 
     * 							   configuration or that the pool name isn't unique.
     * @since 5.1.0 
     */
    @Override
    public IWorkerPool createWorkerPool(final String name, final IWorkerPoolConfiguration configuration) 
    																				throws WorkerPoolException {
    	if (Utils.isNullOrEmpty(name))
    		throw new IllegalArgumentException("A pool name must be provided");    	
    	if (configuration == null) 
    		throw new IllegalArgumentException("A pool configuration must be provided");
    	
    	if (workerPools.containsKey(name)) {
    		log.warn("Request to add a worker pool rejected as there already exists a pool with same name ({})", name);
    		throw new WorkerPoolException("Duplicate pool name");
    	}
    	
    	try {
    		log.trace("Creating new worker pool: {}", name);
    		final WorkerPool newPool = new WorkerPool(name, configuration);
    		log.trace("Starting new worker pool");
    		newPool.start();
    		workerPools.put(name, newPool);
    		
    		log.debug("Added new worker pool: {}", name);
    		return newPool;
    	} catch (Throwable poolFailure) {
    		log.error("An error occurred creating the new worker pool ({}). Error details: {} - {}", name, 
    					poolFailure.getClass().getSimpleName(), poolFailure.getMessage());
    		throw (poolFailure instanceof WorkerPoolException) ? (WorkerPoolException) poolFailure : 
    											new WorkerPoolException("Creating worker pool failed", poolFailure);
    	}    	
    }
    
    /**
     * Gets the worker pool with the given name.
     * 
     * @param name	of the worker pool to retrieve
     * @return		the worker pool with the given name, or <code>null</code> when no such pool exists
     * @since 5.1.0
     */
    public IWorkerPool getWorkerPool(final String name) {    	
    	return workerPools != null ? workerPools.get(name) : null;
    }
    
    /**
     * Resumes processing of the <i>suspended</i> User Message.  
     * <p>Note that only outgoing User Messages can be in suspended state and resumed. The resume operation will change 
     * the processing state from <i>SUSPENDED</i> to either <i>READY_TO_PUSH</i> or <i>AWAITING_PULL</i> depending on 
     * the MEP defined in the P-Mode. If the current processing state however has already changed it assumed that the 
     * message has already been resumed and no further action is needed.
     *  
     * @param userMessage	to be resumed 
     * @throws PersistenceException		when an error occurs updating the processing state of the message unit
     * @throws IllegalArgumentException when the given User Message is an incoming User Message 
     * @since 5.3.0
     */
    public void resumeProcessing(IUserMessageEntity userMessage) throws PersistenceException, IllegalArgumentException {
    	if (userMessage.getDirection() == Direction.IN) {
    		log.warn("Illegal request to resume processing of received message unit [msgId={}]", 
    					userMessage.getMessageId());
    		throw new IllegalArgumentException("Incoming message unit cannot be resumed");
    	}
    	
    	ProcessingState newState = PModeUtils.doesHolodeckB2BTrigger(PModeUtils.getLeg(userMessage)) ?
    									ProcessingState.READY_TO_PUSH : ProcessingState.AWAITING_PULL;
    	log.trace("Resume processing of User Message [msgId={}], set proc state to {}", userMessage.getMessageId(), 
    				newState.name());
    	getStorageManager().setProcessingState(userMessage, ProcessingState.SUSPENDED, newState);
    	if (userMessage.getCurrentProcessingState().getState() == newState)
    		log.info("Processing of User Message [msgId={}] resumed", userMessage.getMessageId());
    	else
    		log.info("Processing of User Message [msgId={}] already changed.", userMessage.getMessageId());
    }
    
}
