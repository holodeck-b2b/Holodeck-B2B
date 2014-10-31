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
package org.holodeckb2b.ebms3.mmd.xml;

import org.holodeckb2b.common.general.IProperty;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Represents a <code>Property</code> element in the MMD document.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Property implements IProperty {
    
    @Text
    private String  value;
            
    @Attribute(name = "name", required = true)
    private String  name;
    
    @Attribute(name = "type", required = false)
    private String  type;
    
    /**
     * Default constructor
     */
    public Property() {}
    
    /**
     * Creates a new <code>Property</code> object based on the given data
     * 
     * @param p     The property info to use
     */
    public Property(IProperty p) {
        this.value = p.getValue();
        this.name = p.getName();
        this.type = p.getType();                
    }

    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}
