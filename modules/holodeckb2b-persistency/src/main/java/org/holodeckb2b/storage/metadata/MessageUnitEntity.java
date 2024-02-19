/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata;

import java.util.Date;
import java.util.List;

import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.storage.metadata.jpa.MessageUnit;

/**
 * Is the {@link IMessageUnitEntity} implementation of the default Meta-data Storage Provider of Holodeck B2B.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @param <T>   The class of the proxied JPA entity object
 * @since  3.0.0
 */
public abstract class MessageUnitEntity<T extends MessageUnit> extends JPAObjectProxy<T> implements IMessageUnitEntity {

    /**
     * Creates a new <code>MessageUnitEntity</code> object for the given JPA entity object.
     *
     * @param jpaObject     The JPA entity object to create a proxy for
     */
    protected MessageUnitEntity(final T jpaObject) {
        super(jpaObject);
    }

    @Override
    public void loadCompletely() {
    	jpaEntityObject.getProcessingStates();
    }

    @Override
    public boolean usesMultiHop() {
        return jpaEntityObject.usesMultiHop();
    }

    @Override
    public Direction getDirection() {
        return jpaEntityObject.getDirection();
    }

    @Override
    public Date getTimestamp() {
        return jpaEntityObject.getTimestamp();
    }

    @Override
    public String getMessageId() {
        return jpaEntityObject.getMessageId();
    }

    @Override
    public String getRefToMessageId() {
        return jpaEntityObject.getRefToMessageId();
    }

    @Override
    public String getPModeId() {
        return jpaEntityObject.getPModeId();
    }

    @Override
    public List<IMessageUnitProcessingState> getProcessingStates() {
        return jpaEntityObject.getProcessingStates();
    }

    @Override
    public IMessageUnitProcessingState getCurrentProcessingState() {
        return jpaEntityObject.getCurrentProcessingState();
    }

	@Override
	public String getCoreId() {
		return jpaEntityObject.getCoreId();
	}

	@Override
	public void setPModeId(String pmodeId) {
		jpaEntityObject.setPModeId(pmodeId);
	}

	@Override
	public void setMultiHop(boolean usingMultiHop) {
		jpaEntityObject.setMultiHop(usingMultiHop);
	}

	@Override
	public void setProcessingState(ProcessingState newState, String description) {
		jpaEntityObject.setProcessingState(new MessageProcessingState(newState, description));
	}
}
