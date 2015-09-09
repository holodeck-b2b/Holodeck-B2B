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

package org.holodeckb2b.ebms3.persistent.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.general.ITradingPartner;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.general.Property;
import org.holodeckb2b.ebms3.persistent.general.TradingPartner;

/**
 * Is a persistency class representing an ebMS User Message message unit that 
 * is processed by Holodeck B2B. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
@Table(name="USER_MESSAGE")
@DiscriminatorValue("USERMSG")
@NamedQueries({
        @NamedQuery(name="UserMessage.isDelivered",
            query = "SELECT 'true' " 
                    + "FROM UserMessage um JOIN um.states s1 "
                    + "WHERE um.MESSAGE_ID = :msgId "
                    + "AND s1.START = (SELECT MAX(s2.START) FROM um.states s2) " 
                    + "AND s1.NAME = '" + ProcessingStates.DELIVERED + "'"
            ),
        @NamedQuery(name="UserMessage.numOfTransmits",
            query = "SELECT COUNT(s1.NAME) "
                    + "FROM UserMessage um JOIN um.states s1 " 
                    + "WHERE um.MESSAGE_ID = :msgId " 
                    + "AND s1.NAME = '" + ProcessingStates.SENDING + "'"
            ),
        @NamedQuery(name="UserMessage.findForPModesInState",
            query = "SELECT um " +
                    "FROM UserMessage um JOIN um.states s1 " +
                    "WHERE um.PMODE_ID IN :pmodes " +
                    "AND s1.START = (SELECT MAX(s2.START) FROM um.states s2) " +
                    "AND s1.NAME = :state " +
                    "ORDER BY s1.START"  
            ),
        @NamedQuery(name="UserMessage.findResponsesTo",
            query = "SELECT um " +
                    "FROM UserMessage um " +
                    "WHERE um.REF_TO_MSG_ID = :refToMsgId " 
            )
        }
)
public class UserMessage extends MessageUnit implements org.holodeckb2b.common.messagemodel.IUserMessage {
    
    private enum PartnerType { SENDER, RECEIVER }
    
    /*
     * Getters and setters
     */

    @Override
    public String getMPC() {
        return MPC;
    }
    
    public void setMPC(String mpc) {
        // If no MPC is given automatically assign the default one
        if (mpc == null || mpc.isEmpty())
            MPC = Constants.DEFAULT_MPC;
        else
            MPC = mpc;
    }

    @Override
    public ITradingPartner getSender() {
        return partners.get(PartnerType.SENDER);
    }
    
    public void setSender(TradingPartner sender) {
        partners.put(PartnerType.SENDER, sender);
    }

    @Override
    public ITradingPartner getReceiver() {
        return partners.get(PartnerType.RECEIVER);
    }

    public void setReceiver(TradingPartner receiver) {
        partners.put(PartnerType.RECEIVER, receiver);
    }

    @Override
    public ICollaborationInfo getCollaborationInfo() {
        return collaborationInfo;
    }
    
    public void setCollaborationInfo(CollaborationInfo info) {
        this.collaborationInfo = info;
    }

    @Override
    public Collection<IProperty> getMessageProperties() {
        return properties;
    }
    
    public void setMessageProperties(Collection<Property> props) {
        if ( props == null || props.isEmpty()) {
            properties = null;
        } else {
            properties = new ArrayList<IProperty>();
            properties.addAll(props);
        } 
    }

    public void addMessageProperty(Property p) {
        if (properties == null)
            properties = new ArrayList<IProperty>();
        properties.add(p);
    }
    
    @Override
    public Collection<IPayload> getPayloads() {
        return payloads;
    }
    
    public void setPayloads(Collection<Payload> pls) {
        if (payloads == null)
            payloads = new ArrayList<IPayload>();
        payloads.clear();
        payloads.addAll(pls);
    }
    
    public void addPayload(Payload p) {
        if (payloads == null)
            payloads = new ArrayList<IPayload>();
        payloads.add(p);
    }
    
    /*
     * Constructors
     */
    public UserMessage() {
        super();
        
        this.partners = new HashMap<PartnerType, TradingPartner>();
    }
    
    
    /*
     * Fields
     * 
     * NOTES: 
     * 1) The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */
    
    /**
     * If no specific MPC is assigned to the user message the default MPC is assumed.
     */
    private String              MPC = Constants.DEFAULT_MPC;
    
    /*
     * A user message is always associated with two trading partners, one
     * sending and one receiving the message. We use a map with an enumeration
     * to identify whether the partner is the sender or receiver.
     */
    @ElementCollection
    @CollectionTable(name="UM_PARTNERS")
    @MapKeyColumn(name="PARTNERTYPE")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<PartnerType, TradingPartner>      partners;
    
    @Embedded
    private CollaborationInfo   collaborationInfo;
    
    /*
     * The business data is exchanged through payloads included in the user message. So
     * normally each user message will contain one or more payload with the business data.
     * The ebMS spec however allows for user messages without payloads
     */
    @OneToMany(targetEntity = Payload.class, cascade = CascadeType.ALL)
    private List<IPayload>       payloads;
    
    /*
     * A user message can contain an unlimited number of properties, but they 
     * are all specific to one user meesage. 
     */
    @ElementCollection(targetClass = Property.class)
    @CollectionTable(name="UM_PROPERTIES")
    private List<IProperty>      properties;
    
    
    
}
