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
package org.holodeckb2b.core.config;

import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;

/**
 * Extends the public configuration interface with some settings only to be used by the Holodeck B2B Core itself.
 * <p>An interface is used to make it easier to use a different implementation when testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  2.1.0
 */
public interface InternalConfiguration extends IConfiguration {

    /**
     * Gets the Axis2 configuration context.
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
     * Indicates whether Holodeck B2B Core should not fall back to the default event processor in case the configured
     * event processor fails to load. The default behaviour is to fall back but in certain deployment it may be required
     * to use the configured implementation.   
     *  
     * @return	<code>true</code> if Core should fall back to default implementation,<br>
     * 			<code>false</code> if startup of the gateway should be aborted in case the configured event processor
     * 							   cannot be loaded
     * @since 5.0.0
     */
    boolean eventProcessorFallback();    
    
    /**
     * Indicates whether a P-Mode for which no {@link IPModeValidator} implementation is available to check it before
     * loading it should be rejected or still be loaded.   
     * 
     * @return <code>true</code> if the P-Mode should still be loaded,<br><code>false</code> if it should be rejected
     * @since 5.0.0
     */
    boolean acceptNonValidablePMode();
}
