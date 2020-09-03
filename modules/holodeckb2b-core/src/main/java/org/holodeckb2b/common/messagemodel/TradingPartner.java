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

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Is an in memory only implementation of {@link ITradingPartner} to temporarily store the information on a trading
 * partner that is involved in the exchange of a User Message message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class TradingPartner implements ITradingPartner, Serializable {
	private static final long serialVersionUID = -4399459217276905109L;

	private ArrayList<IPartyId>   partyIds;
    private String                role;

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
        this.role = sourceTradingPartner.getRole();
    }

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
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(final String role) {
        this.role = role;
    }




}
