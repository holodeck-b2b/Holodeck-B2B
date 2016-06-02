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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.general.IProperty;
import org.simpleframework.xml.Element;

/**
 * Implements a Property class with name value pairs.
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class Property implements IProperty {
    
    @Element (name = "name")
    private String name;
    
    @Element (name = "value", required = false)
    private String value;
    
    // Type cannot appear in the message since it then would be 
    // no longer schema compliant. See also issue database at the Oasis organisation.
    // Type is added here so that is is compliant with the specification, since the spec
    // does describe the type.
    private String type;
    
    /**
     * Default constructor
     */
    public Property() {}
    
    
    /**
     * Gets the type of the property
     * @return The type of property as string
     */
    @Override
    public String getType() {
        return this.type;
    }
 
    /**
     * Gets the name of the property
     * @return The name of property as string
     */
    @Override
    public String getName() {
        return this.name;
    }
    
    /**
     * Gets the value of the property
     * @return The value of property as string
     */
    @Override
    public String getValue() {
        return this.value;
    }
    
}
