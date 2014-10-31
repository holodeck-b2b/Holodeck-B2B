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
import org.holodeckb2b.common.general.IProperty;

/**
 * Is a persistency class for a property consisting of a name value pair and 
 * a (optional) type indication. As a property is not a very useful entity on 
 * its own but always related to another entity (a message or payload) it is 
 * defined as an <code>Embedable</code> class.
 * 
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Embeddable
public class Property implements Serializable, IProperty {

    /*
     * Getters and setters
     */
    
    @Override
    public String getName() {
        return NAME;
    }

    public void setName(String name) {
        NAME = name;
    }
    
    @Override
    public String getValue() {
        return VALUE;
    }
    
    public void setValue(String value) {
        VALUE = value;
    }

    @Override
    public String getType() {
        return TYPE;
    }
    
    public void setType(String type) {
        TYPE = type;
    }
    
    /*
     * Constructors
     */
    public Property() {};
    
    /**
     * Create a new <code>Property</code> for the given name value pair.
     * 
     * @param String   The name of the property
     * @param String   The value of the property
     */
    public Property(String name, String value) {
        NAME = name;
        VALUE = value;
    }

    /**
     * Create a new <code>Property</code> for the given name value pair and type.
     * 
     * @param String   The name of the property
     * @param String   The value of the property
     * @param String   The type of the property
     */
    public Property(String name, String value, String type) {
        NAME = name;
        VALUE = value;
        TYPE = type;
    }
    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    private String  NAME;
    
    private String  VALUE;
    
    private String  TYPE;    
    
}
