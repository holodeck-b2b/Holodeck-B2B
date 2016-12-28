/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.persistency.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for executing generic functions on stored
 * message unit information, like setting the processing state or retrieving a message units with a specific messageId.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public interface IMessageUnitDAO {

    /**
     * Retrieves all message units of the specified type and that are in one of the given states and are flowing in the
     * specified direction.
     * <br><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
     * unit is going to be processed it must be checked if it is loaded completely.
     *
     * @param <T>       Limits the <code>type</code> parameter to only message unit classes
     * @param <V>       The returned objects will be entity objects. V and T will share the same parent type.
     * @param type      The type of message units to retrieve specified by the interface they implement
     * @param direction The direction of the message units to retrieve
     * @param states    Array of processing states that the message units to retrieve should be in
     * @return          A collection of entity objects representing the message units of the specified type that are in
     *                  one of the given states,<br>or <code>null</code> when no such message units are found.
     * @throws PersistenceException When a problem occurs during the retrieval of the message units
     */
    <T extends IMessageUnit, V extends IMessageUnitEntity> Collection<V> getMessageUnitsInState(final Class<T> type,
                                                                        final IMessageUnit.Direction direction,
                                                                        final ProcessingState[] states)
                                                                                        throws PersistenceException;

    /**
     * Retrieves all message units with the given <code>MessageId</code>.
     * <p>Although messageIds should be unique there can exist multiple <code>MessageUnits</code> with the same
     * messageId due to resending (and because other MSH or business applications may not conform to this constraint).
     * <br><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
     * unit is going to be processed it must be checked if it is loaded completely.
     *
     * @param messageId     The messageId of the message units to retrieve
     * @return              The list of received {@link MessageUnit}s with the given message id or,<br>
     *                      <code>null</code> if no received message units with this message if where found
     * @throws PersistenceException If an error occurs when saving the object to the database
     */
    Collection<IMessageUnitEntity> getMessageUnitsWithId(final String messageId) throws PersistenceException;

    /**
     * Retrieves all message units of which the last change in processing state occurred before the given date and time.
     * <br><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
     * unit is going to be processed it must be checked if it is loaded completely.
     *
     * @param   maxLastChangeDate   The latest date of a processing state change that is to be included in the result
     * @return          Collection of entity objects representing the message units which processing state changed at
     *                  latest at the given date
     * @throws PersistenceException If an error occurs while retrieving the message units from the database
     */
    Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(final Date maxLastChangeDate)
                                                                                        throws PersistenceException;

    /**
     * Retrieves all message units of the specified type and that are in the given state and which processing is defined
     * by a P-Mode with one of the given P-Mode ids. The message units are ordered ascending on the timestamp of the
     * current processing state, i.e. the messages that are the longest in the given state are at the front of the
     * list.
     * <br><b>NOTE:</b> The entity objects in the resulting list may not be completely loaded! Before a message unit is
     * going to be processed it must be checked if it is loaded completely.
     *
     * @param <T>       Limits the <code>type</code> parameter to only message unit classes
     * @param <V>       The returned objects will be entity objects. V and T will share the same parent type.
     * @param type      The type of message units to retrieve specified by the interface they implement
     * @param pmodeIds  List of P-Mode ids
     * @param state     The processing state the message units to retrieve should be in
     * @return          The ordered list of entity objects representing the message unit objects of the specified
     *                  type and which are in the specified processing state and have their processing defined by a
     *                  P-Mode with one of the specified ids,
     *                  or<br> <code>null</code> if no such message units where found
     * @throws PersistenceException When an error occurs while executing the query
     */
    <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModeIdsInState(
                                                                                    final Class<T> type,
                                                                                    final Collection<String> pmodeIds,
                                                                                    final ProcessingState state)
                                                                                throws PersistenceException;

    /**
     * Retrieves all message units of the specified type that are responses to the given message id, i.e. their
     * <i>refToMessageId</i> equals the given message id.
     * <br><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
     * unit is going to be processed it must be checked if it is loaded completely.
     *
     * @param <T>           Limits the <code>type</code> parameter to only message unit classes
     * @param <V>           The returned objects will be entity objects. V and T will share the same parent type.
     * @param type          The type of message units to retrieve specified by the interface they implement
     * @param refToMsgId    The message Id of the message the requested message units should be a response to.
     * @return  A list of {@link EntityProxy} objects to the entity objects representing the message units that are
     *          related to the given message unit
     * @throws PersistenceException When an error occurs while executing the query
     */
    <T extends IMessageUnit, V extends IMessageUnitEntity> Collection<V> getResponsesTo(final Class<T> type,
                                                                                        final String refToMsgId)
                                                                                throws PersistenceException;

    /**
     * Persists the ID of the P-Mode that defines how the message unit should be processed.
     *
     * @param msgUnit   The entity object representing the message unit
     * @param pmodeId   The ID of the P-Mode that defines how the message unit must be processed
     * @throws PersistenceException If an error occurs when saving the P-Mode ID to the database
     */
    void setPModeId(final IMessageUnitEntity msgUnit, final String pmodeId) throws PersistenceException;

    /**
     * Updates the processing state of the given message unit to the specified state.
     * <p>This method does not check the current state before updating to the new state. The new processing state's
     * start time will be set to the current time.
     *
     * @param msgUnit       The entity object representing the message unit
     * @param procState     The new processing state
     * @throws PersistenceException When a problem occurs updating the processing state of the message unit
     */
    void setProcessingState(final IMessageUnitEntity msgUnit, final ProcessingState procState)
                                                                                        throws PersistenceException;

    /**
     * Updates the processing state of the given message unit to the specified state.
     * <p>Before changing the processing state this method checks that the message unit is in a specific state. The
     * check and change need to be executed in one transaction to ensure that no other thread can make changes to the
     * message unit's processing state.<br>
     * The new processing state's  start time will be set to the current time.
     *
     * @param msgUnit           The entity object representing the message unit
     * @param currentProcState  The required current processing state of the message unit
     * @param newProcState      The new processing state
     * @return                  <code>true</code> if the processing state was changed,<br>
     *                          <code>false</code> if not because the current processing state has already changed
     * @throws PersistenceException When a problem occurs updating the processing state of the message unit
     */
    boolean setProcessingState(final IMessageUnitEntity msgUnit, final ProcessingState currentProcState
                                                               , final ProcessingState newProcState)
                                                                                        throws PersistenceException;

    /**
     * Sets the multi-hop indicator of the message unit.
     *
     * @param msgUnit       The entity object representing the message unit which multi-hop indicator should be set
     * @param isMultihop    The indicator whether this message unit uses multi-hop
     * @throws PersistenceException When a database error occurs while updating the entity object
     */
    void setMultiHop(final IMessageUnitEntity msgUnit, final boolean isMultihop) throws PersistenceException;

    /**
     * Sets the leg on which the message unit is exchanged.
     *
     * @param msgUnit       The entity object representing the message unit which leg should be set
     * @parem legLabel      The label of the leg on which the message unit is exchanged
     * @throws PersistenceException When a database error occurs while updating the entity object
     */
    void setLeg(final IMessageUnit msgUnit, final ILeg.Label legLabel) throws PersistenceException;
}
