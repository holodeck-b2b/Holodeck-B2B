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

import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for managing the storage of <i>Receipt
 * Signal Message</i> meta-data.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 * @see IReceiptEntity
 */
public interface IReceiptDAO {

    /**
     * Creates a new persistency object to store the meta-data of a <i>Receipt Signal</i> message unit.
     *
     * @param receipt   The meta-data on the Receipt that should be stored in the new persistent object
     * @return          The created persistency object.
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    IReceiptEntity createReceipt(final IReceipt receipt) throws PersistenceException;

    /**
     * Retrieves all information of the Receipt entity object from the storage, including the receipt content (which may
     * be lazily loaded).
     * <p>This method gets the message unit meta-data as it is currently stored and can be called repeatedly, in which
     * case it acts as a refresh operation.
     *
     * @param incompleteEntity  The {@link IReceiptEntity} that should be loaded completely
     * @throws PersistenceException  If an error occurs when loading the meta-data from storage
     */
    void loadCompletely(final IReceiptEntity incompleteEntity) throws PersistenceException;

    /**
     * Gets the persisted meta-data of the <i>User Message</i> message unit that the given <i>Receipt</i> refers to.
     *
     * @param receipt   The entity object representing the Receipt Signal
     * @return The persistent entity object representing the <i>User Message</i> message unit that is acknowledged by
     *         the given Receipt.
     * @throws PersistenceException If an error occurs when retrieving the information from the database.
     */
    IUserMessageEntity getAcknowledgedUserMessage(final IReceiptEntity receipt) throws PersistenceException;

    /**
     * Deletes the meta-data of the given <i>Receipt Signal</i> message unit from the database.
     *
     * @param msgUnit       The {@link IReceiptEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    void deleteReceipt(final IReceiptEntity msgUnit) throws PersistenceException;
}
