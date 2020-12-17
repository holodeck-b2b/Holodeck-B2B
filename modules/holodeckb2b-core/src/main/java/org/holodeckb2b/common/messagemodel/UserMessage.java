/**
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.messagemodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is an in memory only implementation of {@link IUserMessage} to temporarily store the meta-data information on a User
 * Message message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class UserMessage extends MessageUnit implements IUserMessage, Serializable {
	private static final long serialVersionUID = -5011254916644982271L;

	private String               mpc;

    private TradingPartner       sender;
    private TradingPartner       receiver;

    private CollaborationInfo    collabInfo;

    private ArrayList<IProperty> msgProperties;
    private ArrayList<IPayload>  payloads;

    /**
     * Default constructor to initialize as empty meta-data object
     */
    public UserMessage() {
        super();
    }

    /**
     * Create a new <code>UserMessage</code> object for the user message unit described by the given
     * {@link IUserMessage} object.
     *
     * @param sourceUserMessage   The meta data of the user message unit to copy to the new object
     */
    public UserMessage(final IUserMessage sourceUserMessage) {
        super(sourceUserMessage);

        if (sourceUserMessage == null)
            return;

        this.mpc = sourceUserMessage.getMPC();
        setSender(sourceUserMessage.getSender());
        setReceiver(sourceUserMessage.getReceiver());
        setCollaborationInfo(sourceUserMessage.getCollaborationInfo());
        setMessageProperties(sourceUserMessage.getMessageProperties());
        setPayloads(sourceUserMessage.getPayloads());
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

    public void setMessageProperties(final Collection<IProperty> msgProps) {
        this.msgProperties = new ArrayList<>(msgProps != null ? msgProps.size() : 0);
        if(!Utils.isNullOrEmpty(msgProps)) {
            for(final IProperty p : msgProps)
                this.msgProperties.add(new Property(p));
        }
    }

    public void addMessageProperty(final IProperty prop) {
        if (prop != null) {
            if (this.msgProperties == null)
                this.msgProperties = new ArrayList<>();
            this.msgProperties.add(new Property(prop));
        }
    }

    @Override
    public Collection<IPayload> getPayloads() {
        return payloads;
    }

    /**
     * Sets the meta-data on the payloads contained in this User Message.
     *
     * @param payloads  The meta-data on the payloads
     */
    public void setPayloads(final Collection<IPayload> payloads) {
        if (!Utils.isNullOrEmpty(payloads)) {
            this.payloads = new ArrayList<>();
            for (IPayload p : payloads)
                this.payloads.add(new Payload(p));
        } else
            this.payloads = null;
    }

    /**
     * Adds meta-data about one payload to the existing set of payload meta-data.
     *
     * @param p The meta-data on the specific payload
     */
    public void addPayload(final IPayload p) {
        if (p != null) {
            if (payloads == null)
                payloads = new ArrayList<>(1);
            payloads.add(new Payload(p));
        }
    }
}
