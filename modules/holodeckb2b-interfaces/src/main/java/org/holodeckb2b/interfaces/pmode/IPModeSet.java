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


import java.util.Collection;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.InvalidPModeException;

/**
 * Represents the set of {@link IPMode}s that configure how Holodeck B2B should process the ebMS messages. The set of
 * P-Modes is therefore an essential component in Holodeck B2B as without P-Modes it will not be possible to exchange
 * any message. The active set can be accessed through the {@link HolodeckB2BCoreInterface#getPModeSet()} method.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0 this interface only represents the external interface exposed by the Holodeck B2B Core to access the
 * 				P-Modes
 */
public interface IPModeSet {

    /**
     * Gets the P-Mode with the given <b>PMode.id</b>
     * <p>
     * NOTE: The returned {@link IPMode} instance represents the P-Mode as it is configured <b>at the moment</b> this
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
     * <p>
     * NOTE: The returned set represents the <b>currently</b> contained P-Modes. Changes to the set that are made after
     * this call may not be reflected in the returned set.
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
     * Validates and then adds a P-Mode to the set.
     * <p>
     * To enable more dynamic P-Mode configurations it is allowed to supply this method a P-Mode without id. An id will
     * be assigned when the P-Mode is added to the set.
     * <p>
     * NOTE: The validation of the P-Mode is delegated to the {@link IPModeValidator}s which are dynamically loaded. It
     * therefore depends on the instance's configuration what is checked and whether the P-Mode is considered to be
     * valid.
     *
     * @param pmode P-Mode to be added
     * @return The id of the new P-Mode. This will be equal to {@link IPMode#getId()} when that is not <code>null</code>
     *          or empty, otherwise an id will be assigned by the set.
     * @throws InvalidPModeException When the P-Mode is invalid
     * @throws PModeSetException When the P-Mode can not be added to the set, for example because the set already
     *          contains a P-Mode with the same id.
     * @see IPModeValidator
     */
    String add(IPMode pmode) throws InvalidPModeException, PModeSetException;

    /**
     * Validates and replaces the configuration of a P-Mode in the set.
     * <p>
     * NOTE: The validation of the P-Mode is delegated to the {@link IPModeValidator}s which are dynamically loaded. It
     * therefore depends on the instance's configuration what is checked and whether the P-Mode is considered to be
     * valid.
     *
     * @param pmode The new configuration of the P-Mode
     * @throws InvalidPModeException When the P-Mode is invalid
     * @throws PModeSetException When the P-Mode can not be replaced by the given P-Mode. If this exception occurs it is
     *          not guaranteed that either the old or new configuration is loaded.
     */
    void replace(IPMode pmode) throws InvalidPModeException, PModeSetException;

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

    /**
     * Registers a event listener to receive {@link PModeSetEvent}s when a change occurs in the P-Mode set. The listener
     * can choose to limit the type of changes for which event are reported.
     *
     * @param listener	the event listener that should be informed on a change in the P-Mode set
     * @param actions	the type of changes for which the listener should be informed. If none are given, the listener
     * 					will be informed on any change
     * @since 8.0.0
     */
    void registerEventListener(IPModeSetListener listener, PModeSetEvent.PModeSetAction... actions);

	/**
	 * Unregisters a event listener from receiving {@link PModeSetEvent}s when a change occurs in the P-Mode set. The
	 * listener can be removed for specific changes in the P-Mode set.
	 *
	 * @param listener	the event listener which registration should be removed
	 * @param actions	the type of changes for which the registration should be removed. If none are given, the
	 * 					listener will be completely removed and no more events will be reported to it.
	 * @since 8.0.0
	 */
	void unregisterEventListener(IPModeSetListener listener, PModeSetEvent.PModeSetAction... actions);
}
