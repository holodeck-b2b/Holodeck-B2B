/**
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import org.holodeckb2b.interfaces.general.ISchemaReference;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store the schema reference of a payload as described by the
 * {@link ISchemaReference} interface in the Holodeck B2B messaging model.
 * <p>This class is <i>embeddable</i> as the schema reference meta-data is always specific to one payload of a User
 * Message.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Embeddable
public class SchemaReference implements ISchemaReference, Serializable {

    /*
     * Getters and setters
     */

    @Override
    public String getLocation() {
        return LOCATION;
    }

    public void setLocation(final String location) {
        LOCATION = location;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public void setNamespace(final String namespace) {
        NAMESPACE = namespace;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    public void setVersion(final String version) {
        VERSION = version;
    }

    /*
     * Constructors
     */
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
        this(source.getLocation(), source.getNamespace(), source.getVersion());
    }

    /**
     * Create a new <code>SchemaReference</code> object based with the given namespace.
     *
     * @param namespace The namespace of the reference schema
     */
    public SchemaReference(final String namespace) {
        this(null, namespace, null);
    }

    /**
     * Create a new <code>SchemaReference</code> object based with the given namespace and location
     *
     * @param namespace The namespace of the reference schema
     * @param location  The location where the schema can be found
     */
    public SchemaReference(final String namespace, final String location) {
        this(location, namespace, null);
    }

    /**
     * Creates a new SchemaReference object for the given schema
     *
     * @param   location     The location where the schema can be found
     * @param   namespace    The namespace of the schema
     * @param   version      The version of the schema
     */
    public SchemaReference(final String location, final String namespace, final String version) {
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
