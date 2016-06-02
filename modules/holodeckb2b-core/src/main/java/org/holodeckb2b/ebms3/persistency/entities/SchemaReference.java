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
import org.holodeckb2b.interfaces.general.ISchemaReference;

/**
 * Is the JPA persistency class for storing schema reference information about a payload. Because the schema reference 
 * is always part of the payload meta-data it is defined as <i>embeddable</i>.
 * 
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Embeddable
public class SchemaReference implements Serializable, ISchemaReference {

    /*
     * Getters and setters
     */
    
    @Override
    public String getLocation() {
        return LOCATION;
    }
    
    public void setLocation(String location) {
        LOCATION = location;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public void setNamespace(String namespace) {
        NAMESPACE = namespace;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    public void setVersion(String version) {
        VERSION = version;
    }
    
    /*
     * Constructors
     */
    public SchemaReference() {}
    
    /**
     * Creates a new SchemaReference object for the given schema
     * 
     * @param   location     The location where the schema can be found
     * @param   namespace    The namespace of the schema
     * @param   version      The version of the schema
     */
    public SchemaReference(String location, String namespace, String version) {
        LOCATION = location;
        NAMESPACE = namespace;
        VERSION = version;
    }
    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    @Lob
    @Column(length = 1024)
    private String  LOCATION;
    
    @Lob
    @Column(length = 1024)
    private String  NAMESPACE;
    
    @Lob
    @Column(length = 1024)
    private String  VERSION;    
}
