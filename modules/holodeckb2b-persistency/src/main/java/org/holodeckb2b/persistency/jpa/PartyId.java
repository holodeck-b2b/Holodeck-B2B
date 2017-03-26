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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import org.holodeckb2b.interfaces.general.IPartyId;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store party identifier information as described by the {@link
 * IPartyId} interface. Both the party identifier and its type can have max. 1024 characters.
 * <p>This class is <i>embeddable</i> so the party id meta-data is always stored together with the specific trading
 * partner meta-data it relates to.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 */
@Embeddable
public class PartyId implements IPartyId, Serializable {

    /*
     * Getters and setters
     */

    @Override
    public String getId() {
        return P_ID;
    }

    public void setId(final String id) {
        P_ID = id;
    }

    @Override
    public String getType() {
        return P_TYPE;
    }

    public void setType(final String type) {
        P_TYPE = type;
    }

    /*
     * Constructors
     */
    /**
     * Default constructor
     */
    public PartyId() {}

    /**
     * Creates a <code>PartyId</code> with given data
     *
     * @param pid   The PartyId data to use
     */
    public PartyId(final IPartyId pid) {
        this.P_ID = pid.getId();
        this.P_TYPE = pid.getType();
    }

    /**
     * Create a new <code>PartyId</code> as with the given id without a type.
     *
     * @param id The partyid to use for the new instance
     */
    public PartyId(final String id) {
        this(id, null);
    }

    /**
     * Create a new <code>PartyId</code> as a copy of given {@link IPartyId}.
     *
     * @param id   The id itself
     * @param type The type of the PartyId
     */
    public PartyId(final String id, final String type) {
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
    @Lob
    @Column(length = 1024)
    private String  P_ID;

    /*
     * Type of the party id
     */
    @Lob
    @Column(length = 1024)
    private String  P_TYPE;
}
