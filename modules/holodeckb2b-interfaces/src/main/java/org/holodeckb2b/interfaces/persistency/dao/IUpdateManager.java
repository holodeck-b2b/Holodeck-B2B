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
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for executing the functions that write
 * message unit meta-data to storage, like creating the stored object and setting the processing state.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.<br>
 * Also implementations must take into account the "<i>completely loaded</i>" state of the message unit entity when
 * updates are performed, i.e. when all information of the updated message unit entity was loaded before the update it
 * should still be loaded after performing the update.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface IUpdateManager {

    /**
     * Creates a new persistency object to store the meta-data of the given message unit.
     *
     * @param <T>           Limits the <code>messageUnit</code> to message units types
     * @param <V>           Only entity objects will be returned, V and T will be of the same message type
     * @param messageUnit   The meta-data on message unit that should be stored in the new persistent object
     * @return              The created persistency object.
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    <T extends IMessageUnit, V extends IMessageUnitEntity> V storeMessageUnit(final T messageUnit)
                                                                                            throws PersistenceException;

    /**
     * Sets the ID of the P-Mode that defines how the message unit should be processed.
     *
     * @param msgUnit   The entity object representing the message unit
     * @param pmodeId   The ID of the P-Mode that defines how the message unit must be processed
     * @throws PersistenceException If an error occurs when saving the P-Mode ID to the database
     */
    void setPModeId(final IMessageUnitEntity msgUnit, final String pmodeId) throws PersistenceException;

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

    /**
     * Sets the information on the payloads included in the given User Message.
     * <p>Note that this is really a <b>setter</b> and overwrites any current payload meta-data.
     *
     * @param userMessage   The entity object representing the User Message
     * @param payloadInfo   The meta-data on the payloads included with the User Message which must be persisted
     * @throws PersistenceException     If an error occurs when saving the payload meta-data to the database
     */
    void setPayloadInformation(final IUserMessageEntity userMessage, final Collection<IPayload> payloadInfo)
                                                                                        throws PersistenceException;

    /**
     * Sets the indicator on an error message whether a SOAP Fault should be added to the message when sending the
     * error message.
     * <p>Note that the decision whether the SOAP Fault will be added to the message is taken when the actual message
     * is constructed and depends also on the other message units that are included in the same message.
     *
     * @param errorMessage      The entity object representing the Error Message
     * @param addSOAPFault      The indicator whether to add a SOAP fault.
     * @throws PersistenceException  If an error occurs when updating the indicator
     */
    void setAddSOAPFault(final IErrorMessageEntity errorMessage, final boolean addSOAPFault)
                                                                                        throws PersistenceException;

    /**
     * Deletes the meta-data of the given message unit from the database.
     *
     * @param messageUnit       The {@link IMessageUnitEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    void deleteMessageUnit(IMessageUnitEntity messageUnit) throws PersistenceException;
}
