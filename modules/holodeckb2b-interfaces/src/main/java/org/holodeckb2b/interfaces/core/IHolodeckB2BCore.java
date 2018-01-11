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

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

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
    void setPullWorkerPoolConfiguration(IWorkerPoolConfiguration pullConfiguration) throws TaskConfigurationException;

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
     * @since HB2B_NEXT_VERSION
     */
    ICertificateManager getCertificateManager();
}
