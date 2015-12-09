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
package org.holodeckb2b.ebms3.mmd.xml;


import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.common.general.IPartyId;
import org.holodeckb2b.common.general.ITradingPartner;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Represents the <i>From</i> and <i>To</i> elements in the MMD document. The
 * element name to use is defined in {@see PartyInfo} which is the representation
 * of the parent element.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class TradingPartner implements ITradingPartner {
    
    /*
     * When used there must be at least one PartyId child element 
     */
    @ElementList(entry = "PartyId", type = PartyId.class , required = true, inline = true)
    private ArrayList<IPartyId>   partyIds;
    
    /*
     * Optionally a <i>role</i> might be provided
     */
    @Element(name="Role", required = false)
    private String          role;

    /**
     * Default constructor
     */
    public TradingPartner() {}
    
    /**
     * Creates a <code>TradingPartner</code> object with the given data.
     * 
     * @param tp    The data to use
     */
    public TradingPartner(ITradingPartner tp) {
        this.setPartyIds(tp.getPartyIds());
        this.role = tp.getRole();
    }
    
    @Override
    public Collection<IPartyId> getPartyIds() {
        return partyIds;
    }

    public void setPartyIds(Collection<IPartyId> pids) {
        // Copy to list of PartyId object
        if (pids != null && pids.size() > 0) {
            partyIds = new ArrayList<IPartyId>(pids.size());
            for (IPartyId p : pids) 
                partyIds.add(new org.holodeckb2b.ebms3.mmd.xml.PartyId(p));            
        } else
            partyIds = null;
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
    public void setRole(String role) {
        this.role = role;
    }

    
    
    
}
