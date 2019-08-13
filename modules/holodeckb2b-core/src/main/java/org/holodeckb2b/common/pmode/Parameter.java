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

import org.simpleframework.xml.Element;

/**
 * Represents the P-Mode parameters that contain settings used by delivery methods, custom validators and/or event
 * handlers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class Parameter implements Serializable {
	private static final long serialVersionUID = 7787023057300928752L;

    @Element (name = "name", required = true)
    private String name;

    @Element (name = "value", required = true)
    private String value;

    /**
     * Default constructor, required for SimpleXML
     */   
    Parameter() {}
    
    /**
     * Creates a new instance with the given name and value.
     *
     * @param name   The name to use for the new instance
     * @param value  The type to use for the new instance
     */
    public Parameter(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
