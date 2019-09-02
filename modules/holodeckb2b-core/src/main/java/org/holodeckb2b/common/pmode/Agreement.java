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

import org.holodeckb2b.interfaces.general.IAgreement;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to a agreement meta-data. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class Agreement implements IAgreement, Serializable {
	private static final long serialVersionUID = 1057302083084053298L;
   
	@Element (name = "name", required = true)
    private String name;

    @Element (name = "type", required = false)
    private String type;

    /**
     * Default constructor, required for SimpleXML
     */
    public Agreement() {}
    
    /**
     * Creates a new <code>Agreement</code> instance with the given name and without a type.
     *
     * @param name  The name to use for the new Agreement object
     */
    public Agreement(final String name) {
        this.name = name;
    }

    /**
     * Creates a new <code>Agreement</code> instance with the given name and type.
     *
     * @param name  The name to use for the new Agreement object
     * @param type  The type to use for the new Agreement object
     */
    public Agreement(final String name, final String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Creates a new <code>Agreement</code> instance using the parameters from the provided {@link IAgreement} object.
     *
     * @param source The source object to copy the parameters from
     */
    public Agreement(IAgreement source) {
        this.name = source.getName();
        this.type = source.getType();
    }

    /**
     * Gets the agreement name.
     *
     * @return The agreement name
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Gets the agreement type.
     *
     * @return The agreement type
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * Set the name of the agreement.
     * @param name The agreement name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set the type of the agreement.
     * @param type The agreement type to set
     */
    public void setType(final String type) {
        this.type = type;
    }
}
