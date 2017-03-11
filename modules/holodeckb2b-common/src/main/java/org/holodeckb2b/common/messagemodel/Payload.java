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

import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is an in memory only implementation of {@link IPayload} to temporarily store the <b>meta-data</b> of a payload that
 * is contained in a User Message message unit. The actual content of the payload is stored by the Holodeck B2B Core on
 * the file system.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class Payload implements IPayload {

    private String                  contentLocation;
    private String                  mimeType;
    private IPayload.Containment    containment;
    private String                  uri;
    private ArrayList<IProperty>    properties;
    private SchemaReference         schemaRef;
    private Description             description;

    /**
     * Default constructor creates empty object
     */
    public Payload() {}

    /**
     * Creates a new <code>PartInfo</code> object using the source from the given source as source
     *
     * @param source  The source to use for the new object
     */
    public Payload(final IPayload source) {
        this.contentLocation = source.getContentLocation();
        this.mimeType = source.getMimeType();
        this.containment = source.getContainment();
        this.uri = source.getPayloadURI();

        setProperties(source.getProperties());
        setSchemaReference(source.getSchemaReference());
        setDescription(source.getDescription());
    }

    @Override
    public Containment getContainment() {
        return containment;
    }

    public void setContainment(final Containment containment) {
        this.containment = containment;
    }

    @Override
    public String getPayloadURI() {
        return uri;
    }

    public void setPayloadURI(final String uri) {
        this.uri = uri;
    }

    @Override
    public Collection<IProperty> getProperties() {
        return properties;
    }

    public void setProperties(final Collection<IProperty> props) {
        // Copy the properties to a new list
        if (!Utils.isNullOrEmpty(props)) {
            this.properties = new ArrayList<>(props.size());
            for(final IProperty p : props)
                this.properties.add(new Property(p));
        } else
            this.properties = null;
    }

    /**
     * Adds a property to the set of properties.
     *
     * @param prop The property to add
     */
    public void addProperty(final IProperty prop) {
        if (prop != null) {
            if (properties == null)
                this.properties = new ArrayList<>();
            this.properties.add(new Property(prop));
        }
    }

    @Override
    @Deprecated
    public Description getDescription() {
        return description;
    }

    public void setDescription(final IDescription descr) {
        this.description = descr != null ? new Description(descr) : null;
    }

    @Override
    public SchemaReference getSchemaReference() {
        return schemaRef;
    }

    public void setSchemaReference(final ISchemaReference schema) {
        this.schemaRef = schema != null ? new SchemaReference(schema) : null;
    }

    @Override
    public String getContentLocation() {
        return contentLocation;
    }

    public void setContentLocation(final String location) {
        this.contentLocation = location;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }
}
