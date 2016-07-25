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

/**
 * Extends the public configuration interface with some settings only to be used by the Holodeck B2B Core itself.
 * <p>An interface is used to make it easier to use a different implementation when testing.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  2.1.0
 */
public interface InternalConfiguration extends IConfiguration {

    /**
     * Gets the Axis2 configuration context. This context is used for processing messages.
     *
     * @return The Axis2 configuration context.
     */
    public ConfigurationContext getAxisConfigurationContext();

    /**
     * Gets the name of the JPA persistency unit to use for accessing the database
     *
     * @return The name of the persistency unit
     */
    public String getPersistencyUnit();

    /**
     * Gets the location of the workerpool configuration file. This an optional configuration parameter and when not
     * specified the default location of worker configuration file is <code>«HB2B_HOME»/conf/workers.xml</code>.
     *
     * @return The absolute path to the worker pool configuration file.
     */
    public String getWorkerPoolCfgFile();

    /**
     * Gets the configured class name of the component that is responsible for the processing of event that are raised
     * during the message processing. This is an optional configuration parameter and when not set the Holodeck B2B Core
     * will use a default implementation.
     *
     * @return String containing the class name of the {@link IMessageProcessingEventProcessor} implementation to use
     */
    public String getMessageProcessingEventProcessor();
}
