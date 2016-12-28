/*
 * Copyright (C) 2016 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.persistency.dao;

import java.util.Collection;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for managing the storage of <i>User
 * Message</i> meta-data.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 * @see IUserMessageEntity
 */
public interface IUserMessageDAO {

    /**
     * Creates a new persistency object to store the meta-data of a <i>User Message</i> message unit.
     *
     * @param userMessage   The meta-data on the User Message that should be stored in the new persistent object
     * @return              The created persistency object.
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    IUserMessageEntity createUserMessage(final IUserMessage userMessage) throws PersistenceException;

    /**
     * Updates the information on the payloads included in the given User Message.
     *
     * @param userMessage   The entity object representing the User Message
     * @param payloadInfo   The meta-data on the payloads included with the User Message which must be persisted
     * @throws PersistenceException     If an error occurs when saving the payload meta-data to the database
     */
    void updatePayloadInformation(final IUserMessageEntity userMessage, final Collection<IPayload> payloadInfo)
                                    throws PersistenceException;

    /**
     * Retrieves all information of the User Message entity object from the storage, including the information on
     * sender, receiver, payloads and message properties (which may be lazily loaded).
     * <p>This method gets the message unit meta-data as it is currently stored and can be called repeatedly, in which
     * case it acts as a refresh operation.
     *
     * @param incompleteEntity  The {@link IUserMessageEntity} that should be loaded completely
     * @throws PersistenceException  If an error occurs when loading the meta-data from storage
     */
    void loadCompletely(final IUserMessageEntity incompleteEntity) throws PersistenceException;

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

    /**
     * Deletes the meta-data of the given <i>User Message</i> message unit from the database.
     *
     * @param msgUnit       The {@link IUserMessageEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    void deleteUserMessage(final IUserMessageEntity msgUnit) throws PersistenceException;
}
