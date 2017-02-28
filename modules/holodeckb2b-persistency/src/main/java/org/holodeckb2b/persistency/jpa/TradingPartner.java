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
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Is the JPA entity class used to store the meta-data about the <i>trading partners</i> involved in the exchange of an
 * ebMS User Message and which meta-data is contained in the ebMS header. See the {@link IPayload} interface from the
 * Holodeck B2B messaging model.
 * <p>Although an [business] entity may be involved in multiple exchanges using the same PartyId and Role, each
 * {@link UserMessage} will have its own two <code>TradingPartner</code> instances for sender and receiver.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Entity
public class TradingPartner implements ITradingPartner, Serializable {

    /*
     * Getters and setters
     */
    @Override
    public Collection<IPartyId> getPartyIds() {
        return partyIds;
    }

    /**
     * Sets the party identifiers used by this trading partner.
     *
     * @param pids  A collection of party identifiers
     */
    public void setPartyIds(final Collection<IPartyId> pids) {
        // Copy to list of PartyId object
        if (!Utils.isNullOrEmpty(pids)) {
            partyIds = new ArrayList<>(pids.size());
            for (final IPartyId p : pids)
                partyIds.add(new PartyId(p));
        } else
            partyIds = null;
    }

    /**
     * Adds a party identifier to the current set of identifiers
     *
     * @param pid   The party id to add
     */
    public void addPartyId(final IPartyId pid) {
        if (pid != null) {
            if (partyIds == null)
                partyIds = new ArrayList<>();
            partyIds.add(new PartyId(pid));
        }
    }

    /**
     * @return the role
     */
    @Override
    public String getRole() {
        return TP_ROLE;
    }

    /**
     * @param role the role to set
     */
    public void setRole(final String role) {
        this.TP_ROLE = role;
    }
    /*
     * Constructors
     */
    /**
     * Default constructor
     */
    public TradingPartner() {}

    /**
     * Creates a new <code>TradingPartner</code> object with the given data.
     *
     * @param sourceTradingPartner    The data to use
     */
    public TradingPartner(final ITradingPartner sourceTradingPartner) {
        this.setPartyIds(sourceTradingPartner.getPartyIds());
        this.TP_ROLE = sourceTradingPartner.getRole();
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
    @ElementCollection(targetClass = org.holodeckb2b.persistency.jpa.PartyId.class,
                       fetch = FetchType.EAGER)
    private Collection<IPartyId>     partyIds;

}
