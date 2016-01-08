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
package org.holodeckb2b.ebms3.persistent.dao;

import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.persistency.entities.PartyId;
import org.holodeckb2b.ebms3.persistency.entities.TradingPartner;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Is a <i>data access object</i> class for working with {@see TradingPartner} objects.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class TradingPartnerDAO {
    
    public static TradingPartner createTradingPartner(ITradingPartner tp, EntityManager em) {
        if (tp == null)
            return null; // nothing to create if no information given
        
        TradingPartner  ntp = new TradingPartner();
        
        // Copy info to the entity object
        //
        ntp.setRole(tp.getRole());
        
        for(IPartyId pid : tp.getPartyIds())
            ntp.addPartyId(new PartyId(pid.getId(), pid.getType()));
        
        // Save new TradingPartner to database
        em.persist(ntp);
        
        return ntp;
    }
}
