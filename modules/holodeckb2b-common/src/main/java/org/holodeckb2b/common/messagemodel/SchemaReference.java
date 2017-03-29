/**
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.messagemodel;

import org.holodeckb2b.interfaces.general.ISchemaReference;

/**
 * Is an in memory only implementation of {@link ISchemaReference} to temporarily store the information about the schema
 * defining the structure of a payload from a User Message message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 */
public class SchemaReference implements ISchemaReference {

    private String  namespace;
    private String  version;
    private String  location;

    /**
     * Default constructor cresates empty object
     */
    public SchemaReference() {}

    /**
     * Create a new <code>SchemaReference</code> object based using the source from the given source object.
     *
     * @param source  The source to use for the new object
     */
    public SchemaReference(final ISchemaReference source) {
        this.location = source.getLocation();
        this.version = source.getVersion();
        this.namespace = source.getNamespace();
    }

    /**
     * Create a new <code>SchemaReference</code> object based with the given namespace.
     *
     * @param namespace The namespace of the reference schema
     */
    public SchemaReference(final String namespace) {
        this.namespace = namespace;
    }

    /**
     * Create a new <code>SchemaReference</code> object based with the given namespace and location
     *
     * @param namespace The namespace of the reference schema
     * @param location  The location where the schema can be found
     */
    public SchemaReference(final String namespace, final String location) {
        this.namespace = namespace;
        this.location = location;
    }

    @Override
    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

}
