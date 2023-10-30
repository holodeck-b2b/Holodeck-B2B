package org.holodeckb2b.persistency.inmemory.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

public abstract class MessageUnitDTO implements IMessageUnitEntity {
	private Date	lastChange;
	private String 	coreId;
	private String	pModeId;
	private String 	messageId;
	private String 	refToMessageId;
	private Date	timestamp;
	private Direction direction;
	private boolean usesMultiHop;
	
	private List<IMessageUnitProcessingState> states = new ArrayList<>();
	private Set<String>	related = new HashSet<>();
	
	protected MessageUnitDTO() {
		this.coreId = UUID.randomUUID().toString();
		this.lastChange = new Date();
	}
	
	protected MessageUnitDTO(IMessageUnit source) {
		this();		
		if (source ==  null)
			copyFrom(source);
	}
	
	public void copyFrom(IMessageUnit source) {		
		this.pModeId = source.getPModeId();
		this.messageId = source.getMessageId();
		this.refToMessageId = source.getRefToMessageId();
		this.direction = source.getDirection();
		this.timestamp = source.getTimestamp();		
		this.states = new ArrayList<>();
		if (!Utils.isNullOrEmpty(source.getProcessingStates()))
			source.getProcessingStates().forEach(s -> this.states.add(new MessageProcessingState(s)));
		if (source instanceof IMessageUnitEntity) {
			IMessageUnitEntity e = (IMessageUnitEntity) source;
			this.coreId = e.getCoreId();
			this.usesMultiHop = e.usesMultiHop();
			this.related = new HashSet<>();
			if (!Utils.isNullOrEmpty(e.getRelatedTo()))
				this.related.addAll(e.getRelatedTo());
		}
		if (source instanceof MessageUnitDTO)
			this.lastChange = ((MessageUnitDTO) source).getLastChanged();
	}
	
	public abstract MessageUnitDTO clone();
	
	public Date getLastChanged() {
		return lastChange;
	}
	
	public void setChanged(Date d) {
		this.lastChange = d;
	}
	
	@Override
	public Direction getDirection() {
		return direction;
	}

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public String getRefToMessageId() {
		return refToMessageId;
	}

	@Override
	public String getPModeId() {
		return pModeId;
	}
	
	public void setPModeId(String pModeId) {
		this.pModeId = pModeId;
	}

	@Override
	public List<IMessageUnitProcessingState> getProcessingStates() {
		return states;
	}

	@Override
	public IMessageUnitProcessingState getCurrentProcessingState() {
		return states.isEmpty() ? null : states.get(states.size() - 1);
	}

	@Override
	public String getCoreId() {
		return coreId;
	}

	@Override
	public Set<String> getRelatedTo() {
		return related;
	}

	@Override
	public void addRelatesTo(String coreId) {
		related.add(coreId);
	}

	@Override
	public boolean isLoadedCompletely() {
		return true;
	}

	@Override
	public boolean usesMultiHop() {
		return usesMultiHop;
	}

	@Override
	public void setMultiHop(boolean usingMultiHop) {
		usesMultiHop = usingMultiHop;
	}

	@Override
	public void setProcessingState(ProcessingState newState, String descr) {
		states.add(new MessageProcessingState(newState, descr));
	}
}
