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
package org.holodeckb2b.module;

import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Provides access to the Holodeck B2B Core of a running instance to the Holodeck B2B Core classes. It is an extension
 * of the public interface the Core offers to extensions and adds methods that are intended for internal use only. Note
 * that this is just a <i>facade</i> to the actual Core implementation that is still one object.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 */
public class HolodeckB2BCore extends HolodeckB2BCoreInterface {

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
     * @since 2.2
     */
    public static StorageManager getStoreManager() {
        return ((HolodeckB2BCoreImpl) coreImplementation).getStorageManager();
    }
}
