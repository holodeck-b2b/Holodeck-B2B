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

import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for managing the storage of <i>Error
 * Signal Message</i> meta-data.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 * @see IErrorMessageEntity
 */
public interface IErrorMessageDAO {

    /**
     * Creates a new persistency object to store the meta-data of an <i>Error Signal</i> message unit.
     *
     * @param error   The meta-data on the Error that should be stored in the new persistent object
     * @return        The created persistency object.
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    IErrorMessageEntity createErrorMessage(final IErrorMessage error) throws PersistenceException;

    /**
     * Retrieves all information of the Error entity object from the storage, including the detailed error information
     * (which may be lazily loaded).
     * <p>This method gets the message unit meta-data as it is currently stored and can be called repeatedly, in which
     * case it acts as a refresh operation.
     *
     * @param incompleteEntity  The {@link IErrorMessageEntity} that should be loaded completely
     * @throws PersistenceException  If an error occurs when loading the meta-data from storage
     */
    void loadCompletely(final IErrorMessageEntity incompleteEntity) throws PersistenceException;

    /**
     * Gets the persisted meta-data of the message unit that the given <i>Error</i> refers to.
     *
     * @param error   The entity object representing the Error Signal
     * @return The persistent entity object representing the message unit that is acknowledged by the given Error.
     * @throws PersistenceException If an error occurs when retrieving the information from the database.
     */
    IMessageUnitEntity getMessageUnitInError(final IErrorMessageEntity error) throws PersistenceException;

    /**
     * Deletes the meta-data of the given <i>Error Signal</i> message unit from the database.
     *
     * @param msgUnit       The {@link IErrorMessageEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    void deleteErrorMessage(final IErrorMessageEntity msgUnit) throws PersistenceException;
}
