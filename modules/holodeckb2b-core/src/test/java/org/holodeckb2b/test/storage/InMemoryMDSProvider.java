/*
 * Copyright (C) 2024 The Holodeck B2B Team
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
package org.holodeckb2b.test.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.AlreadyChangedException;
import org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is a {@link IPersistencyProvider} implementation for testing that stores the message units in-memory. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class InMemoryMDSProvider implements IMetadataStorageProvider {

	private Set<IMessageUnitEntity>	 msgUnitStore = Collections.synchronizedSet(new HashSet<IMessageUnitEntity>());	
	private Set<IPayloadEntity>	 payloadInfoStore = Collections.synchronizedSet(new HashSet<IPayloadEntity>());	
	
	@Override
	public String getName() {
		return "In Memory Test Meta-data Storage Provider";
	}
		
	@Override
	public void init(IConfiguration config) throws StorageException {
	}

	@Override
	public void shutdown() {
	}

	public long getNumberOfStoredMessageUnits() {
		return msgUnitStore.size();
	}

	public long getNumberOfStoredPayloads() {
		return payloadInfoStore.size();
	}
	
	public void clear() {
		msgUnitStore.clear();
		payloadInfoStore.clear();
	}
	
	public boolean existsMessageId(String messageId) {
		return msgUnitStore.parallelStream().anyMatch(m -> messageId.equals(m.getMessageId()));
	}

	public boolean existsPayloadId(String payloadId) {
		return payloadInfoStore.parallelStream().anyMatch(p -> payloadId.equals(p.getPayloadId()));
	}
	
	public IMessageUnitEntity getMessageUnit(String messageId) {
		return msgUnitStore.parallelStream().filter(m -> messageId.equals(m.getMessageId())).findFirst().orElse(null);
	}

	public IPayloadEntity getPayloadMetadate(String payloadId) {
		return payloadInfoStore.parallelStream().filter(p -> payloadId.equals(p.getPayloadId())).findFirst().orElse(null);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeMessageUnit(T messageUnit)
																							throws StorageException {
		IMessageUnitEntity dto;
		if (messageUnit instanceof IUserMessage)
			dto = new UserMessageEntity((IUserMessage) messageUnit);
		else if (messageUnit instanceof ISelectivePullRequest)
			dto = new PullRequestEntity((IPullRequest) messageUnit);
		else if (messageUnit instanceof IPullRequest)
			dto = new PullRequestEntity((IPullRequest) messageUnit);		
		else if (messageUnit instanceof IReceipt)
			dto = new ReceiptEntity((IReceipt) messageUnit);
		else if (messageUnit instanceof IErrorMessage)
			dto = new ErrorMessageEntity((IErrorMessage) messageUnit);
		else
			throw new IllegalArgumentException("Unknown message unit type");
		
		msgUnitStore.add(dto);
		
		return (V) ((MessageUnitEntity) dto).clone();
	}

	@Override
	public void updateMessageUnit(IMessageUnitEntity messageUnit) throws StorageException {
		if (!(messageUnit instanceof MessageUnitEntity))
			throw new StorageException("Unsupported entity class");
		
		MessageUnitEntity update = ((MessageUnitEntity) messageUnit);
		MessageUnitEntity stored = (MessageUnitEntity) msgUnitStore.stream()
									.filter(m -> messageUnit.getCoreId().equals(m.getCoreId())).findFirst().orElse(null);
		if (stored == null)
			throw new StorageException("Entity not managed");
		
		if (stored.getLastChanged().after(update.getLastChanged())) {
			update.copyFrom(stored);
			throw new AlreadyChangedException(); 
		}
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		update.setChanged(new Date());
		if (stored != messageUnit)
			stored.copyFrom(update);
	}
	
	@Override
	public IPayloadEntity storePayloadMetadata(IPayload payload) throws StorageException {
		PayloadEntity entity = new PayloadEntity(payload);
		payloadInfoStore.add(entity);
		
		return entity.clone();
	}	
	
	@Override
	public void updatePayloadMetadata(IPayloadEntity payload) throws StorageException {
		if (!(payload instanceof PayloadEntity))
			throw new StorageException("Unsupported entity class");

		PayloadEntity update = ((PayloadEntity) payload);
		PayloadEntity stored = (PayloadEntity) payloadInfoStore.stream()
								.filter(p -> payload.getPayloadId().equals(p.getPayloadId())).findFirst().orElse(null);
		if (stored == null)
			throw new StorageException("Entity not managed");
		
		if (stored.getLastChanged().after(update.getLastChanged())) {
			update.copyFrom(stored);
			throw new AlreadyChangedException(); 
		}
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		update.setChanged(new Date());
		if (stored != update)
			stored.copyFrom(update);
	}	

	@Override
	public void deleteMessageUnit(IMessageUnitEntity messageUnit) throws StorageException {
		msgUnitStore.removeIf(m -> m.getCoreId().equals(messageUnit.getCoreId()));
	}
	
    @Override
   	public void deletePayloadMetadata(IPayloadEntity payload) throws StorageException {
    	payloadInfoStore.removeIf(p -> p.getPayloadId().equals(payload.getPayloadId()));
   	}	
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsInState(Class<T> type,
			Direction direction, ProcessingState[] states) throws StorageException {
		final List<ProcessingState> statesCollection = Arrays.asList(states);
		
		return 	(List<V>) msgUnitStore.stream().filter(m -> type.isAssignableFrom(m.getClass())  
													&& m.getDirection() == direction 
													&& statesCollection.contains(m.getCurrentProcessingState().getState()))
										   .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
										   .collect(Collectors.toList());

	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithId(String messageId, Direction... direction)
			throws StorageException {
		if (direction.length > 2)
			throw new StorageException("Cannot specify more than 2 directions");
		else if (direction.length == 0) {
			direction = new Direction[] { Direction.IN, Direction.OUT};
		}
		
		final Collection<Direction> dirCollections = Arrays.asList(direction); 
			
		return msgUnitStore.stream().filter(m -> dirCollections.contains(m.getDirection()) 
											&& messageId.equals(m.getMessageId()))		
								.collect(Collectors.toList());
	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(Date maxLastChangeDate)
			throws StorageException {
		return msgUnitStore.stream().filter(m -> m.getCurrentProcessingState().getStartTime().before(maxLastChangeDate))		
								.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(Class<T> type,
			Collection<String> pmodeIds, ProcessingState state) throws StorageException {
		return 	(List<V>) msgUnitStore.stream().filter(m -> type.isAssignableFrom(m.getClass())  
													&& pmodeIds.contains(m.getPModeId()) 
													&& m.getCurrentProcessingState().getState() == state)
										   .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
										   .collect(Collectors.toList());	
	}


	@Override
	public int getNumberOfTransmissions(IUserMessageEntity userMessage) throws StorageException {
		return (int) 
				userMessage.getProcessingStates().stream().filter(s -> s.getState() == ProcessingState.SENDING).count();
	}

	@Override
	public boolean isAlreadyProcessed(IUserMessageEntity userMessage) throws StorageException {
		return userMessage.getCurrentProcessingState().getState() == ProcessingState.FAILURE ||
					userMessage.getCurrentProcessingState().getState() == ProcessingState.DELIVERED;
	}

	@Override
	public IMessageUnitEntity getMessageUnitWithCoreId(String coreId) throws StorageException {
		return msgUnitStore.stream().filter(m -> m.getCoreId().equals(coreId)).findFirst().orElse(null);
	}

	@Override
	public <E extends IMessageUnitEntity> void loadCompletely(E entity) throws StorageException {
	}    
}
