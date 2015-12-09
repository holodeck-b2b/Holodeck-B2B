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
package org.holodeckb2b.common.general;

/**
 * Represents a <b>reference to</b> a schema that defines the structure of an object/entity. The referred schema can be 
 * any kind of structure definition, like XML Schema, a database schema, DTD, etc.
 * <p>Corresponds to the information contained in the <code>//eb:UserMessage//eb:PartInfo/eb:Schema</code> element. See
 * section 5.2.2.13 of the ebMS Core Specification for more information.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface ISchemaReference {
    
    /**
     * Gets the location where the schema can be found.
     * <p>Corresponds to the <code>location</code> attribute of the <code>eb:Schema</code> element.
     * 
     * @return  The schema location. This is a required property of the reference and therefor this method SHOULD NOT 
     *          return null
     */
    public String   getLocation();
    
    /**
     * Gets the namespace of the schema.
     * <p>Corresponds to the <code>namespace</code> attribute of the <code>eb:Schema</code> element.
     * 
     * @return  The namespace of the schema if specified,<br>
     *          <code>null</code> otherwise
     */
    public String   getNamespace();
    
    /**
     * Gets the version of the schema.
     * <p>Corresponds to the <code>version</code> attribute of the <code>eb:Schema</code> element.
     * 
     * @return  The version of the schema if specified,<br>
     *          <code>null</code> otherwise
     */
    public String   getVersion();
}
