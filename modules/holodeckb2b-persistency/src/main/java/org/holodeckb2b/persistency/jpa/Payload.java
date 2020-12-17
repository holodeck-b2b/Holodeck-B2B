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
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is the JPA entity class used to store the meta-data about the <i>payloads</i> contained in an ebMS User Message as
 * described by the {@link IPayload} interface from the Holodeck B2B messaging model.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since  3.0.0
 */
@Entity
@Table(name="PAYLOAD")
public class Payload implements IPayload, Serializable {

    /*
     * Getters and setters
     */
    @Override
    public Containment getContainment() {
        return CONTAINMENT;
    }

    public void setContainment(final Containment containment) {
        CONTAINMENT = containment;
    }

    @Override
    public String getPayloadURI() {
        return URI;
    }

    public void setPayloadURI(final String payloadURI) {
        URI = payloadURI;
    }

    @Override
    public Collection<IProperty> getProperties() {
        return properties;
    }

    public void setProperties(final Collection<IProperty> props) {
        // Copy the properties to a new list
        this.properties = new ArrayList<>(props != null ? props.size() : 0);
        if (!Utils.isNullOrEmpty(props)) {
            for(final IProperty p : props)
                this.properties.add(new Property(p));
        }
    }

    public void addProperty(final IProperty p) {
        if( properties == null)
            properties = new ArrayList<>();
        properties.add(new Property(p));
    }

    @Override
    public IDescription getDescription() {
        return description;
    }

    public void setDescription(final IDescription descr) {
        description = descr != null ? new Description(descr) : null;
    }

    @Override
    public ISchemaReference getSchemaReference() {
        return schemaRef;
    }

    public void setSchemaReference(final ISchemaReference schemaRef) {
        this.schemaRef = schemaRef != null ? new SchemaReference(schemaRef) : null;
    }

    @Override
    public String getContentLocation() {
        return FILE_LOCATION;
    }

    public void setContentLocation(final String location) {
        FILE_LOCATION = location;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    public void setMimeType(final String mimeType) {
        MIME_TYPE = mimeType;
    }

    /*
     * Constructors
     */
    /**
     * Default constructor creates empty object
     */
    public Payload() {
        this.properties = new ArrayList<>();
    }

    /**
     * Creates a new <code>PartInfo</code> object using the source from the given source as source
     *
     * @param source  The source to use for the new object
     */
    public Payload(final IPayload source) {
        this.FILE_LOCATION = source.getContentLocation();
        this.MIME_TYPE = source.getMimeType();
        this.CONTAINMENT = source.getContainment();
        this.URI = source.getPayloadURI();

        setProperties(source.getProperties());
        setSchemaReference(source.getSchemaReference());
        setDescription(source.getDescription());
    }

    /*
     * Fields
     *
     * NOTE: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     */

    /*
     * Technical object id acting as the primary key
     */
    @Id
    @GeneratedValue
    private long    OID;

    @ElementCollection(targetClass = Property.class, fetch = FetchType.EAGER)
    @CollectionTable(name="PL_PROPERTIES")
    private Collection<IProperty>   properties;

    @Embedded
    private Description         description;

    @Embedded
    private SchemaReference     schemaRef;

    private String              URI;

    private String              FILE_LOCATION;

    private String              MIME_TYPE;

    @Enumerated(EnumType.STRING)
    private Containment         CONTAINMENT;
}
