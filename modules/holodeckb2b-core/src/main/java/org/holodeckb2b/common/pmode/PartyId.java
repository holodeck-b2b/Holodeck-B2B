/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;

import org.holodeckb2b.interfaces.general.IPartyId;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Contains the parameters related to the PartyIds of the involved trading partner. This class is part of the generic
 * P-Mode implementation that can be initialized using another P-Mode.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class PartyId implements IPartyId, Serializable {
	private static final long serialVersionUID = 7490054765182562610L;

    @Text
    private String  id;

    @Attribute(required = false)
    private String  type;

    /**
     * Default constructor creates a new and empty <code>PartyId</code> instance.
     */
    public PartyId() {
    }

    /**
     * Creates a new <code>PartyId</code> instance with the given identifier and type.
     *
     * @param id    The identifier for the new instance
     * @param type  The type for the new instance
     */
    public PartyId(String id, String type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Creates a new <code>PartyId</code> instance using the parameters from the provided {@link IPartyId}  object.
     *
     * @param source The source object to copy the parameters from
     */
    public PartyId(final IPartyId source) {
        this.id = source.getId();
        this.type = source.getType();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
