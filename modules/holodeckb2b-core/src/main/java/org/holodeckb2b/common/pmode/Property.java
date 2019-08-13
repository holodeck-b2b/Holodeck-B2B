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

import org.holodeckb2b.interfaces.general.IProperty;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the message or part properties.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class Property implements IProperty, Serializable {
	private static final long serialVersionUID = 3344694075984021000L;

    @Element (name = "name")
    private String name;

    @Element (name = "value", required = false)
    private String value;

    @Element (name = "type", required = false)
    private String type;

    /**
     * Default constructor creates a new and empty <code>Property</code> instance.
     */
    public Property() {}

    /**
     * Creates a new <code>Property</code> instance using the parameters from the provided {@link IProperty} object.
     *
     * @param source The source object to copy the parameters from
     */
    public Property(final IProperty source) {
        this(source.getName(), source.getType(), source.getValue());
    }
    
    /**
     * Creates a new <code>Property</code> instance with the given name and value
     *
     * @param name 		The name of the property
     * @param value 	The value of the property
     */
    public Property(final String name, final String value) {
    	this(name, null, value);
    }
    
    /**
     * Creates a new <code>Property</code> instance with the given name, type and value
     *
     * @param name 		The name of the property
     * @param type		The type of the property
     * @param value 	The value of the property
     */
    public Property(final String name, final String type, final String value) {
    	this.name = name;
    	this.type = type;
		this.value = value;
    }    
    
    @Override
    public String getName() {
    	return name;
    }
    
    public void setName(final String name) {
    	this.name = name;
    }

    @Override
    public String getType() {
    	return type;
    }
    
    public void setType(final String type) {
    	this.type = type;
    }
    
    @Override
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
