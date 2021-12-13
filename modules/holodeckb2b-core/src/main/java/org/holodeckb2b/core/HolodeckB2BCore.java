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
package org.holodeckb2b.core;

import org.apache.axis2.AxisFault;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.validation.IValidationExecutor;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Is a <i>facade</i> to {@link HolodeckB2BCoreImpl} to [rovides access to the Holodeck B2B Core components. It is an 
 * extension of the public interface of the Core offered to extensions as defined in the interface module that adds 
 * methods intended for internal use only. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class HolodeckB2BCore extends HolodeckB2BCoreInterface {

    /**
	 * Initialises Holodeck B2B based on the provided configuration.
	 * 
	 * @param config	the configuration to use for this instance
	 * @throws AxisFault	when the Core could not be correctly initialised
	 */
	public static void init(final InternalConfiguration config) throws AxisFault {
		new HolodeckB2BCoreImpl(config);
	}
	
	/**
	 * Shuts down the Holodeck B2B Core.
	 */
	public static void shutdown() {
		 coreImpl().shutdown();
	}	
		
    /**
     * Returns the current configuration of this Holodeck B2B instance. The configuration parameters can be used
     * by extension to integrate their functionality with the core.
     *
     * @return  The current configuration as a {@link IConfiguration} object
     */
    public static InternalConfiguration getConfiguration() {
        return (InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration();
    }

    /**
     * Gets the data access object that should be used to store and update the meta-data on processed message units.
     * <p>The returned data access object is a facade to the one provided by the persistency provider to ensure that
     * changes in the message unit meta-data are managed correctly.
     *
     * @return  The {@link StorageManager} that Core classes should use to update meta-data of message units
     * @since  3.0.0
     */
    public static StorageManager getStorageManager() {
        return coreImpl().getStorageManager();
    }

    /**
     * Gets the {@link IValidationExecutor} implementation that should be used for the execution of the custom
     * message validations.
     *
     * @return  The component responsible for execution of the custom validations.
     * @since 4.0.0
     */
    public static IValidationExecutor getValidationExecutor() {
        return coreImpl().getValidationExecutor();
    }
    
    /** 
     * @return the Core implementation object.	
     * @since 5.3.1
     */
    static HolodeckB2BCoreImpl coreImpl() {
    	return (HolodeckB2BCoreImpl) coreImplementation;
    }
}
