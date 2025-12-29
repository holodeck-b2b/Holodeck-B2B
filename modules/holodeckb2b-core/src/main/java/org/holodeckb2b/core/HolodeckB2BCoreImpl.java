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

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisError;
import org.apache.axis2.modules.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.events.SyncEventProcessor;
import org.holodeckb2b.common.workerpool.XMLWorkerPoolConfiguration;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.pmode.PModeManager;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.core.storage.QueryManager;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.core.submission.MessageSubmitter;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.core.validation.IValidationExecutor;
import org.holodeckb2b.core.workerpool.WorkerPool;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.core.IQueryManager;
import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.general.IVersionInfo;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.storage.AlreadyChangedException;
import org.holodeckb2b.interfaces.storage.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPool;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;

/**
 * The Holodeck B2B Core which provides access to and ensures that core components like the P-Mode and persistency
 * provider.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class HolodeckB2BCoreImpl implements IHolodeckB2BCore {
    private static final class SubmitterSingletonHolder {
        static final IMessageSubmitter instance = new MessageSubmitter();
    }

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
     * The Metadata Storage Provider in use
     */
    private IMetadataStorageProvider mdsProvider;

    /**
     * The Payload Storage Provider in use
     */
    private IPayloadStorageProvider psProvider;

    /**
     * The storage manager facade to manage updates of the message units' data
     */
    private StorageManager	storageManager = null;

    /**
     * The query manager facade to manage updates of the message units' data
     */
    private QueryManager	queryManager = null;

    /**
     * The installed certificate manager that manages and checks certificates used in the message processing
     * @since 5.0.0
     */
    private ICertificateManager certManager = null;

    /**
     * The installed delivery manager that handles the delivery of message units to the back-end application
     * @since 6.0.0
     */
    private DeliveryManager deliveryManager = null;

    /**
     * The list of globally configured event handlers
     *
     */
    private List<IMessageProcessingEventConfiguration>	eventConfigurations = null;

    // This constructor is only here so a test mock can use it as base class, it should be removed!
    protected HolodeckB2BCoreImpl() {};

    /**
     * Initializes the Holodeck B2B Core.
     *
     * @param config the loaded configuration file
     * @throws AxisFault	when the Core cannot be initialised correctly
     */
    HolodeckB2BCoreImpl(final InternalConfiguration config) throws AxisFault {
        log.info("Starting Holodeck B2B Core...");
        System.out.println("Starting Holodeck B2B Core...");
        this.instanceConfiguration = config;
        try {
        	log.trace("Initialize the P-Mode manager");
			pmodeManager = new PModeManager(config);
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
	        	eventProcessor.init(instanceConfiguration);
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
        eventConfigurations = new ArrayList<>();

        log.debug("Load the Metadata Storage provider");
        mdsProvider = Utils.getFirstAvailableProvider(IMetadataStorageProvider.class);
        if (mdsProvider != null) {
        	log.debug("Using Metadata Storage Provider: {}", mdsProvider.getName());
        	try {
        		mdsProvider.init(instanceConfiguration);
        		log.info("Loaded Metadata Storage Provider : {}", mdsProvider.getName());
        	} catch (StorageException initializationFailure) {
        		log.error("Could not initialize the Metadata Storage Provider - {} : {}", mdsProvider.getName(),
        				initializationFailure.getMessage());
        		mdsProvider = null;
        	}
        }
        log.debug("Load the Payload Storage provider");
        psProvider = Utils.getFirstAvailableProvider(IPayloadStorageProvider.class);
        if (psProvider != null) {
        	log.debug("Using Payload Storage Provider: {}", psProvider.getName());
        	try {
        		psProvider.init(instanceConfiguration);
        		log.info("Loaded Payload Storage Provider : {}", psProvider.getName());
        	} catch (StorageException initializationFailure) {
        		log.error("Could not initialize the Payload Storage Provider - {} : {}", psProvider.getName(),
        				initializationFailure.getMessage());
        		psProvider = null;
        	}
        }

        if (mdsProvider == null || psProvider == null) {
        	log.fatal("Cannot start Holodeck B2B because required Metadata or Payload Storage Provider is not available!");
        	throw new AxisFault("Required Metadata or Payload Storage provider not available!");
        }

        storageManager = new StorageManager(mdsProvider, psProvider);
        queryManager = new QueryManager(mdsProvider, psProvider);

        log.trace("Load the certificate manager");
    	certManager = Utils.getFirstAvailableProvider(ICertificateManager.class);
    	if (certManager != null) {
	        log.debug("Using certificate manager: {}", certManager.getName());
	        try {
	        	certManager.init(instanceConfiguration);
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

        log.trace("Initialise the Delivery Manager");
        deliveryManager = new DeliveryManager(storageManager, config);
        log.info("Initialised the Delivery Manager");

        log.trace("Create list of managed worker pools");
        workerPools = new HashMap<>();

        // From this point on other components can be started which need access to the Core
        log.debug("Make Core available to outside world");
        HolodeckB2BCore.setImplementation(this);

        log.trace("Initialize Core worker pool");
        XMLWorkerPoolConfiguration poolConfiguration;
        try {
        	poolConfiguration = new XMLWorkerPoolConfiguration(instanceConfiguration.getWorkerPoolCfgFile());
        } catch (WorkerPoolException corePoolCfgError) {
	        log.fatal("Could not load the workers configuration file {} : {}",
	        			instanceConfiguration.getWorkerPoolCfgFile(), corePoolCfgError.getMessage());
			throw new AxisFault("Unable to start Holodeck B2B. Could not load workers from file "
								+ instanceConfiguration.getWorkerPoolCfgFile());
        }
        try {
        	createWorkerPool("hb2b-core", poolConfiguration);
        } catch (WorkerPoolException corePoolCfgError) {
        	// As the workers are needed for correct functioning of Holodeck B2B, failure to either
            // load the configuration or start the pool is fatal.
        	log.fatal("Could not load workers from file {}. Failed workers are: {}",
            			instanceConfiguration.getWorkerPoolCfgFile(), corePoolCfgError.getFailedWorkers().stream()
            																		  .map(c -> c.getName()).toArray());
            throw new AxisFault("Unable to start Holodeck B2B. Could not load workers from file "
                                + instanceConfiguration.getWorkerPoolCfgFile());
        }

        log.info("Holodeck B2B Core " + VersionInfo.fullVersion + " STARTED.");
        System.out.println("Holodeck B2B Core started.");
    }

	public void shutdown() {
        log.info("Shutting down Holodeck B2B Core...");
        log.trace("Stopping worker pools");
        workerPools.forEach((n, p) -> { try {
        									log.trace("Stopping worker pool: {}", n);
        									p.shutdown(10);
        								} catch (Throwable t) {
        									log.error("Error during worker pool ({}) shutdown: {}", n,
        												Utils.getExceptionTrace(t));
        								}
        							  });
        log.debug("Worker pools stopped");
        try {
        	log.trace("Shutting down Certificate Manager");
        	certManager.shutdown();
        	log.debug("Certificate Manager shut down");
        } catch (Throwable t) {
        	log.error("Error during Certificate Manager shutdown: {}", Utils.getExceptionTrace(t));
        }
        try {
        	log.trace("Shutting down Delivery Manager");
        	deliveryManager.shutdown();
        	log.debug("Delivery Manager shut down");
        } catch (Throwable t) {
        	log.error("Error during Delivery Manager shutdown: {}", Utils.getExceptionTrace(t));
        }
        try {
        	log.trace("Shutting down Metadata Storage Provider");
        	mdsProvider.shutdown();
        	log.debug("Metadata Storage Provider shut down");
        } catch (Throwable t) {
        	log.error("Error during Metadata Storage Provider shutdown: {}", Utils.getExceptionTrace(t));
        }
        try {
        	log.trace("Shutting down Payload Storage Provider");
        	psProvider.shutdown();
        	log.debug("Payload Storage Provider shut down");
        } catch (Throwable t) {
        	log.error("Error during Payload Storage Provider shutdown: {}", Utils.getExceptionTrace(t));
        }
        try {
        	log.trace("Shutting down Event Processor");
        	eventProcessor.shutdown();
        	log.debug("Event processor shut down");
        } catch (Throwable t) {
        	log.error("Error during Event Processor shutdown: {}", Utils.getExceptionTrace(t));
        }
        try {
        	log.trace("Shutting down P-Mode Manager");
        	pmodeManager.shutdown();
        	log.debug("P-Mode Manager shut down");
        } catch (Throwable t) {
        	log.error("Error during P-Mode Manager shutdown: {}", Utils.getExceptionTrace(t));
        }
        log.info("Holodeck B2B Core STOPPED.");
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
     * @see HolodeckB2BCore#getStorageManager()
     * @since 3.0.0
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }

    /**
     * {@inheritDoc}
     * @since  3.0.0
     */
    @Override
    public IQueryManager getQueryManager() {
        return queryManager;
    }

    /**
     * @see HolodeckB2BCore#getValidationExecutor()
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
     * {@inheritDoc}
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
    		eventConfigurations.set(i - 1, eventConfiguration);
    	} else
    		eventConfigurations.add(eventConfiguration);
    	log.info("Registered event handler configuration [id={}]", id);
    	return exists;
    }

    /**
     * {@inheritDoc}
     * @since 4.1.0
     */
    @Override
	public void removeEventHandler(String id) {
    	int i; boolean exists = false;
    	for(i = 0; i < eventConfigurations.size() && !exists; i++)
    		exists = eventConfigurations.get(i).getId().equals(id);
    	if (exists) {
    		eventConfigurations.remove(i - 1);
    		log.info("Removing event handler configuration [id=]", id);
    	} else
    		log.warn("No event handler configuration registered for id=[{}]", id);
    }

    /**
     * {@inheritDoc}
     * @since 4.1.0
     */
    @Override
	public List<IMessageProcessingEventConfiguration> getEventHandlerConfiguration() {
    	return eventConfigurations;
    }

    /**
     * {@inheritDoc}
     * @since 5.0.0
     */
    @Override
	public IVersionInfo getVersion() {
    	return VersionInfo.getInstance();
    }

    /**
     * {@inheritDoc}
     * @since 5.0.0
     */
    @Override
    public Module getModule(final String name) {
		final AxisModule module = instanceConfiguration.getModule(name);
		// The AxisModule is only meta-data on the module, we need to get the actual implementing class from it
		return module != null ? module.getModule() : null;
    }

    /**
     * {@inheritDoc}
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
    	} catch (WorkerPoolException poolFailure) {
    		throw poolFailure;
    	} catch (Throwable unexpected) {
    		log.error("An error occurred creating the new worker pool ({}). Error details: {} - {}", name,
    					unexpected.getClass().getSimpleName(), unexpected.getMessage());
    		throw new WorkerPoolException("Unexpected worker pool failure", unexpected);
    	}
    }

    /**
     * {@inheritDoc}
     * @since 5.1.0
     */
    @Override
    public IWorkerPool getWorkerPool(final String name) {
    	return workerPools != null ? workerPools.get(name) : null;
    }

    /**
     * {@inheritDoc}
     * @since 5.3.0
     */
    @Override
    public void resumeProcessing(IUserMessageEntity userMessage) throws StorageException, IllegalArgumentException {
    	if (userMessage.getDirection() == Direction.IN) {
    		log.warn("Illegal request to resume processing of received message unit [msgId={}]",
    					userMessage.getMessageId());
    		throw new IllegalArgumentException("Incoming message unit cannot be resumed");
    	}
    	if (userMessage.getCurrentProcessingState().getState() != ProcessingState.SUSPENDED) {
    		log.warn("Processing state of message unit [msgId={}] has already changed to {}",
					userMessage.getMessageId(), userMessage.getCurrentProcessingState().getState());
    		return;
    	}

    	ProcessingState newState = PModeUtils.doesHolodeckB2BTrigger(PModeUtils.getLeg(userMessage)) ?
    									ProcessingState.READY_TO_PUSH : ProcessingState.AWAITING_PULL;
    	log.trace("Resume processing of User Message [msgId={}], set proc state to {}", userMessage.getMessageId(),
    				newState.name());
    	boolean resumed;
    	try {
    		resumed = getStorageManager().setProcessingState(userMessage, newState);
    	} catch (AlreadyChangedException changed) {
    		resumed = false;
    	}
    	if (resumed)
    		log.info("Processing of User Message [msgId={}] resumed", userMessage.getMessageId());
    	else
    		log.info("Processing of User Message [msgId={}] already changed.", userMessage.getMessageId());
    }

    /**
     * {@inheritDoc}
     * @since 6.0.0
     */
    @Override
    public IDeliveryManager getDeliveryManager() {
    	return deliveryManager;
    }

}
