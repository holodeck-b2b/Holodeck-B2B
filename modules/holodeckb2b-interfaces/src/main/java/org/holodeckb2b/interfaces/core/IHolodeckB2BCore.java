/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.core;

import java.util.List;

import org.apache.axis2.modules.Module;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.general.IVersionInfo;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPool;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;

/**
 * Defines the interface the Holodeck B2B Core implementation has to provide to the outside world, like submitters
 * delivery methods and extensions for dynamic configuration.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IHolodeckB2BCore {

    /**
     * Gets the current configuration of this Holodeck B2B instance. The configuration parameters can be used by
     * extensions to integrate their functionality with the core.
     *
     * @return  The current configuration as a {@link IConfiguration}
     */
    IConfiguration getConfiguration();

    /**
     * Gets the active Axis2 Module with the given name. This can for example be used by protocol extension to get 
     * access to "their" module for protocol specific settings.  
     * 
     * @param name	the requested module's name
     * @return 		the active Axis2 module if it exists in this Holodeck B2B instance,<br><code>null</code> otherwise
     * @since 5.0.0
     */
    Module getModule(final String name);
    
    /**
     * Gets a {@link IMessageDeliverer} object configured as specified by the {@link IDeliverySpecification} that can be
     * used to deliver message units to the <i>Consumer</i> business application.
     *
     * @param deliverySpec      Specification of the delivery method for which a deliver must be returned.
     * @return                  A {@link IMessageDeliverer} object for the given delivery specification
     * @throws MessageDeliveryException When no delivery specification is given or when the message deliverer can not
     *                                  be created
     */
    IMessageDeliverer getMessageDeliverer(IDeliverySpecification deliverySpec) throws MessageDeliveryException;

    /**
     * Gets a {@link IMessageSubmitter} object that can be used by the <i>Producer</i> business application for
     * submitting User Messages to the Holodeck B2B Core.
     *
     * @return  A {@link IMessageSubmitter} object to use for submission of User Messages
     */
    IMessageSubmitter getMessageSubmitter();

    /**
     * Gets the set of currently configured P-Modes.
     * <p>The P-Modes define how Holodeck B2B should process the messages. The set of P-Modes is therefor the most
     * important configuration item in Holodeck B2B, without P-Modes it will not be possible to send and receive
     * messages.
     *
     * @return  The current set of P-Modes as a {@link IPModeSet}
     * @see IPMode
     */
    IPModeSet getPModeSet();

    /**
     * Gets the core component that is responsible for processing <i>"events"</i> that are raised while processing a
     * message unit. Such <i>"message processing events"</i> may need to be send to the business (or other external)
     * application to keep them updated. The {@link IMessageProcessingEventProcessor} will manage the notifications to
     * the external applications based on the configuration provided in the P-Mode.
     *
     * @return  The {@link IMessageProcessingEventProcessor} managing the event processing
     * @since 2.1.0
     */
    IMessageProcessingEventProcessor getEventProcessor();

    /**
     * Gets the data access object that should be used to query the meta-data on processed message units.
     * <p>Note that the DAO itself is provided by the persistency provider.
     *
     * @return  The {@link IQueryManager} that should use to query the meta-data of message units
     * @since  3.0.0
     */
    IQueryManager getQueryManager();

    /**
     * Gets the {@link ICertificateManager} of the active <i>security provider</i>. Using the certificate manager keys
     * and certificates needed for the correct processing of messages can be managed.
     *
     * @return The active certificate manager
     * @since 4.0.0
     */
    ICertificateManager getCertificateManager();
    
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
    boolean registerEventHandler(IMessageProcessingEventConfiguration eventConfiguration) 
    																	throws MessageProccesingEventHandlingException;
    
    /**
     * Removes a <i>global</i> event handler configuration.
     * 
     * @param id	The id of the event handler configuration to remove
     * @since 4.1.0 
     */
    void removeEventHandler(String id);
    
    /**
     * Gets the list of globally configured event handlers. 
     *  
     * @return		The list of event handler configurations 
     * @since 4.1.0
     */
    List<IMessageProcessingEventConfiguration> getEventHandlerConfiguration();
    
    /**
     * Gets information about the version of the Holodeck B2B Core of this instance. 
     *   
     * @return	The version info
     * @since 5.0.0
     */
    IVersionInfo getVersion();
    
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
    IWorkerPool createWorkerPool(final String name, final IWorkerPoolConfiguration configuration) 
																							throws WorkerPoolException;    
    
    /**
     * Gets the worker pool with the given name.
     * 
     * @param name	of the worker pool to retrieve
     * @return		the worker pool with the given name, or <code>null</code> when no such pool exists
     * @since 5.1.0
     */
    IWorkerPool getWorkerPool(final String name);
    
    /**
     * Resumes processing of the <i>suspended</i> User Message.  
     * <p>Note that only outgoing User Messages can be in suspended state and resumed. The resume operation will change 
     * the processing state from <i>SUSPENDED</i> to either <i>READY_TO_PUSH</i> or <i>AWAIT_PULL</i> depending on the 
     * MEP defined in the P-Mode. If the current processing state however has already changed it assumed that the 
     * message has already been resumed and no further action is needed.
     *  
     * @param userMessage	to be resumed 
     * @throws PersistenceException		when an error occurs updating the processing state of the message unit
     * @throws IllegalArgumentException when the given User Message is an incoming User Message 
     * @since 5.3.0
     */
    void resumeProcessing(IUserMessageEntity userMessage) throws PersistenceException, IllegalArgumentException;
}
