package org.holodeckb2b.persistency.inmemory.dto;

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
import org.holodeckb2b.interfaces.persistency.entities.IPayloadEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;

/**
 * Is the {@link IUserMessageEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UserMessageDTO extends MessageUnitDTO implements IUserMessageEntity {
	private String               mpc;
    private TradingPartner       sender;
    private TradingPartner       receiver;
    private CollaborationInfo    collabInfo;
    private ArrayList<IProperty> msgProperties = new ArrayList<>();
    private ArrayList<IPayloadEntity>  payloads = new ArrayList<>();

    public UserMessageDTO() {
        super();
    }

    public UserMessageDTO(final IUserMessage sourceUserMessage) {
        super(sourceUserMessage);
        copyFrom(sourceUserMessage);
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
        	sourceUserMessage.getPayloads().forEach(p -> payloads.add(new PayloadDTO(this, p)));
    }

    @Override
	public MessageUnitDTO clone() {
		return new UserMessageDTO(this);
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
        if(!Utils.isNullOrEmpty(msgProps)) 
            msgProps.forEach(p -> this.msgProperties.add(new Property(p)));        
    }

    public void addMessageProperty(final IProperty prop) {
        if (prop != null) 
            this.msgProperties.add(new Property(prop));        
    }

    @Override
    public Collection<IPayloadEntity> getPayloads() {
        return payloads;
    }
}
