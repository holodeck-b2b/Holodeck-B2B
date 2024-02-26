/*
 * Copyright (C) 2019 The Holodeck B2B Team
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;

public abstract class MessageUnitEntity implements IMessageUnitEntity {
	private Date	lastChange;
	private String 	coreId;
	private String	pModeId;
	private String 	messageId;
	private String 	refToMessageId;
	private Date	timestamp;
	private Direction direction;
	private boolean usesMultiHop;

	private List<IMessageUnitProcessingState> states = new ArrayList<>();

	protected MessageUnitEntity() {
		this.coreId = UUID.randomUUID().toString();
		this.lastChange = new Date();
	}

	protected MessageUnitEntity(IMessageUnit source) {
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
		}
		if (source instanceof MessageUnitEntity)
			this.lastChange = ((MessageUnitEntity) source).getLastChanged();
	}

	@Override
	public abstract MessageUnitEntity clone();

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

	@Override
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
