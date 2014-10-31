/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.mmd.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.general.ITradingPartner;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.common.messagemodel.IUserMessage;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Represents the root element <code>MessageMetaData</code> from the <i>message
 * meta data</i> (MMD for short) XML document that can be used to exchange user 
 * message units between business application and Holodeck B2B.
 * <p>The structure of the MMD documents is defined by XML schema 
 * <i>http://holodeck-b2b.org/schemas/2014/06/mmd</i>. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Root(name = "MessageMetaData",strict = false)
@Namespace(reference="http://holodeck-b2b.org/schemas/2014/06/mmd")
public class MessageMetaData implements IUserMessage {

    /*
     * When the business application submits a user message it is recommended to
     * only supply a <i>RefToMessageId</i> when needed. Therefor, when such 
     * reference is not needed the complete <i>MessageInfo</i> element can be
     * skipped.
     */
    @Element(name = "MessageInfo", required = false)
    private MessageInfo         messageInfo;
    
    @Element(name = "PartyInfo", required = false)
    private PartyInfo           partyInfo;
    
    @Element(name = "CollaborationInfo", required = true)
    private CollaborationInfo   collabInfo;
    
    @ElementList(name = "MessageProperties", entry = "Property", type = Property.class, required = false)
    private ArrayList<IProperty> msgProperties;
    
    @ElementList(name = "PayloadInfo", entry = "PartInfo", type = PartInfo.class, required=true)
    private ArrayList<IPayload>  payloads;  
    
    /**
     * Default constructor to create an empty <code>MessageMetaData</object>.
     */
    public MessageMetaData() {}
    
    /**
     * Create a new <code>MessageMetaData</code> object for the user message unit
     * described by the given {@see IUserMessage} object.
     * 
     * @param msgData       The meta data of the user message unit to create the
     *                      MMD for
     */
    public MessageMetaData(IUserMessage msgData) {
        setMPC(msgData.getMPC());
        setTimestamp(msgData.getTimestamp());
        setMessageId(msgData.getMessageId());
        setRefToMessageId(msgData.getRefToMessageId());
        setSender(msgData.getSender());
        setReceiver(msgData.getReceiver());
        setCollaborationInfo(msgData.getCollaborationInfo());
        setMessageProperties(msgData.getMessageProperties());
        setPayloads(msgData.getPayloads());
    }

    /**
     * Creates a new <code>MessageMetaData</code> object for the user message unit
     * described by the XML document in the specified {@see File}.
     * 
     * @param  mmdFile      A handle to file that contains the meta data
     * @return              A <code>MessageMetaData</code> for the message meta
     *                      data contained in the given file
     * @throws Exception    When the specified file is not found, readable or
     *                      does not contain a MMD document.
     */
    public static MessageMetaData createFromFile(File mmdFile) throws Exception {
        if( !mmdFile.exists() || !mmdFile.canRead())
            // Given file must exist and be readable to be able to read MMD
            throw new Exception("Specified MMD file ["+mmdFile.getAbsolutePath()+"] not found or no permission to read!");
        
        MessageMetaData mmd = null;
        try {
            Serializer  serializer = new Persister();
            mmd = serializer.read(MessageMetaData.class, mmdFile);
        } catch (Exception ex) {
            // The specified file could not be read as an MMD document
            throw new Exception("Problem reading MMD from " + mmdFile.getAbsolutePath(), ex);
        }
                
        return mmd;
    }
    
    /**
     * Writes the MMD document to a file.
     * 
     * @param mmdFile       The path where the MMD document should be saved.
     * @throws IOException  When the MMD document could not be saved to the specified path
     */
    public void writeToFile(File mmdFile) throws IOException {
        if( mmdFile.exists() && !mmdFile.canWrite())
            // If the given file exists, it must be writeable to be able to save the MMD
            throw new IOException("Specified MMD file ["
                                    + mmdFile.getAbsolutePath() + "] already exists but is not writeable!");

        Serializer serializer = new Persister();
        try {
            serializer.write(this, mmdFile);
        } catch (Exception ex) {
            // The MMD could not be saved to the specified file
            throw new IOException("Problem writing MMD to " + mmdFile.getAbsolutePath(), ex);
        }
        
    }
    
    
    @Override
    public String getMPC() {
        if(messageInfo != null)
            return messageInfo.getMpc();
        else
            return null;
    }
    
    public void setMPC(String mpc) {
        if(messageInfo == null)
           messageInfo = new MessageInfo();
        messageInfo.setMpc(mpc);
    }

    @Override
    public ITradingPartner getSender() {
        if(partyInfo != null)
            return partyInfo.getSender();
        else
            return null;
    }

    public void setSender(ITradingPartner sender) {
        if (sender != null) {
            if(partyInfo == null)
                partyInfo = new PartyInfo();
            partyInfo.setSender(sender);
        }
    }
    
    @Override
    public ITradingPartner getReceiver() {
        if(partyInfo != null)
            return partyInfo.getReceiver();
        else
            return null;
    }

    public void setReceiver(ITradingPartner receiver) {
        if (receiver != null) {
            if(partyInfo == null)
                partyInfo = new PartyInfo();
            partyInfo.setReceiver(receiver);
        }
    }
    
    @Override
    public ICollaborationInfo getCollaborationInfo() {
        return collabInfo;
    }
    
    public void setCollaborationInfo(ICollaborationInfo ci) {
        if (ci != null)
            this.collabInfo = new CollaborationInfo(ci);
        else 
            this.collabInfo = null;
    }

    @Override
    public Collection<IProperty> getMessageProperties() {
        return msgProperties;
    }

    public void setMessageProperties(Collection<IProperty> msgProps) {
        if(msgProps != null && msgProps.size() > 0) {
            this.msgProperties = new ArrayList<IProperty>(msgProps.size());
            for(IProperty p : msgProps)
                this.msgProperties.add(new Property(p));
        }
        else
            this.msgProperties = null;
    }
    
    @Override
    public Collection<IPayload> getPayloads() {
        return payloads;
    }

    public void setPayloads(Collection<IPayload> pl) {
        if(pl != null && pl.size() > 0) {
            this.payloads = new ArrayList<IPayload>(pl.size());
            for(IPayload p : pl)
                this.payloads.add(new PartInfo(p));
        }
        else
            this.payloads = null;
    }

    @Override
    public Date getTimestamp() {
        if( messageInfo != null)
            return messageInfo.getTimestamp();
        else
            return null;
    }
    
    public void setTimestamp(Date ts) {
        if( messageInfo == null)
            messageInfo = new MessageInfo();
        messageInfo.setTimestamp(ts);
    }

    @Override
    public String getMessageId() {
        if( messageInfo != null)
            return messageInfo.getMessageId();
        else
            return null;    
    }

    public void setMessageId(String msgId) {
        if( messageInfo == null)
            messageInfo = new MessageInfo();
        messageInfo.setMessageId(msgId);
    }

    @Override
    public String getRefToMessageId() {
        if( messageInfo != null)
            return messageInfo.getRefToMessageId();
        else
            return null;    
    }

    public void setRefToMessageId(String refId) {
        if( messageInfo == null)
            messageInfo = new MessageInfo();
        messageInfo.setRefToMessageId(refId);
    }
}
