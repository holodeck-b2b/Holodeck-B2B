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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.jpa.UserMessage;

/**
 * Is the {@link IUserMessageEntity} implementation of the default Meta-data Storage Provider of Holodeck B2B.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class UserMessageEntity extends MessageUnitEntity<UserMessage> implements IUserMessageEntity {

	private List<PayloadEntity>	payloadEntities;

    public UserMessageEntity(UserMessage jpaObject) {
        super(jpaObject);
    }

    @Override
    public void loadCompletely() {
    	super.loadCompletely();
    	jpaEntityObject.getReceiver();
    	jpaEntityObject.getSender();
    	jpaEntityObject.getPayloads();
    	allMetadataLoaded = true;
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
    public Collection<PayloadEntity> getPayloads() {
        if (!Utils.isNullOrEmpty(payloadEntities))
        	return payloadEntities;

        Collection<PayloadInfo> pl = jpaEntityObject.getPayloads();
        payloadEntities = new ArrayList<>(!Utils.isNullOrEmpty(pl) ? pl.size() : 0);
        if (!Utils.isNullOrEmpty(pl))
        	pl.forEach(p -> payloadEntities.add(new PayloadEntity(p)));

        return payloadEntities;
    }

}
