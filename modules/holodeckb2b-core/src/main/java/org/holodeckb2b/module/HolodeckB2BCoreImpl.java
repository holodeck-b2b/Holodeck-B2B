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
package org.holodeckb2b.module;

import java.util.HashMap;
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
import org.holodeckb2b.common.config.Config;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.WorkerPool;
import org.holodeckb2b.common.workerpool.xml.XMLWorkerPoolConfig;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;
import org.holodeckb2b.ebms3.pulling.PullConfiguration;
import org.holodeckb2b.ebms3.pulling.PullConfigurationWatcher;
import org.holodeckb2b.ebms3.pulling.PullWorker;
import org.holodeckb2b.ebms3.submit.core.MessageSubmitter;
import org.holodeckb2b.events.SyncEventProcessor;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.pmode.PModeManager;

/**
 * Axis2 module class for the Holodeck B2B Core module.
 * <p>This class is responsible for the initialization and shutdown of the ebMS module. This includes
 * starting and stopping the workers needed to drive the message exchanges.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
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
    private static Log log = LogFactory.getLog(HolodeckB2BCoreImpl.class);

    /**
     * The configuration of this Holodeck B2B instance
     */
    private static  InternalConfiguration  instanceConfiguration = null;

    /**
     * Pool of worker threads that handle recurring tasks like message sending and
     * resending.
     */
    private static  WorkerPool      workers = null;

    /**
     * Pool of worker threads that handle the sending of <i>pull request</i>. The workers in this pool are {@link PullWorker}
     * objects. The pool is configured by the {@link PullConfigurationWatcher} that runs in the normal worker pool and
     * checks the pull configuration for changes and applies them to this pull worker pool.
     */
    private static  WorkerPool      pullWorkers = null;

    /**
     * Collection of active message delivery methods mapped by the <i>id</i> of the {@link IDeliverySpecification} that
     * defined their configuration.
     * <p>For each unique delivery specification id Holodeck B2B will create factory class that creates the actual
     * {@link IMessageDeliverer} objects that are used to deliver messages to the business application.
     */
    private static Map<String, IMessageDelivererFactory>    msgDeliveryFactories = null;

    /**
     * The P-Mode manager that maintains the set of deployed P-Modes
     */
    private static PModeManager pmodeManager = null;

    /**
     * The component responsible for processing of events that occur while processing a message. The processor will
     * pass the events on to the configured event handlers.     *
     * @since 2.1.0
     */
    private static IMessageProcessingEventProcessor eventProcessor = null;

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

        log.debug("Initialize the P-Mode manager");
        HolodeckB2BCoreImpl.pmodeManager = new PModeManager(instanceConfiguration.getPModeValidatorImplClass(),
                                                            instanceConfiguration.getPModeStorageImplClass());

        log.debug("Create the processor for message processing events");
        final String eventProcessorClassname = instanceConfiguration.getMessageProcessingEventProcessor();
        if (!Utils.isNullOrEmpty(eventProcessorClassname)) {
            try {
               eventProcessor = (IMessageProcessingEventProcessor) Class.forName(eventProcessorClassname).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
               // Could not create the specified event processor, fall back to default implementation
               log.error("Could not load the specified event processor: " + eventProcessorClassname
                        + ". Using default implementation instead.");
            }
        } else
            eventProcessor = new SyncEventProcessor();
        log.debug("Created " + eventProcessor.getClass().getSimpleName() + " event processor");

        // From this point on external components can be started which need access to the Core
        log.debug("Make Core available to outside world");
        HolodeckB2BCoreInterface.setImplementation(this);

                // Special ClassLoader required for correct Hibernate init!
        {
          final ClassLoader aOldCL = Thread.currentThread ().getContextClassLoader ();
          Thread.currentThread ().setContextClassLoader (JPAUtil.class.getClassLoader ());
          try
          {
            JPAUtil.getEntityManager ();
          }
          catch (final Exception ex)
          {}
          finally
          {
            Thread.currentThread ().setContextClassLoader (aOldCL);
          }
        }

        log.debug("Initialize worker pool");
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

        log.debug("Create the pull worker pool");
        // The pull worker pool is only created here, initialization is done by the worker part of the normal
        //  worker pool
        pullWorkers = new WorkerPool(PullConfiguration.PULL_WORKER_POOL_NAME);
        log.debug("Pull worker pool created");

        log.debug("Create list of available message delivery methods");
        msgDeliveryFactories = new HashMap<>();

        log.info("Holodeck B2B Core module STARTED.");
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
        log.debug("Stopping worker pool");
        workers.stop(10);
        log.debug("Worker pool stopped");
        log.debug("Stopping pull worker pool");
        pullWorkers.stop(10);
        log.debug("Pull worker pool stopped");


        log.info("Holodeck B2B Core module STOPPED.");
    }

    /**
     * Returns the current configuration of this Holodeck B2B instance. The configuration parameters can be used
     * by extension to integrate their functionality with the core.
     *
     * @return  The current configuration as a {@link IConfiguration} object
     */
    public IConfiguration getConfiguration() {
        if (instanceConfiguration == null) {
            log.fatal("Missing configuration for this Holodeck B2B instance!");
            throw new IllegalStateException("Missing configuration for this Holodeck B2B instance!");
        } else
            return instanceConfiguration;
    }

    /**
     * Gets a {@link IMessageSubmitter} object that can be used by the <i>Producer</i> business application for
     * submitting User Messages to the Holodeck B2B Core.
     *
     * @return  A {@link IMessageSubmitter} object to use for submission of User Messages
     */
    public IMessageSubmitter getMessageSubmitter() {
        return SubmitterSingletonHolder.instance;
    }

    /**
     * Gets a {@link IMessageDeliverer} object configured as specified that can be used to deliver message units to the
     * business application.
     *
     * @param deliverySpec      Specification of the delivery method for which a deliver must be returned.
     * @return                  A {@link IMessageDeliverer} object for the given delivery specification
     * @throws MessageDeliveryException When no delivery specification is given or when the message deliverer can not
     *                                  be created
     */
    public IMessageDeliverer getMessageDeliverer(final IDeliverySpecification deliverySpec)
                                                        throws MessageDeliveryException {
        if (deliverySpec == null) {
            log.error("No delivery specification given!");
            throw new MessageDeliveryException("No delivery specification given!");
        }

        IMessageDeliverer   deliverer = null;

        log.debug("Check if there is a factory available for this specification [" + deliverySpec.getId() +"]");
        IMessageDelivererFactory mdf = msgDeliveryFactories.get(deliverySpec.getId());

        if (mdf != null) {
            log.debug("Factory available, get message deliverer");
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
                log.debug("No factory available yet for this specification [" + deliverySpec.getId() + "]");
                final String factoryClassName = deliverySpec.getFactory();
                log.debug("Create a factory [" + factoryClassName + "] for delivery specification ["
                        + deliverySpec.getId() + "]");
                mdf = (IMessageDelivererFactory) Class.forName(factoryClassName).newInstance();
                // Initialize the new factory with the settings from the delivery spec
                mdf.init(deliverySpec.getSettings());
                log.info("Created factory [" + factoryClassName + "] for delivery specification ["
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
            log.debug("Added new factory to list of available delivery methods");
        }

        log.debug("Get and return deliverer from the factory");
        return mdf.createMessageDeliverer();
    }

    /**
     * Gets the set of currently configured P-Modes.
     * <p>The P-Modes define how Holodeck B2B should process the messages. The set of P-Modes is therefor the most
     * important configuration item in Holodeck B2B, without P-Modes it will not be possible to send and receive
     * messages.
     *
     * @return  The current set of P-Modes as a {@link IPModeSet}
     * @see IPMode
     */
    public IPModeSet getPModeSet() {
        return pmodeManager;
    }

    /**
     * Gets the core component that is responsible for processing <i>"events"</i> that are raised while processing a
     * message unit.
     * <p>By default the {@link SyncEventProcessor} implementation is used for processing events, but the processor to
     * use can be configured by setting the <i>"MessageProcessingEventProcessor"</i> parameter in the Holodeck B2B
     * configuration file.
     *
     * @return  The {@link IMessageProcessingEventProcessor} managing the event processing
     * @since 2.1.0
     */
    @Override
    public IMessageProcessingEventProcessor getEventProcessor() {
        return eventProcessor;
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
     * @since 2.1.0
     */
    @Override
    public void setPullWorkerPoolConfiguration(final IWorkerPoolConfiguration pullConfiguration)
                                                                                    throws TaskConfigurationException {
        log.debug("New pull worker configuration provided, reconfiguring Pull Worker Pool");
        if (pullConfiguration == null) {
            log.debug("Configuration is removed, stop worker pool");
            pullWorkers.stop(0);
            log.warn("Pull Worker Pool stopped due to removal of configuration!");
        } else {
            log.debug("New configuration provided, reconfigure the worker pool");
            pullWorkers.setConfiguration(pullConfiguration);
            log.info("Pull configuration succesfully changed");
        }
    }
}
