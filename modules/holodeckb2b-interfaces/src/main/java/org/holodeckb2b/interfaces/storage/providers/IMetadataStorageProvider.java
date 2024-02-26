/*
 * Copyright (C) 2024 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.storage.providers;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;

/**
 * Defines the interface of a Holodeck B2B <i>Meta-data Storage Provider</i> that allows the Holodeck B2B Core to
 * persist and retrieve the meta-data of processed message units.
 * <p>
 * There can always be just one <i>Meta-data Storage Provider</i> active in an Holodeck B2B instance. The implementation
 * to use is loaded using the Java <i>Service Prover Interface</i> mechanism. Therefore the JAR file containing the
 * <i>Meta-data Storage Provider</i> implementation must contain a file in the <code>META-INF/services/</code> directory
 * named <code>org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider</code> that contains the class name
 * of the provider implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0 This interface is a merge of the <code>IPersistencyProvider</coder>, <code>IUpdateManager</code> and
 * 				<code>IQueryManager</code> interfaces from the previous version.
 */
public interface IMetadataStorageProvider {

    /**
     * Gets the name of this provider to identify it in logging. This name is only used for logging purposes and it is
     * recommended to include a version number of the implementation. If no name is specified by the implementation the
     * class name will be used.
     *
     * @return  The name of the persistency provider.
     */
    default String getName() { return this.getClass().getName(); }

    /**
     * Initialises the provider. This method is called once at startup of the Holodeck B2B instance. Since the message
     * processing depends on the correct functioning of the provider this method MUST ensure that all required
     * configuration and data is available.
     *
     * @param config	the Holodeck B2B configuration
     * @throws StorageException     When the initialization of the provider can not be completed. The exception
     *                                  message SHOULD include a clear indication of what caused the init failure.
     */
	void init(final IConfiguration config) throws StorageException;

	/**
	 * Shuts down the provider.
	 * <p>This method is called by the Holodeck B2B Core when the instance is shut down. Implementations should use it
	 * to release resources held for storing the message meta-data.
	 */
	void shutdown();

	/*----------------------------------------------------------------------------------------------------------------
	 * CRUD methods to manage message unit meta-data
	 *--------------------------------------------------------------------------------------------------------------*/

	/**
	 * Stores the meta-data of the given message unit in the database and returns a new entity object representing the
	 * saved message unit. The new entity object MUST have been assigned a unique <i>CoreId</i>.
	 * <p>
	 * NOTE: When the message unit to be stored is a {@link IUserMessage} the provider must check whether the payloads
	 * included are {@link IPayloadEntity} instances, and if so, couple the existing payloads with the message unit. If
	 * the <i>PayloadId</i> do not exist or are already linked to another User Message, the provider MUST throw an
	 * exception.<br/>
	 * If the payloads are not {@link IPayloadEntity} instances the provider MUST create new {@link IPayloadEntity}
	 * objects and assign a unique <i>PayloadId</i> to them. The assigned <i>PayloadId</i> MUST NOT contain special
	 * characters.
	 *
	 * @param <T>           Limits the <code>messageUnit</code> to message units types
	 * @param <E>           Only entity objects will be returned, V and T will be of the same message type
	 * @param messageUnit   The meta-data on message unit that should be stored in the new persistent object
	 * @return              The created entity object.
	 * @throws DuplicateMessageIdException When the MessageId of the <b>outgoing</b> message unit already exists.
	 * @throws StorageException   If an error occurs when saving the new message unit to the database. This can happen
	 * 							  when the given message unit is a User Message that contains a {@link IPayloadEntity}
	 * 							  with an non-existing <i>PayloadId</i>.
	 */
	<T extends IMessageUnit, E extends IMessageUnitEntity> E storeMessageUnit(final T messageUnit)
															throws DuplicateMessageIdException, StorageException;

	/**
	 * Saves the updated meta-data of the message unit to the database. If the database already contains newer data the
	 * update must be rejected.
	 * <p>
	 * NOTE 1: The meta-data that can be updated is limited to the data for which update methods are specified in the
	 * entity interfaces. Implementation may use this knowledge to optimise the database updates.<br/>
	 * NOTE 2: This method should not update the meta-data on paylaods contained in a User Message. For updates to
	 * payload meta-data the Holodeck B2B Core will use {@link #updatePayload(IPayloadEntity)}.
	 *
	 * @param messageUnit   Entity object containing the updated data that should be saved to storage
	 * @throws AlreadyChangedException When the database contains more up to date data.
	 * @throws StorageException   If some other error occured when saving the updated message unit to the database
	 */
	void updateMessageUnit(final IMessageUnitEntity messageUnit) throws AlreadyChangedException, StorageException;

	/**
	 * Deletes the meta-data of the given message unit from the database. For User Message message units the provider
	 * must also remove all related {@link IPayloadEntity} objects.
	 *
	 * @param messageUnit       The {@link IMessageUnitEntity} object to be deleted
	 * @throws StorageException     When a problem occurs while removing the message unit from the database.
	 */
	void deleteMessageUnit(IMessageUnitEntity messageUnit) throws StorageException;

	/**
	 * Stores the meta-data of the given payload in the database and returns a new entity object representing the
	 * saved payload info. The new object MUST be assigned a unique <i>PayloadId</i>. The assigned <i>PayloadId</i> MUST
	 * NOT contain special characters.
	 *
	 * @param payload	the payload which meta-data must be stored
	 * @return	the created entity object
	 * @throws StorageException If an error occurs when saving the payload meta-data to the database
	 */
	IPayloadEntity storePayloadMetadata(final IPayload payload) throws StorageException;

	/**
	 * Saves the updated meta-data of the payload to the database. If the database already contains newer data the
	 * update must be rejected.
	 *
	 * @param payload	Entity object containing the updated data that should be saved to storage
	 * @throws AlreadyChangedException When the database contains more up to date data.
	 * @throws StorageException	If an error occurs when saving the updated payload data to the database, for example
	 * 								when the database contains more up to date meta-data.
	 */
	void updatePayloadMetadata(IPayloadEntity payload) throws AlreadyChangedException, StorageException;

	/**
	 * Deletes the meta-data of the given payload from the database. If the User Message to which the payload to be
	 * deleted is related still exists, the delete request should be rejected.
	 *
	 * @param payload       The {@link IPayloadEntity} object to be deleted
	 * @throws StorageException     When a problem occurs while removing the payload meta-data from the database. This
	 * 								exception can be thrown when the User Message to which the payload is linked still
	 * 								exists.
	 */
	void deletePayloadMetadata(final IPayloadEntity payload) throws StorageException;

	/*----------------------------------------------------------------------------------------------------------------
	 * Query methods to retrieve message unit meta-data
	 *--------------------------------------------------------------------------------------------------------------*/

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
	 * @param pmodeIds  Set of P-Mode ids
	 * @param state     The processing state the message units to retrieve should be in
	 * @return          The ordered list of entity objects representing the message unit objects of the specified
	 *                  type and which are in the specified processing state and have their processing defined by a
	 *                  P-Mode with one of the specified ids
	 * @throws StorageException When an error occurs while executing the query
	 */
	<T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(
	                                                                                final Class<T> type,
	                                                                                final Set<String> pmodeIds,
	                                                                                final ProcessingState state)
	                                                                            throws StorageException;

	/**
	 * Retrieves all message units of the specified type and that are in one of the given states and are flowing in the
	 * specified direction. The found message units are sorted on their time stamp starting with the oldest message
	 * units.
	 * <p><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
	 * unit is going to be processed it must be checked if it is loaded completely.
	 *
	 * @param <T>       Limits the <code>type</code> parameter to only message unit classes
	 * @param <V>       The returned objects will be entity objects. V and T will share the same parent type.
	 * @param type      The type of message units to retrieve specified by the interface they implement
	 * @param direction The direction of the message units to retrieve
	 * @param states    Set of processing states that the message units to retrieve should be in
	 * @return          List with entity objects representing the message units of the specified type that are
	 * 					in one of the given states, in ascending order on time stamp
	 * @throws StorageException When a problem occurs during the retrieval of the message units
	 */
	<T extends IMessageUnit, V extends IMessageUnitEntity> List<V>
	                                             getMessageUnitsInState(final Class<T> type,
	                                                                    final Direction direction,
	                                                                    final Set<ProcessingState> states)
	                                                                                    throws StorageException;

	/**
	 * Retrieves all message units with the given <code>MessageId</code>. Optionally the direction in which the
	 * searched messages units flow can also be specified.
	 * <p>Although messageIds should be unique there can exist multiple <code>MessageUnits</code> with the same
	 * messageId due to resending (and because other MSH or business applications may not conform to this constraint).
	 * <br><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
	 * unit is going to be processed it must be checked if it is loaded completely.
	 *
	 * @param messageId     The messageId of the message units to retrieve
	 * @param direction		The direction in which the message units to retrieve should be.
	 * @return              The list of {@link IMessageUnitEntity}s with the given message id
	 * @throws StorageException If an error occurs retrieving the message units from the database
	 */
	Collection<IMessageUnitEntity> getMessageUnitsWithId(final String messageId,
														 final Direction... direction)
																 						throws StorageException;

	/**
	 * Retrieves all message units of which the last change in processing state occurred before the given date and time.
	 * <br><b>NOTE:</b> The entity objects in the resulting collection may not be completely loaded! Before a message
	 * unit is going to be processed it must be checked if it is loaded completely.
	 *
	 * @param   maxLastChangeDate   The latest date of a processing state change that is to be included in the result
	 * @return          Collection of entity objects representing the message units which processing state changed at
	 *                  latest at the given date
	 * @throws StorageException If an error occurs while retrieving the message units from the database
	 */
	Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(final Date maxLastChangeDate)
	                                                                                    throws StorageException;

	/**
	 * Retrieves the message unit with the given <code>CoreId</code>.
	 * <p><b>NOTE:</b> The returned entity object may not be completely loaded! Before a message unit is going to be
	 * processed it must be checked if it is loaded completely.
	 *
	 * @param coreId     The CoreId of the message unit to retrieve
	 * @return           The {@link IMessageUnitEntity} with the given CoreId or <code>null</code> if none exists
	 * @throws StorageException If an error occurs when retrieving the message unit from the database
	 * @since 7.0.0
	 */
	IMessageUnitEntity getMessageUnitWithCoreId(final String coreId) throws StorageException;

	/**
	 * Gets the number of times the given <i>User Message</i> message unit has already been sent to the receiver without
	 * getting a <i>Receipt</i> back.
	 * <p>This equals the number of times the message unit was in the {@link ProcessingState#SENDING} state. Note that
	 * this means also failed transmission attempts, i.e. where a transport failure occurred, count for the result.
	 *
	 * @param userMessage   The user message to get the number of transmissions for
	 * @return              The number of times the user message was already sent out
	 * @throws StorageException If an error occurs when retrieving the number of transmissions
	 */
	int getNumberOfTransmissions(final IUserMessageEntity userMessage) throws StorageException;

	/**
	 * Checks whether there exists a <b>received</b> <i>User Message</i> message unit with the given <code>MessageId
	 * </code> that has already been processed completely or is in the process of delivery to the back end, i.e. its
	 * <i>current</i> processing state is either {@link ProcessingState#DELIVERED}, {@link
	 * ProcessingState#OUT_FOR_DELIVERY} or {@link ProcessingState#FAILURE}.
	 *
	 * @param userMessage The <code>User Message</code> to check for if it's already processed
	 * @return            <code>true</code> if there exists a User Message entity with {@link
	 *                    IUserMessage#getMessageId()} == <code>messageId</code> and {@link IUserMessage#getDirection()}
	 *                    == <code>IN</code> and {@link IUserMessage#getCurrentProcessingState()} ==
	 *                    {@link ProcessingState#DELIVERED} | {@link ProcessingState#OUT_FOR_DELIVERY}
	 *                    | {@link ProcessingState#FAILURE},
	 *                    <br><code>false</code> otherwise.
	 * @throws StorageException If an error occurs when executing this query
	 * @since 4.0.0
	 * @since 7.0.0 The argument type is now the entity class
	 */
	boolean isAlreadyProcessed(final IUserMessageEntity userMessage) throws StorageException;
}
