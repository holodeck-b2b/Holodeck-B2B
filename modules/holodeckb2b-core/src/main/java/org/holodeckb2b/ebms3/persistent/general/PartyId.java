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

package org.holodeckb2b.ebms3.persistent.general;

import java.io.Serializable;
import javax.persistence.Embeddable;
import org.holodeckb2b.common.general.IPartyId;

/**
 * Is a persistency class for PartyIds. As a PartyId is not a very useful entity
 * on its own but always related to another entity (a business partner in some role) 
 * it is defined as an <code>Embedable</code> class.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Embeddable
public class PartyId implements Serializable, IPartyId {

    /*
     * Getters and setters
     */
    
    @Override
    public String getId() {
        return P_ID;
    }

    public void setId(String id) {
        P_ID = id;
    }
    
    @Override
    public String getType() {
        return P_TYPE;
    }
    
    public void setType(String type) {
        P_TYPE = type;
    }
    
    /*
     * Constructors
     */
    public PartyId() {}

    /**
     * Create a new <code>PartyId</code> as with the given id without a type.
     * 
     * @param id The partyid to use for the new instance
     */
    public PartyId(String id) {
        P_ID = id;
    }

    /**
     * Create a new <code>PartyId</code> as a copy of given {@link IPartyId}.
     * 
     * @param id   The id itself
     * @param type The type of the PartyId
     */
    public PartyId(String id, String type) {
        P_ID = id;
        P_TYPE = type;
    }
    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    
    /*
     * The party id itself, REQUIRED
     */
    private String  P_ID;
    
    /*
     * Type of the party id
     */
    private String  P_TYPE;
}
