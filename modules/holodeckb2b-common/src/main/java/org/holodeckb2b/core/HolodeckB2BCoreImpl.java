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
import org.apache.axis2.modules.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.events.SyncEventProcessor;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.WorkerPool;
import org.holodeckb2b.common.workerpool.xml.XMLWorkerPoolConfig;
import org.holodeckb2b.core.config.Config;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.pmode.PModeManager;
import org.holodeckb2b.core.submission.MessageSubmitter;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.core.validation.IValidationExecutor;
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
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IDAOFactory;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;

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
    private static final Log log = LogFactory.getLog(HolodeckB2BCoreImpl.class);

    /**
     * The configuration of this Holodeck B2B instance
     */
    private InternalConfiguration  instanceConfiguration = null;

    /**
     * Pool of worker threads that handle recurring tasks like message sending and
     * resending.
     */
    private WorkerPool      workers = null;

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
     * The DAO factory object of the persistency provider that manages the storage of the meta-data on processed
     * message units.
     * @since  3.0.0
     */
    private IDAOFactory    daoFactory = null;

    /**
     * The installed security provider responsible for the processing of the WS-Security header of messages.
     * @since 4.0.0
     */
    private ISecurityProvider securityProvider = null;
    
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
            log.fatal("Invalid Holodeck B2B Core module configuration found! Name in configuration is: "
                        + am.getName() + ", expected was: " + HOLODECKB2B_CORE_MODULE);
            throw new AxisFault("Invalid configuration found for module: " + am.getName());
        }

        log.debug("Load configuration");
        try {
            instanceConfiguration = new Config(cc);
        } catch (final Exception ex) {
            log.fatal("Could not intialize configuration! ABORTING startup!"
                     + "\n\tError details: " + ex.getMessage());
            throw new AxisFault("Could not initialize Holodeck B2B module!", ex);
        }

        log.trace("Initialize the P-Mode manager");
        pmodeManager = new PModeManager(instanceConfiguration);

        final String eventProcessorClassname = instanceConfiguration.getMessageProcessingEventProcessor();
        if (!Utils.isNullOrEmpty(eventProcessorClassname)) {
        	log.debug("Using " + eventProcessorClassname + " as event processor");
            try {
               eventProcessor = (IMessageProcessingEventProcessor) Class.forName(eventProcessorClassname).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
               // Could not create the specified event processor, fall back to default implementation
               log.error("Could not load the specified event processor: " + eventProcessorClassname
                        + ". Using default implementation instead.");
            }
        } else
            eventProcessor = new SyncEventProcessor();
        log.info("Created " + eventProcessor.getClass().getSimpleName() + " event processor");

        log.debug("Load the persistency provider for storing meta-data on message units");
        String persistencyProviderClassname = instanceConfiguration.getPersistencyProviderClass();
        if (Utils.isNullOrEmpty(persistencyProviderClassname))
            persistencyProviderClassname = "org.holodeckb2b.persistency.DefaultProvider";

        IPersistencyProvider persistencyProvider = null;
        try {
           persistencyProvider = (IPersistencyProvider) Class.forName(persistencyProviderClassname).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
           log.fatal("Could not load the persistency provider: " + persistencyProviderClassname);
           throw new AxisFault("Unable to load required persistency provider!");
        }
        log.info("Using " + persistencyProvider.getName() + " as persistency provider");
        try {
             persistencyProvider.init(instanceConfiguration.getHolodeckB2BHome());
             daoFactory = persistencyProvider.getDAOFactory();
        } catch (PersistenceException initializationFailure) {
            log.fatal("Could not initialize the persistency provider " + persistencyProvider.getName()
                      + "! Unable to start Holodeck B2B. \n\tError details: " + initializationFailure.getMessage());
            throw new AxisFault("Unable to initialize required persistency provider!");
        }
        log.info("Succesfully loaded " + persistencyProvider.getName() + " as persistency provider");

        log.trace("Load the security provider for processing of the message level security");
        String securityProviderClassname = instanceConfiguration.getSecurityProviderClass();
        if (Utils.isNullOrEmpty(securityProviderClassname))
            securityProviderClassname = "org.holodeckb2b.security.DefaultProvider";
        try {
           securityProvider = (ISecurityProvider) Class.forName(securityProviderClassname).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
           log.fatal("Could not load the security provider: " + securityProviderClassname);
           throw new AxisFault("Unable to load required security provider!");
        }
        log.debug("Using security provider: " + securityProvider.getName());
        try {
             securityProvider.init(instanceConfiguration.getHolodeckB2BHome());
        } catch (SecurityProcessingException initializationFailure) {
            log.fatal("Could not initialize the security provider " + securityProvider.getName()
                      + "! Unable to start Holodeck B2B. \n\tError details: " + initializationFailure.getMessage());
            throw new AxisFault("Unable to initialize required security provider!");
        }
        log.info("Succesfully loaded " + securityProvider.getName() + " as security provider");

        log.trace("Create list of available message delivery methods");
        msgDeliveryFactories = new HashMap<>();
        log.trace("Create list of globally configured event handlers");
        eventConfigurations = new ArrayList<>();
        
        // From this point on other components can be started which need access to the Core
        log.debug("Make Core available to outside world");
        HolodeckB2BCore.setImplementation(this);

        log.trace("Initialize worker pool");
        final IWorkerPoolConfiguration poolCfg =
                                        XMLWorkerPoolConfig.loadFromFile(instanceConfiguration.getWorkerPoolCfgFile());
        if (poolCfg != null) {
            workers = new WorkerPool(poolCfg);
            log.info("Started the worker pool");
        } else {
            // As the workers are needed for correct functioning of Holodeck B2B, failure to either
            // load the configuration or start the pool is fatal.
            log.fatal("Could not load workers from file " + instanceConfiguration.getWorkerPoolCfgFile());
            throw new AxisFault("Unable to start Holodeck B2B. Could not load workers from file "
                                + instanceConfiguration.getWorkerPoolCfgFile());
        }


        log.info("Holodeck B2B Core " + VersionInfo.fullVersion + " STARTED.");
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

        // Stop all the workers by shutting down the normal and pull worker pool
        log.trace("Stopping worker pool");
        workers.stop(10);
        log.debug("Worker pool stopped");

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

        IMessageDeliverer   deliverer = null;

        log.trace("Check if there is a factory available for this specification [" + deliverySpec.getId() +"]");
        IMessageDelivererFactory mdf = msgDeliveryFactories.get(deliverySpec.getId());

        if (mdf != null) {
            log.trace("Factory available, get message deliverer");
            try {
                deliverer = mdf.createMessageDeliverer();
            } catch (final MessageDeliveryException mde) {
                log.error("Could not get a message delivered from factory [" + mdf.getClass().getSimpleName()
                            + "] for delivery specification [" + deliverySpec.getId() + "]"
                            + "\n\tError details: " + mde.getMessage());
                throw mde;
            }
        } else {
            try {
                log.trace("No factory available yet for this specification [" + deliverySpec.getId() + "]");
                final String factoryClassName = deliverySpec.getFactory();
                log.debug("Create a factory [" + factoryClassName + "] for delivery specification ["
                        + deliverySpec.getId() + "]");
                mdf = (IMessageDelivererFactory) Class.forName(factoryClassName).newInstance();
                // Initialize the new factory with the settings from the delivery spec
                mdf.init(deliverySpec.getSettings());
                log.debug("Created factory [" + factoryClassName + "] for delivery specification ["
                        + deliverySpec.getId() + "]");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | ClassCastException | MessageDeliveryException ex) {
                // Somehow the factory class failed to load
                log.error("The factory for delivery specification [" + deliverySpec.getId()
                            + "] could not be created! Error details: " + ex.getMessage());
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
        return new StorageManager(daoFactory.getUpdateManager());
    }

    /**
     * {@inheritDoc}
     * @since  3.0.0
     */
    @Override
    public IQueryManager getQueryManager() {
        return daoFactory.getQueryManager();
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
        return securityProvider.getCertificateManager();
    }

    /**
     * Gets the active <i>security provider</i> of this Holodeck B2B instance.
     *
     * @return The active security provider
     * @since 4.0.0
     * @see ISecurityProvider
     */
    public ISecurityProvider getSecurityProvider() {
        return securityProvider;
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
    		log.trace("Replacing existing event handler configuration [id=" + id + "]");
    		eventConfigurations.set(i, eventConfiguration);
    	} else 
    		eventConfigurations.add(eventConfiguration);
    	log.info("Registered event handler configuration [id" + id + "]");
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
    		log.info("Removing event handler configuration [id=" + id + "]");
    	} else
    		log.warn("No event handler configuration registered for id=" + id);
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
     * @since HB2B_NEXT_VERSION
     */
    @Override
	public IVersionInfo getVersion() {
    	return VersionInfo.getInstance();
    }
}
