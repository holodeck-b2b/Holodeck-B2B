package org.holodeckb2b.persistency.inmemory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

@SuppressWarnings("unchecked")
public class QueryManager implements IQueryManager {

	private Set<IMessageUnitEntity>	msgUnits;
	
	QueryManager(Set<IMessageUnitEntity> data) {
		msgUnits = data;
	}
	
	public long getNumberOfStoredMessageUnits() {
		return msgUnits.size();
	}
	
	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsInState(Class<T> type,
			Direction direction, ProcessingState[] states) throws PersistenceException {
		final List<ProcessingState> statesCollection = Arrays.asList(states);
		
		return 	(List<V>) msgUnits.stream().filter(m -> type.isAssignableFrom(m.getClass())  
													&& m.getDirection() == direction 
													&& statesCollection.contains(m.getCurrentProcessingState().getState()))
										   .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
										   .collect(Collectors.toList());

	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithId(String messageId, Direction... direction)
			throws PersistenceException {
		if (direction.length > 2)
			throw new PersistenceException("Cannot specify more than 2 directions");
		else if (direction.length == 0) {
			direction = new Direction[] { Direction.IN, Direction.OUT};
		}
		
		final Collection<Direction> dirCollections = Arrays.asList(direction); 
			
		return msgUnits.stream().filter(m -> dirCollections.contains(m.getDirection()) 
											&& messageId.equals(m.getMessageId()))		
								.collect(Collectors.toList());
	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(Date maxLastChangeDate)
			throws PersistenceException {
		return msgUnits.stream().filter(m -> m.getCurrentProcessingState().getStartTime().before(maxLastChangeDate))		
								.collect(Collectors.toList());
	}

	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(Class<T> type,
			Collection<String> pmodeIds, ProcessingState state) throws PersistenceException {
		return 	(List<V>) msgUnits.stream().filter(m -> type.isAssignableFrom(m.getClass())  
													&& pmodeIds.contains(m.getPModeId()) 
													&& m.getCurrentProcessingState().getState() == state)
										   .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
										   .collect(Collectors.toList());	
	}

	@Override
	public <V extends IMessageUnitEntity> void ensureCompletelyLoaded(V messageUnit) throws PersistenceException {		
	}

	@Override
	public int getNumberOfTransmissions(IUserMessageEntity userMessage) throws PersistenceException {
		return (int) 
				userMessage.getProcessingStates().stream().filter(s -> s.getState() == ProcessingState.SENDING).count();
	}

	@Override
	public boolean isAlreadyProcessed(IUserMessage userMessage) throws PersistenceException {
		return userMessage.getCurrentProcessingState().getState() == ProcessingState.FAILURE ||
					userMessage.getCurrentProcessingState().getState() == ProcessingState.DELIVERED;
	}

}
