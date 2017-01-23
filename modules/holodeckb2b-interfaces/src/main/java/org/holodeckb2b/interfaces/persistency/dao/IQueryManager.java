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
package org.holodeckb2b.interfaces.persistency.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for executing the functions that query
 * stored message unit meta-data, like retrieving all message units in a certain processing state or that are related
 * to another message unit.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public interface IQueryManager {

    /**
     * Retrieves all message units of the specified type and that are in one of the given states and are flowing in the
     * specified direction. If message units are found they are sorted on their timestamp starting with the oldest
     * message units.
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
    <T extends IMessageUnit, V extends IMessageUnitEntity> List<V>
                                                 getMessageUnitsInState(final Class<T> type,
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
    <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(
                                                                                    final Class<T> type,
                                                                                    final Collection<String> pmodeIds,
                                                                                    final ProcessingState state)
                                                                                throws PersistenceException;

    /**
     * Ensures that all meta-data of the given entity object is loaded and available for processing.
     * <p>NOTE: The implementation of this method by the persistency provider may reload the meta-data from the storage
     * and overwrite any changes that were made to the entity object.
     *
     * @param <V>           Limits the <code>messageUnit</code> parameter to only message unit classes
     * @param messageUnit   The entity object that needs to be loaded completely
     * @throws PersistenceException When an error occurs while loading the object
     */
    <V extends IMessageUnitEntity> void ensureCompletelyLoaded(V messageUnit) throws PersistenceException;

    /**
     * Gets the number of times the given <i>User Message</i> message unit has already been sent to the receiver without
     * getting a <i>Receipt</i> back.
     * <p>This equals the number of times the message unit was in the {@link ProcessingState#SENDING} state. Note that
     * this means also failed transmission attempts, i.e. where a transport failure occurred, count for the result.
     *
     * @param userMessage   The user message to get the number of transmissions for
     * @return              The number of times the user message was already sent out
     * @throws PersistenceException If an error occurs when retrieving the number of transmissions
     */
    int getNumberOfTransmissions(final IUserMessageEntity userMessage) throws PersistenceException;

    /**
     * Checks whether there exists a <b>received</b> <i>Message Unit</i> with the given <code>MessageId</code> that has
     * already been delivered, i.e. its <i>current</i> processing state is {@link ProcessingState#DELIVERED}.
     *
     * @param messageId   The <code>MessageId</code> to check delivery for
     * @return            <code>true</code> if there exists a User Message entity with {@link
     *                    IUserMessage#getMessageId()} == <code>messageId</code> and {@link IUserMessage#getDirection()}
     *                    == <code>OUT</code> and {@link IUserMessage#getCurrentProcessingState()} == {@link
     *                    ProcessingStates#DELIVERED},
     *                    <br><code>false</code> otherwise.
     * @throws PersistenceException If an error occurs when executing this query
     */
    boolean isAlreadyDelivered(final String messageId) throws PersistenceException;
}
