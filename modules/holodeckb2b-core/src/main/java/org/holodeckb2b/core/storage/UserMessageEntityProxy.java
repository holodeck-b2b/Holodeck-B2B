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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;

/**
 * Is a proxy to the {@link IUserMessageEntity} object managed by the {@link IMetadataStorageProvider} that holds a
 * collection of {@link PayloadEntityProxy}s so the payloads' content can also be managed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public class UserMessageEntityProxy implements IUserMessageEntity {
	// The entity object being proxied
	private final IUserMessageEntity	source;
	// The payload proxies
	private Collection<PayloadEntityProxy>	payloads = null;

	UserMessageEntityProxy(IUserMessageEntity source) {
		this.source = source;
		this.payloads = new ArrayList<>();
		if (!Utils.isNullOrEmpty(source.getPayloads()))
			source.getPayloads().forEach(p -> this.payloads.add(p instanceof PayloadEntityProxy ? (PayloadEntityProxy) p
																						: new PayloadEntityProxy(p)));
	}

	IUserMessageEntity getSource() {
		return source;
	}

	@Override
	public String getCoreId() {
		return source.getCoreId();
	}

	@Override
	public boolean usesMultiHop() {
		return source.usesMultiHop();
	}

	@Override
	public void setMultiHop(boolean usingMultiHop) {
		source.setMultiHop(usingMultiHop);
	}

	@Override
	public void setPModeId(String pmodeId) {
		source.setPModeId(pmodeId);
	}

	@Override
	public void setProcessingState(ProcessingState newState, String description) {
		source.setProcessingState(newState, description);
	}

	@Override
	public IMessageUnitProcessingState getCurrentProcessingState() {
		return source.getCurrentProcessingState();
	}

	@Override
	public Direction getDirection() {
		return source.getDirection();
	}

	@Override
	public Date getTimestamp() {
		return source.getTimestamp();
	}

	@Override
	public String getMessageId() {
		return source.getMessageId();
	}

	@Override
	public String getRefToMessageId() {
		return source.getRefToMessageId();
	}

	@Override
	public String getPModeId() {
		return source.getPModeId();
	}

	@Override
	public List<IMessageUnitProcessingState> getProcessingStates() {
		return source.getProcessingStates();
	}

	@Override
	public String getMPC() {
		return source.getMPC();
	}

	@Override
	public ITradingPartner getSender() {
		return source.getSender();
	}

	@Override
	public ITradingPartner getReceiver() {
		return source.getReceiver();
	}

	@Override
	public ICollaborationInfo getCollaborationInfo() {
		return source.getCollaborationInfo();
	}

	@Override
	public Collection<IProperty> getMessageProperties() {
		return source.getMessageProperties();
	}

	@Override
	public Collection<PayloadEntityProxy> getPayloads() {
		return payloads;
	}
}
