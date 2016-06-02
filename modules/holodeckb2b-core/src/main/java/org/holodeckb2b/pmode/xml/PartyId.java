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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.general.IPartyId;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

/**
 * Represents a <code>PartyId</code> element in the MMD document.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Root
public class PartyId implements IPartyId {
    
    @Text
    private String  id;
    
    @Attribute(required = false)
    private String  type;

    /**
     * Default constructor
     */
    public PartyId() {}
    
    /**
     * Creates a <code>PartyId</code> with given data
     * 
     * @param pid   The PartyId data to use
     */
    public PartyId(IPartyId pid) {
        this.id = pid.getId();
        this.type = pid.getType();
    }
    /**
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the type
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
    
}