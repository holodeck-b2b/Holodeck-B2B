/**
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
package org.holodeckb2b.storage.metadata.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;

/**
 * Is the JPA entity class for storing the meta-data of an ebMS <i>User Message</i> message unit as described by the
 * {@link IUserMessageEntity} interface in Holodeck B2B persistency model.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Entity
@Table(name="USER_MESSAGE")
@DiscriminatorValue("USERMSG")
public class UserMessage extends MessageUnit {
	private static final long serialVersionUID = 2076775451055156430L;

	private enum PartnerType { SENDER, RECEIVER }

    /*
     * Getters and setters
     */
    public String getMPC() {
        return MPC;
    }

    public ITradingPartner getSender() {
        return partners.get(PartnerType.SENDER);
    }

    public ITradingPartner getReceiver() {
        return partners.get(PartnerType.RECEIVER);
    }

    public ICollaborationInfo getCollaborationInfo() {
        return collaborationInfo;
    }

    public Collection<IProperty> getMessageProperties() {
        return properties;
    }

    public Collection<PayloadInfo> getPayloads() {
        return payloads;
    }

    public void addPayload(PayloadInfo payload) {
    	if (payloads == null)
			payloads = new ArrayList<>();
	    payloads.add(payload);
    }

    /*
     * Constructors
     */
    /**
     * Default constructor to initialize as empty meta-data object
     */
    public UserMessage() {
        super();
        this.partners = new HashMap<>();
        this.properties = new ArrayList<>();
    }

    /**
     * Create a new <code>UserMessage</code> object for the user message unit described by the given
     * {@link IUserMessage} object.
     *
     * @param sourceUserMessage   The meta data of the user message unit to copy to the new object
     */
    public UserMessage(final IUserMessage sourceUserMessage) {
        super(sourceUserMessage);
        this.partners = new HashMap<>();

        if (sourceUserMessage == null)
            return;

        this.MPC = sourceUserMessage.getMPC();
        if (sourceUserMessage.getSender() != null)
        	this.partners.put(PartnerType.SENDER, new TradingPartner(sourceUserMessage.getSender()));
        if (sourceUserMessage.getReceiver() != null)
        	this.partners.put(PartnerType.RECEIVER, new TradingPartner(sourceUserMessage.getReceiver()));
        if (sourceUserMessage.getCollaborationInfo() != null)
			this.collaborationInfo = new CollaborationInfo(sourceUserMessage.getCollaborationInfo());
        Collection<IProperty> srcMsgProps = sourceUserMessage.getMessageProperties();
        if (!Utils.isNullOrEmpty(srcMsgProps)) {
        	this.properties = new ArrayList<>(srcMsgProps.size());
        	srcMsgProps.forEach(p -> this.properties.add(new Property(p)));
        }


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
    @Lob
    @Column(length = 1024)
    private String              MPC = EbMSConstants.DEFAULT_MPC;

    /**
     * A user message is always associated with two trading partners, one sending and one receiving the message. We use
     * a map with an enumeration to identify whether the partner is the sender or receiver.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name="UM_PARTNERS")
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
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<PayloadInfo>       payloads;

    /*
     * A user message can contain an unlimited number of properties, but they
     * are all specific to one user meesage.
     */
    @ElementCollection(targetClass = Property.class)
    @CollectionTable(name="UM_PROPERTIES")
    private List<IProperty>      properties;
}
