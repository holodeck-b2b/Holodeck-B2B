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

import org.holodeckb2b.interfaces.general.IService;
import org.simpleframework.xml.Element;

/**
 * Contains the Service meta-data parameters.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class Service implements IService, Serializable {
	private static final long serialVersionUID = 4235073900459514445L;

    @Element (name = "type", required = false)
    private String type;

    @Element (name = "name", required = true)
    private String name;
    
	/**
     * Default constructor creates a new and empty <code>Service</code> instance.
     */
    public Service() {}
    
    /**
     * Creates a new <code>Service</code> instance with the given name
     *
     * @param name 		The name of the service
     */
    public Service(final String name) {
    	this.name = name;
    }        
    
    /**
     * Creates a new <code>Service</code> instance with the given name and type
     *
     * @param name 		The name of the service
     * @param type		The type of the service
     */
    public Service(final String name, final String type) {
    	this.name = name;
    	this.type = type;
    }        

    /**
     * Creates a new <code>Service</code> instance using the parameters from the provided {@link IService} object.
     *
     * @param source The source object to copy the parameters from
     */
    public Service(final IService source) {
        this.name = source.getName();
        this.type = source.getType();
    }
    
    public void setType(final String type) {
    	this.type = type;
    }
    
    @Override
    public String getType() {
        return this.type;
    }

    public void setName(final String name) {
    	this.name = name;
    }
    
    @Override
    public String getName() {
        return this.name;
    }    
}
