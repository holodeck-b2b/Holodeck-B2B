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
package org.holodeckb2b.ebms3.mmd.xml;

import org.holodeckb2b.common.general.ISchemaReference;
import org.simpleframework.xml.Attribute;

/**
 * Represents the <code>Schema</code> element from a MMD document. The information
 * contained in this element is intended for use by the business applications that
 * handle the payload. It is not used by Holodeck B2B in processing the message. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SchemaReference implements ISchemaReference {

    @Attribute(name="namespace", required = false)
    private String  namespace;
    
    @Attribute(name="version", required = false)
    private String  version;
    
    @Attribute(name="location")
    private String  location;

    /**
     * Default constructor
     */
    public SchemaReference() {}
    
    /**
     * Create a new <code>SchemaReference</code> object based on the given schema
     * reference data.
     * 
     * @param data  The data to use for the new object
     */
    public SchemaReference(ISchemaReference data) {
        this.location = data.getLocation();
        this.version = data.getVersion();
        this.namespace = data.getNamespace();
    }
    
    @Override
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
}
