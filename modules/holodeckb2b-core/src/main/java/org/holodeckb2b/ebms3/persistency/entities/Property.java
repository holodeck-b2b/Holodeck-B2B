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
package org.holodeckb2b.ebms3.persistency.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Is the JPA embeddable persistency class for storing a <i>Property</i> consisting of a name value pair and (optional) 
 * type indication. As a property is not a very useful entity on its own but always related to another entity (a message 
 * or payload) it is defined as <i>embeddable</i>.
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
    public Property() {}
    
    /**
     * Create a new <code>Property</code> for the given name value pair.
     * 
     * @param name   The name of the property
     * @param value  The value of the property
     */
    public Property(String name, String value) {
        NAME = name;
        VALUE = value;
    }

    /**
     * Create a new <code>Property</code> for the given name value pair and type.
     * 
     * @param name   The name of the property
     * @param value  The value of the property
     * @param type   The type of the property
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
    @Lob
    @Column(length = 1024)
    private String  NAME;
    
    @Lob
    @Column(length = 1024)
    private String  VALUE;
    
    @Lob
    @Column(length = 1024)
    private String  TYPE;    
    
}
