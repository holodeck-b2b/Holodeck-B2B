/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.pmode;


import java.util.Collection;

import org.holodeckb2b.interfaces.config.IConfiguration;

/**
 * Defines the interface for the component that stores the set of P-Modes. This component is used by the Holodeck B2B
 * Core's <code>PModeManager</code> to store the set of P-Modes. There always is just one implementation active within a
 * Holodeck B2B instance. By default a in-memory implementation is used, but a custom implementation can be installed
 * using the Java <i>Service Provide Interface</i> mechanism.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0 This interfaces replaces the old <code>IPModeSet</code> interface which was used for both defining the
 *  			storage component and the external interface of the Core.
 */
public interface IPModeStorage {

 	/**
	 * Initializes the P-Mode storage.
	 *
	 * @param config	the Holodeck B2B configuration
	 * @throws PModeSetException When the P-Mode storage implementation could not successfully be initialised
     */
	void init(final IConfiguration config) throws PModeSetException;

	/**
	 * Shuts down the P-Mode storage.
	 * <p>
	 * This method is called by the Holodeck B2B Core when the instance is shut down. Implementations should use it to
	 * release resources held for storing the P-Modes.
	 */
	void shutdown();

    /**
     * Gets the P-Mode with the given <b>PMode.id</b>
     *
     * @param id    The <b>PMode.id</b> as a <code>String</code> of the P-Mode to retrieve from the set
     * @return      The {@link IPMode} with the given id if it exists in the set, or<br>
     *              <code>null</code> when there is no P-Mode in the set with the given id.
     */
    IPMode get(String id);

    /**
     * Gets all P-Modes currently stored in the set.
     *
     * @return      The complete {@link Collection} of {@link IPMode}s
     */
    Collection<IPMode> getAll();

    /**
     * Determines whether the set contains a P-Mode with the given id.
     *
     * @param id    The id to query as a <code>String</code>
     * @return      <code>true</code> when the set contains a P-Mode with the given id,<br>
     *              <code>false</code> otherwise.
     */
    boolean containsId(String id);

    /**
     * Adds a P-Mode to the set.
     * <p>
     * To enable more dynamic P-Mode configurations it is allowed to supply this method a P-Mode without id. It is the
     * responsibility of the implementation to assign a unique id to the new P-Mode.
     *
     * @param pmode The P-Mode to add to the set
     * @return The id of the new P-Mode. This will be equal to {@link IPMode#getId()} when that is not <code>null</code>
     *         and not empty, otherwise an id will be assigned by the set.
     * @throws PModeSetException When the P-Mode can not be added to the set, for example because the set already
     *         					 contains a P-Mode with the same id.
     */
    String add(IPMode pmode) throws PModeSetException;

    /**
     * Replaces the configuration of a P-Mode.
     * <p>
     * Note that this method is only used by the Holodeck B2B Core when it needs to replace an existing P-Mode. The
     * implementation therefore must ensure that the current set of P-Modes contains a P-Mode with the same id as the
     * provided P-Mode.
     *
     * @param pmode The new configuration of the P-Mode
     * @throws PModeSetException When the P-Mode can not be replaced by the given P-Mode, for example because the set of
     * 							 of stored P-Modes does not contain a P-Mode with the same id.
     * 							 The implementation must guarantee that the old configuration, if one exists, is still
     * 							 loaded when this exception is thrown.
     */
    void replace(IPMode pmode) throws PModeSetException;

    /**
     * Removes the P-Mode with the given id.
     * <p>
     * When this method completes without exception it means that the set does not contain a P-Mode with the given id.
     * So calling this method with an unknown id should result in successful execution.
     *
     * @param id    The <b>PMode.id</b> as a <code>String</code> of the P-Mode to remove
     * @throws PModeSetException When an error occurs while removing the P-Mode. If this exception occurs it is not
     * 							 guaranteed that the P-Mode is removed.
     */
    void remove(String id) throws PModeSetException;

    /**
     * Removes all P-Modes.
     *
     * @throws PModeSetException When an error occurs while removing all P-Modes
     */
    void removeAll() throws PModeSetException;
}
