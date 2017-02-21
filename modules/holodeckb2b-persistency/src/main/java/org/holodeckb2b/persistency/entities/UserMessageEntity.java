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
package org.holodeckb2b.persistency.entities;

import java.util.Collection;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.persistency.jpa.UserMessage;

/**
 * Is the {@link IUserMessageEntity} implementation of the default persistency provider of Holodeck B2B.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public class UserMessageEntity extends MessageUnitEntity<UserMessage> implements IUserMessageEntity {

    public UserMessageEntity(UserMessage jpaObject) {
        super(jpaObject);
    }

    @Override
    public String getMPC() {
        return jpaEntityObject.getMPC();
    }

    @Override
    public ITradingPartner getSender() {
        return jpaEntityObject.getSender();
    }

    @Override
    public ITradingPartner getReceiver() {
        return jpaEntityObject.getReceiver();
    }

    @Override
    public ICollaborationInfo getCollaborationInfo() {
        return jpaEntityObject.getCollaborationInfo();
    }

    @Override
    public Collection<IProperty> getMessageProperties() {
        return jpaEntityObject.getMessageProperties();
    }

    @Override
    public Collection<IPayload> getPayloads() {
        return jpaEntityObject.getPayloads();
    }
}
