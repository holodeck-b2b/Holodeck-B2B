/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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


import java.nio.file.Path;
import java.util.Collection;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Represents the set of {@link IPMode}s that configure how Holodeck B2B should process the ebMS messages. The set of
 * P-Modes is therefore an essential component in Holodeck B2B as without P-Modes it will not be possible to exchange
 * any message. The active set can be accessed through the {@link HolodeckB2BCoreInterface#getPModeSet()} method. 
 * <p>There always is just one <code>IPModeSet</code> active within a Holodeck B2B instance. By default a in-memory
 * implementation is used, but a custom implementation can be installed using the Java <i>Service Provide Interface</i>
 * mechanism.<br>
 * Implementations of this interface MUST ensure that at least the operations that modify the set are thread safe.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IPModeSet {
	
    /**
     * Gets the name of this P-Mode set implementation to identify it in logging. This name is only used for logging 
     * purposes and it is recommended to include a version number of the implementation. If no name is specified by the 
     * implementation the class name will be used. 
     *
     * @return  The name of the P-Mode set to use in logging
     * @since 5.0.0
     */
    default String getName() { return this.getClass().getName(); }
    
	/**
	 * Initializes the P-Mode set. 
	 * 
	 * @param hb2bHomeDir	Path of the Holodeck B2B home directory.
	 * @throws PModeSetException When the P-Mode set implementation could not successfully be initialised
	 * @since 4.0.0
	 * @deprecated Implementations should implement {@link #init(IConfiguration)}
	 */
    @Deprecated
	default void init(final Path hb2bHomeDir) throws PModeSetException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Initializes the P-Mode set. 
	 * 
	 * @param config	the Holodeck B2B configuration
	 * @throws PModeSetException When the P-Mode set implementation could not successfully be initialised
     * @since 6.0.0 For backward compatibility a default, <i>NOP</i> implementation is provided 
     */  
	default void init(final IConfiguration config) throws PModeSetException {}
	
    /**
     * Gets the P-Mode with the given <b>PMode.id</b>
     * <p>NOTE: The returned {@link IPMode} instance represents the P-Mode as it is configured <b>at the moment</b> this
     * method is called. Changes that are made to this P-Mode after this call may not be reflected in the retrieved
     * instance.
     *
     * @param id    The <b>PMode.id</b> as a <code>String</code> of the P-Mode to retrieve from the set
     * @return      The {@link IPMode} with the given id if it exists in the set, or<br>
     *              <code>null</code> when there is no P-Mode in the set with the given id.
     */
    IPMode get(String id);

    /**
     * Gets all P-Modes currently in the set.
     * <p>NOTE: The returned set represents the <b>currently</b> contained P-Modes. Changes to the set that are made
     * after this call may not be reflected in the returned set.
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
     * <p>To enable more dynamic P-Mode configurations it is allowed to supply this method a P-Mode without id. It is
     * the responsibility of the implementation to assign a unique id to the new P-Mode.
     *
     * @param pmode The P-Mode to add to the set
     * @return The id of the new P-Mode. This will be equal to {@link IPMode#getId()} when that is not <code>null</code>
     *          or empty, otherwise an id will be assigned by the set.
     * @throws PModeSetException When the P-Mode can not be added to the set, for example because the set already
     *          contains a P-Mode with the same id.
     */
    String add(IPMode pmode) throws PModeSetException;

    /**
     * Replaces the configuration of a P-Mode in the set.
     *
     * @param pmode The new configuration of the P-Mode
     * @throws PModeSetException When the P-Mode can not be replaced by the given P-Mode. If this exception occurs it is
     *          not guaranteed that either the old or new configuration is loaded.
     */
    void replace(IPMode pmode) throws PModeSetException;

    /**
     * Removes the P-Mode with the given id from the set.
     * <p>When this method completes without exception it means that the set does not contain a P-Mode with the given
     * id. So calling this method with an unknown id will results in successful execution.
     *
     * @param id    The <b>PMode.id</b> as a <code>String</code> of the P-Mode to remove from the set
     * @throws PModeSetException When an error occurs while removing the P-Mode from the set. If this exception occurs
     *          it is not guaranteed that the P-Mode is removed.
     */
    void remove(String id) throws PModeSetException;

    /**
     * Removes all P-Modes from the set.
     * <p>NOTE: <b>Take care</b> using this method as it will stop all message processing!
     *
     * @throws PModeSetException When an error occurs while removing all P-Modes from the set.
     */
    void removeAll() throws PModeSetException;
}
