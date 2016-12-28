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
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Provides access to the Holodeck B2B Core of a running instance. Note that this is just a <i>facade</i> to the actual
 * Core implementation to provide a clean separation of interface and implementation. Although this class is more
 * interface than class it needs to be a class to get an object instance on run time.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class HolodeckB2BCoreInterface {

    /**
     * The Holodeck B2B Core implementation
     */
    private static IHolodeckB2BCore     coreImplementation;

    /**
     * @return <code>true</code> if this class is initialized, <code>false</code> otherwise.
     */
    public static boolean isInitialized () {
        return coreImplementation != null;
    }

    /**
     * Returns the current configuration of this Holodeck B2B instance. The configuration parameters can be used
     * by extension to integrate their functionality with the core.
     *
     * @return  The current configuration as a {@link IConfiguration} object
     */
    public static IConfiguration getConfiguration() {
        assertInitialized();
        return coreImplementation.getConfiguration();
    }

    /**
     * Gets a {@link IMessageDeliverer} object configured as specified by the {@link IDeliverySpecification} that can be
     * used to deliver message units to the <i>Consumer</i> business application.
     *
     * @param deliverySpec      Specification of the delivery method for which a deliver must be returned.
     * @return                  A {@link IMessageDeliverer} object for the given delivery specification
     * @throws MessageDeliveryException When no delivery specification is given or when the message deliverer can not
     *                                  be created
     */
    public static IMessageDeliverer getMessageDeliverer(final IDeliverySpecification deliverySpec)
                                                        throws MessageDeliveryException {
        assertInitialized();
        return coreImplementation.getMessageDeliverer(deliverySpec);
    }

    /**
     * Gets a {@link IMessageSubmitter} object that can be used by the <i>Producer</i> business application for
     * submitting User Messages to the Holodeck B2B Core.
     *
     * @return  A {@link IMessageSubmitter} object to use for submission of User Messages
     */
    public static IMessageSubmitter getMessageSubmitter() {
        assertInitialized();
        return coreImplementation.getMessageSubmitter();
    }

    /**
     * Gets the set of currently configured P-Modes.
     * <p>The P-Modes define how Holodeck B2B should process the messages. The set of P-Modes is therefor the most
     * important configuration item in Holodeck B2B, without P-Modes it will not be possible to send and receive
     * messages.
     *
     * @return  The current set of P-Modes as a {@link IPModeSet}
     * @see IPModeSet
     */
    public static IPModeSet getPModeSet() {
        assertInitialized();
        return coreImplementation.getPModeSet();
    }

    /**
     * Gets the core component that is responsible for processing <i>"events"</i> that are raised while processing a
     * message unit. Such <i>"message processing events"</i> may need to be send to the business (or other external)
     * application to keep them updated. The {@link IMessageProcessingEventProcessor} will manage the notifications to
     * the external applications based on the configuration provided in the P-Mode.
     *
     * @return  The {@link IMessageProcessingEventProcessor} managing the event processing
     * @since 2.1.0
     */
    public static IMessageProcessingEventProcessor getEventProcessor() {
        assertInitialized();
        return coreImplementation.getEventProcessor();
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
    public static void setPullWorkerPoolConfiguration(final IWorkerPoolConfiguration pullConfiguration)
                                                                                    throws TaskConfigurationException {
        assertInitialized();
        coreImplementation.setPullWorkerPoolConfiguration(pullConfiguration);
    }

   /**
     * Sets the Holodeck B2B Core implementation that is in use.
     * <p><b>NOTE: </b>This method is for <b>internal use only</b>!
     *
     * @param impl  The current Holodeck B2B Core implementation
     */
    public static synchronized void setImplementation(final IHolodeckB2BCore impl) {
        coreImplementation = impl;
    }

    /**
     * Ensures that the Holodeck B2B is loaded and available
     */
    private static void assertInitialized() {
        if (coreImplementation == null)
            throw new IllegalStateException("Holodeck B2B is not [correctly] started yet");
    }
}
