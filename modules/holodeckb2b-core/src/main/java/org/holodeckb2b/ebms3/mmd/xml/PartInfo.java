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
package org.holodeckb2b.ebms3.mmd.xml;

import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.common.general.IDescription;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.general.ISchemaReference;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Represents the <code>PartInfo</code> element from the MMD document. This element
 * contains information about a specific payload. There is only one required 
 * information element, the location attribute, and that is the location where 
 * the data of the payload can be found.
 * <p>The payload can be included in the message as a SOAP attachment or be 
 * external, referenced by an URL. When the payload is a XML document it can also 
 * be included in the SOAP body of the ebMS message. Whether a payload should be 
 * included in the message, as attachment or in the SOAP body, or is external is 
 * indicated by the <code>containment</code> attribute. Its default value is false.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartInfo implements IPayload {

    @Element(name="Schema",required = false)
    private SchemaReference     schemaRef;
    
    @Element(name="Description", required = false)
    private Description         description;
    
    @ElementList(name="PartProperties",  entry = "Property", type = Property.class, required = false)
    private ArrayList<IProperty> properties;
    
    @Attribute(name="containment", required = false, empty = "attachment")
    private String  containment;
    
    @Attribute(name="uri", required=false)
    private String  uri;
    
    @Attribute(name="mimeType", required=false)
    private String  mimeType;
    
    @Attribute(name="location")
    private String  location;
    
    /**
     * Default constructor
     */
    public PartInfo() {}
    
    /**
     * Creates a new <code>PartInfo</code> object based on the given data.
     * 
     * @param data  The data to use for the new object
     */
    public PartInfo(IPayload data) {
        this.location = data.getContentLocation();
        this.uri = data.getPayloadURI();
        this.mimeType = data.getMimeType();
        
        setContainment(data.getContainment());
        setSchemaReference(data.getSchemaReference());
        setDescription(data.getDescription());
        setProperties(data.getProperties());
    }
    
    @Override
    public Containment getContainment() {
        if (this.containment != null) {
            if( "body".equals(this.containment.toLowerCase()))
                return Containment.BODY;
            else if( "external".equals(this.containment.toLowerCase()))
                return Containment.EXTERNAL;
        } 
        
        return Containment.ATTACHMENT;
    }

    public void setContainment(Containment containment) {
        this.containment = containment.name().toLowerCase();
    }
    
    @Override
    public String getPayloadURI() {
        return uri;
    }
    
    public void setPayloadURI(String uri) {
        this.uri = uri;
    }

    @Override
    public Collection<IProperty> getProperties() {
        return properties;
    }
    
    public void setProperties(Collection<IProperty> props) {
        // Copy the properties to a new list
        if (props != null && props.size() > 0) {
            this.properties = new ArrayList<IProperty>(props.size());
            for(IProperty p : props)
                this.properties.add(new Property(p));
        } else
            this.properties = new ArrayList<IProperty>();
    }

    @Override
    public IDescription getDescription() {
        return description;
    }
    
    public void setDescription(IDescription descr) {
        if (descr != null )
            this.description = new Description(descr);
        else
            this.description = null;
    }

    @Override
    public ISchemaReference getSchemaReference() {
        return schemaRef;
    }
    
    public void setSchemaReference(ISchemaReference schema) {
        if (schema != null)
            this.schemaRef = new SchemaReference(schema);
        else
            this.schemaRef = null;
    }

    @Override
    public String getContentLocation() {
        return location;
    }
    
    public void setContentLocation(String loc) {
        this.location = loc;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
