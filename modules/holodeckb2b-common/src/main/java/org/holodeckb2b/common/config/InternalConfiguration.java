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
package org.holodeckb2b.common.config;

import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.security.ISecurityProvider;

/**
 * Extends the public configuration interface with some settings only to be used by the Holodeck B2B Core itself.
 * <p>An interface is used to make it easier to use a different implementation when testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  2.1.0
 */
public interface InternalConfiguration extends IConfiguration {

    /**
     * Gets the Axis2 configuration context. This context is used for processing messages.
     *
     * @return The Axis2 configuration context.
     */
    ConfigurationContext getAxisConfigurationContext();

    /**
     * Gets the location of the workerpool configuration file. This an optional configuration parameter and when not
     * specified the default location of worker configuration file is <code>«HB2B_HOME»/conf/workers.xml</code>.
     *
     * @return The absolute path to the worker pool configuration file.
     */
    String getWorkerPoolCfgFile();

    /**
     * Gets the configured class name of the component that is responsible for the processing of event that are raised
     * during the message processing. This is an optional configuration parameter and when not set the Holodeck B2B Core
     * will use a default implementation.
     *
     * @return String containing the class name of the {@link IMessageProcessingEventProcessor} implementation to use
     */
    String getMessageProcessingEventProcessor();

    /**
     * Gets the class name of the {@link IPModeValidator} implementation that the Holodeck B2B Core's <code>PModeManager
     * </code> must use to validate P-Modes before they are deployed.
     *
     * @return  The class name of the {@link IPModeValidator} implementation
     * @since  3.0.0
     */
    String getPModeValidatorImplClass();

    /**
     * Gets the class name of the {@link IPModeSet} implementation that the Holodeck B2B Core's <code>PModeManager
     * </code> must use to store the set of deployed P-Modes.
     *
     * @return  The class name of the {@link IPModeSet} implementation to use for storing deployed P-Modes
     * @since  3.0.0
     */
    String getPModeStorageImplClass();

    /**
     * Gets the class name of the {@link IPersistencyProvider} implementation that the Holodeck B2B Core should use to
     * store meta-data of processed message units. This is an optional configuration parameter and when not set the
     * Holodeck B2B Core will use the default implementation.
     *
     * @return  The class name of the {@link IPersistencyProvider} implementation to use for storing meta-data
     * @since  3.0.0
     */
    String getPersistencyProviderClass();

    /**
     * Gets the class name of the {@link ISecurityProvider} implementation that the Holodeck B2B Core should use to
     * process the WS-Security headers in the messages. This is an optional configuration parameter and when not set the
     * Holodeck B2B Core will use the default implementation.
     *
     * @return The class name of the {@link ISecurityProvider} implementation to use for handling the WS-Security
     *         headers in messages
     * @since HB2B_NEXT_VERSION
     */
    String getSecurityProviderClass();
}
