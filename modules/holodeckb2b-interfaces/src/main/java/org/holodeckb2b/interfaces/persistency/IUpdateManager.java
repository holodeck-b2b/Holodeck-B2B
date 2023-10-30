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
package org.holodeckb2b.interfaces.persistency;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPayloadEntity;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;

/**
 * Defines the interface for the data management component of the <i>Persistency Provider</i> that is responsible for 
 * executing the functions that modify the persisted message unit meta-data.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.<br>
 * Also implementations must take into account the "<i>completely loaded</i>" state of the message unit entity when
 * updates are performed, i.e. when all information of the updated message unit entity was loaded before the update it
 * should still be loaded after performing the update.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @since  7.0.0  The methods for updating specific meta-data attributes have been replaced by more generic create, 
 * 				  update and delete methods. 
 */
public interface IUpdateManager {

    /**
     * Stores the meta-data of the given message unit in database and returns a new entity object representing the saved
     * message unit. The new entity object must have been assigned a unique <i>CoreId</i>.
     *
     * @param <T>           Limits the <code>messageUnit</code> to message units types
     * @param <E>           Only entity objects will be returned, V and T will be of the same message type
     * @param messageUnit   The meta-data on message unit that should be stored in the new persistent object
     * @return              The created persistency object.
     * @throws DuplicateMessageIdException When the MessageId of the <b>outgoing</b> message unit already exists.  
     * @throws PersistenceException   If an error occurs when saving the new message unit to the database
     */
    <T extends IMessageUnit, E extends IMessageUnitEntity> E storeMessageUnit(final T messageUnit)
    														throws DuplicateMessageIdException, PersistenceException;
    
    /**
     * Saves the updated meta-data of the message unit to the database. If the database already contains newer data the
     * update must be rejected and the entity object should be "refreshed" with the current data.
     * <p>
     * NOTE 1: The meta-data that can be updated is limited to the data for which update methods are specified in the 
     * entity interfaces. Implementation may use this knowledge to optimise the database updates.<br/>
     * NOTE 2: This method should not update the meta-data on paylaods contained in a User Message. For updates to 
     * payload meta-data the Holodeck B2B Core will use {@link #updatePayload(IPayloadEntity)}.  
     *
     * @param messageUnit   Entity object containing the updated data that should be saved to storage
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								entity object is updated to the latest meta-data available.
     * @throws PersistenceException   If some other error occured when saving the updated message unit to the database
     */
    void updateMessageUnit(final IMessageUnitEntity messageUnit) throws AlreadyChangedException, PersistenceException;

    /**
     * Saves the updated meta-data of the payload to the database. If the database already contains newer data the
     * update must be rejected.
     * 
     * @param payload	Entity object containing the updated data that should be saved to storage
     * @return          Updated entity object
     * @throws PersistenceException	If an error occurs when saving the updated payload data to the database, for example 
     * 								when the database contains more up to date meta-data.
     */
    void updatePayload(IPayloadEntity payload) throws PersistenceException;
    
    /**
     * Deletes the meta-data of the given message unit from the database.
     *
     * @param messageUnit       The {@link IMessageUnitEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    void deleteMessageUnit(IMessageUnitEntity messageUnit) throws PersistenceException;
}
