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
import java.util.Collection;

import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;

/**
 * Is the {@link IUserMessageEntity} implementation of the in-memory persistency provider used for testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UserMessageEntity extends MessageUnitEntity implements IUserMessageEntity {
	private String               mpc;
    private TradingPartner       sender;
    private TradingPartner       receiver;
    private CollaborationInfo    collabInfo;
    private ArrayList<IProperty> msgProperties = new ArrayList<>();
    private ArrayList<PayloadEntity>  payloads = new ArrayList<>();

    public UserMessageEntity() {
        super();
    }

    public UserMessageEntity(final IUserMessage sourceUserMessage) {
        super(sourceUserMessage);
        this.mpc = sourceUserMessage.getMPC();
        setSender(sourceUserMessage.getSender());
        setReceiver(sourceUserMessage.getReceiver());
        setCollaborationInfo(sourceUserMessage.getCollaborationInfo());
        setMessageProperties(sourceUserMessage.getMessageProperties());
    }

    public void copyFrom(IUserMessage sourceUserMessage) {
        if (sourceUserMessage == null)
            return;
        super.copyFrom(sourceUserMessage);

        this.mpc = sourceUserMessage.getMPC();
        setSender(sourceUserMessage.getSender());
        setReceiver(sourceUserMessage.getReceiver());
        setCollaborationInfo(sourceUserMessage.getCollaborationInfo());
        setMessageProperties(sourceUserMessage.getMessageProperties());
        this.payloads = new ArrayList<>();
        if (!Utils.isNullOrEmpty(sourceUserMessage.getPayloads()))
        	sourceUserMessage.getPayloads().forEach(p -> payloads.add(new PayloadEntity(this, p)));
    }

    @Override
	protected MessageUnitEntity clone() {
		UserMessageEntity clone = new UserMessageEntity();
		clone.copyFrom(this);
		return clone;
	}

    @Override
    public String getMPC() {
        return mpc;
    }

    public void setMPC(final String mpc) {
        this.mpc = mpc;
    }

    @Override
    public TradingPartner getSender() {
        return sender;
    }

    public void setSender(final ITradingPartner sender) {
        this.sender = sender != null ? new TradingPartner(sender) : null;
    }

    @Override
    public TradingPartner getReceiver() {
        return receiver;
    }

    public void setReceiver(final ITradingPartner receiver) {
        this.receiver = receiver != null ? new TradingPartner(receiver) : null;
    }

    @Override
    public CollaborationInfo getCollaborationInfo() {
        return collabInfo;
    }

    public void setCollaborationInfo(final ICollaborationInfo ci) {
        this.collabInfo = ci != null ? new CollaborationInfo(ci) : null;
    }

    @Override
    public Collection<IProperty> getMessageProperties() {
        return msgProperties;
    }

    void setMessageProperties(final Collection<IProperty> msgProps) {
        this.msgProperties = new ArrayList<>(msgProps != null ? msgProps.size() : 0);
        if(!Utils.isNullOrEmpty(msgProps))
            msgProps.forEach(p -> this.msgProperties.add(new Property(p)));
    }

    void addMessageProperty(final IProperty prop) {
        if (prop != null)
            this.msgProperties.add(new Property(prop));
    }

    @Override
    public Collection<PayloadEntity> getPayloads() {
        return payloads;
    }

    void addPayload(PayloadEntity payload) {
    	payloads.add(payload);
    }
}
