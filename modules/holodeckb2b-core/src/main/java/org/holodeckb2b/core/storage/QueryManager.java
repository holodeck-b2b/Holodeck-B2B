/**
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.IQueryManager;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the Holodeck B2B Core component that offers access to message data (both meta-data and payload data) to both other
 * Core and  "external" components.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
@SuppressWarnings("unchecked")
public class QueryManager implements IQueryManager {
	private static final Logger	log = LogManager.getLogger();

	/**
	 * The Metadata Storage Provider in use for storing the message meta-data
	 */
	private final IMetadataStorageProvider	mdsProvider;

	/**
	 * The Payload Storage Provider in use for storing the message meta-data
	 */
	private final IPayloadStorageProvider	psProvider;

	 /**
     * Creates a new query manager that will use the given Metadata and Payload Storage Providers to retrieve the data
     * of message units.
     *
     * @param mdsp    The Metadata Storage Provider in use
     * @param psp     The Payload Storage Provider in use
     */
    public QueryManager(final IMetadataStorageProvider mdsp, final IPayloadStorageProvider psp) {
        this.mdsProvider = mdsp;
        this.psProvider = psp;
    }

	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsInState(Class<T> type,
			Direction direction, Set<ProcessingState> states) throws StorageException {
		return (List<V>) executeQuery(() -> mdsProvider.getMessageUnitsInState(type, direction, states))
							.collect(Collectors.toList());
	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithId(String messageId, Direction... direction)
			throws StorageException {
		return executeQuery(() -> mdsProvider.getMessageUnitsWithId(messageId, direction)).collect(Collectors.toList());
	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(Date maxLastChangeDate)
			throws StorageException {
		return executeQuery(() -> mdsProvider.getMessageUnitsWithLastStateChangedBefore(maxLastChangeDate))
							.collect(Collectors.toList());
	}

	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(Class<T> type,
			Set<String> pmodeIds, ProcessingState state) throws StorageException {
		return (List<V>) executeQuery(() -> mdsProvider.getMessageUnitsForPModesInState(type, pmodeIds, state))
							.collect(Collectors.toList());
	}

	@Override
	public int getNumberOfTransmissions(IUserMessageEntity userMessage) throws StorageException {
		return executeQuery(() -> Collections.singleton(
							mdsProvider.getNumberOfTransmissions(((UserMessageEntityProxy) userMessage).getSource())))
							.findFirst().orElse(0);
	}

	@Override
	public boolean isAlreadyProcessed(IUserMessageEntity userMessage) throws StorageException {
		return executeQuery(() -> Collections.singleton(
							mdsProvider.isAlreadyProcessed(((UserMessageEntityProxy) userMessage).getSource())))
							.findFirst().orElse(Boolean.FALSE);
	}

	@Override
	public IMessageUnitEntity getMessageUnitWithCoreId(String coreId) throws StorageException {
		return executeQuery(() -> Collections.singleton(mdsProvider.getMessageUnitWithCoreId(coreId))).findFirst().orElse(null);
	}

	IPayloadContent retrievePayloadContent(IPayloadEntity payload) throws StorageException {
		try {
			return psProvider.getPayloadContent(payload);
		} catch (StorageException payloadFailure) {
			log.error("Error retrieving the paylaod content (payloadId={}) : {}", payload.getPayloadId(),
						Utils.getExceptionTrace(payloadFailure));
			throw payloadFailure;
		}
	}

	@Override
	public Collection<IPayloadEntity> getUnboundPayloads() throws StorageException {
		return executeQuery(() -> mdsProvider.getUnboundPayloads())
												.map(p -> new PayloadEntityProxy(p))
												.collect(Collectors.toList());
	}

	private <R> Stream<R> executeQuery(Query<R> query) throws StorageException {
		try {
			return (Stream<R>) query.execute().stream()
					.map(m -> m instanceof IUserMessageEntity ? new UserMessageEntityProxy((IUserMessageEntity) m) : m);
		} catch (StorageException queryError) {
	    	log.error("Error executing query ({}) : {}",
	    				queryError.fillInStackTrace().getStackTrace()[1].getMethodName(),
	    				Utils.getExceptionTrace(queryError));
			throw queryError;
		}
	}

	interface Query<R> {
		Collection<R> execute() throws StorageException;
	}
}
