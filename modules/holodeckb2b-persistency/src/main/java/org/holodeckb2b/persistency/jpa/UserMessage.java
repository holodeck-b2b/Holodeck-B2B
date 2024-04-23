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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
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
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is the JPA entity class for storing the meta-data of an ebMS <i>User Message</i> message unit as described by the
 * {@link IUserMessage} interface in Holodeck B2B messaging model.
 * <p>Three queries are defined:
 * <ul><li><i>UserMessage.isDelivered</i> to check whether a User Message is delivered to the <i>Consumer</i>
 *              application, i.e. its current processing state is {@link ProcessingStates#DELIVERED}</li>
 * <li><i>UserMessage.numOfTransmits</i> to retrieve the number of times the message was sent to the receiving MSH, i.e.
 *              the number of times the {@link ProcessingStates#SENDING} occurs as processing state. The result will
 *              also count the attempts that failed on the http level.</li>
 * <li><i>UserMessage.findResponsesTo</i> finds all User Messages that are a response to another User Message, i.e.
 *              which refer to the given message id.</li></ul>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Entity
@Table(name="USER_MESSAGE")
@DiscriminatorValue("USERMSG")
public class UserMessage extends MessageUnit implements IUserMessage, Serializable {

    private enum PartnerType { SENDER, RECEIVER }

    /*
     * Getters and setters
     */
    @Override
    public String getMPC() {
        return MPC;
    }

    public void setMPC(final String mpc) {
        MPC = mpc;
    }

    @Override
    public ITradingPartner getSender() {
        return partners.get(PartnerType.SENDER);
    }

    public void setSender(final ITradingPartner sender) {
        if (sender != null)
            partners.put(PartnerType.SENDER, new TradingPartner(sender));
        else
            partners.remove(PartnerType.SENDER);
    }

    @Override
    public ITradingPartner getReceiver() {
        return partners.get(PartnerType.RECEIVER);
    }

    public void setReceiver(final ITradingPartner receiver) {
        if (receiver != null)
            partners.put(PartnerType.RECEIVER, new TradingPartner(receiver));
        else
            partners.remove(PartnerType.RECEIVER);
    }

    @Override
    public ICollaborationInfo getCollaborationInfo() {
        return collaborationInfo;
    }

    public void setCollaborationInfo(final ICollaborationInfo info) {
        this.collaborationInfo = info != null ? new CollaborationInfo(info) : null;
    }

    @Override
    public Collection<IProperty> getMessageProperties() {
        return properties;
    }

    public void setMessageProperties(final Collection<IProperty> msgProps) {
        if(!Utils.isNullOrEmpty(msgProps)) {
            this.properties = new ArrayList<>(msgProps.size());
            for(final IProperty p : msgProps)
                this.properties.add(new Property(p));
        } else
            this.properties = null;
    }

    public void addMessageProperty(final IProperty prop) {
        if (prop != null) {
            if (this.properties == null)
                this.properties = new ArrayList<>();
            this.properties.add(new Property(prop));
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
        if (this.payloads != null)
            this.payloads.clear();
        else
            this.payloads = new ArrayList<>();
        if (!Utils.isNullOrEmpty(payloads))
            for (IPayload p : payloads)
                this.payloads.add(new Payload(p));
    }

    /**
     * Adds meta-data about one payload to the existing set of payload meta-data.
     *
     * @param p The meta-data on the specific payload
     */
    public void addPayload(final IPayload p) {
        if (p != null) {
            if (payloads == null)
                this.payloads = new ArrayList<>(1);
            this.payloads.add(new Payload(p));
        }
    }

    /*
     * Constructors
     */
    /**
     * Default constructor to initialize as empty meta-data object
     */
    public UserMessage() {
        super();
        this.payloads = new ArrayList<>();
        this.partners = new HashMap<>();
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
        setSender(sourceUserMessage.getSender());
        setReceiver(sourceUserMessage.getReceiver());
        setCollaborationInfo(sourceUserMessage.getCollaborationInfo());
        setMessageProperties(sourceUserMessage.getMessageProperties());
        setPayloads(sourceUserMessage.getPayloads());
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
    @OneToMany(targetEntity = Payload.class, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IPayload>       payloads;

    /*
     * A user message can contain an unlimited number of properties, but they
     * are all specific to one user meesage.
     */
    @ElementCollection(targetClass = Property.class)
    @CollectionTable(name="UM_PROPERTIES")
    private List<IProperty>      properties;
}
