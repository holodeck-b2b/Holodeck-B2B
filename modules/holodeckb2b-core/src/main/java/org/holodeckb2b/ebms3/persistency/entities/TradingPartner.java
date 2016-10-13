/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistency.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Is the JPA entity class for storing the meta-data about a trading partner involved in the exchange of an ebMS User
 * Message. It contains the set of <i>PartyId</i>s used to identify the partner and the <i>Role</i> the partner has
 * in the message exchange.
 * <p>Although an [business] entity may be involved in multiple exchanges using the same PartyId and Role, each
 * {@link UserMessage} will have its own two {@link TradingPartner} instances for sender and receiver of the message.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
public class TradingPartner implements Serializable, ITradingPartner {

    /*
     * Getters and setters
     */
    @Override
    public Collection<IPartyId> getPartyIds() {
        return partyIds;
    }

    /**
     * Add a {@link PartyId} to the collection of ids that identifies this trading partner.
     *
     * @param pid    The party id to add to the collection
     */
    public void addPartyId(final PartyId pid) {
        partyIds.add(pid);
    }

    @Override
    public String getRole() {
        return TP_ROLE;
    }

    public void setRole(final String role) {
        TP_ROLE = role;
    }

    /*
     * Constructors
     */

    /**
     * Default constructor
     */
    public TradingPartner() {
        partyIds = new HashSet<>();
    }

    /**
     * Creates a trading partner with the given PartyId and role.
     *
     * <p><b>Note:</b>In this constructor a new persistent object is created (for PartyId).
     * This is possible because it is an embedded object that is automaticly
     * persisted with the TradingPartner object.
     *
     * @param partyId    The PartyId to use for the new trading partner
     * @param role       The role of the new trading partner
     */
    public TradingPartner(final String partyId, final String role) {
        this();

        TP_ROLE = role;
        partyIds.add(new PartyId(partyId));
    }

    /**
     * Creates a trading partner with the given PartyId and role.
     *
     * @param partyId    The PartyId to use for the new trading partner
     * @param role       The role of the new trading partner
     */
    public TradingPartner(final PartyId partyId, final String role) {
        this();

        TP_ROLE = role;
        partyIds.add(partyId);
    }

    /*
     * Fields
     *
     * NOTE: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     */

    /*
     * Technical object id acting as the primary key
     */
    @Id
    @GeneratedValue
    private long    OID;

    /*
     * Role. Field name is changed because ROLE is SQL-99 keyword
     */
    @Lob
    @Column(length = 1024)
    private String  TP_ROLE;

    /*
     * The PartyIds used to identify the trading partner
     */
    @ElementCollection(targetClass = org.holodeckb2b.ebms3.persistency.entities.PartyId.class,
                       fetch = FetchType.EAGER)
    private final Collection<IPartyId>     partyIds;

}
