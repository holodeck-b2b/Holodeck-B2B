package org.holodeckb2b.persistency.inmemory.dto;

import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IPayloadEntity;

public class PayloadDTO implements IPayloadEntity {
	private UserMessageDTO			parent;
	private String                  contentLocation;
    private String                  mimeType;
    private IPayload.Containment    containment;
    private String                  uri;
    private ArrayList<IProperty>    properties = new ArrayList<>();
    private SchemaReference         schemaRef;
    private Description             description;

    public PayloadDTO(UserMessageDTO parent) {
    	this.parent = parent;
    }

    public PayloadDTO(final UserMessageDTO parent, final IPayload source) {
    	this(parent);
    	copyFrom(source);
    }
    
    public void copyFrom(IPayload source) {
    	if (source == null)
    		return;
    	
        this.contentLocation = source.getContentLocation();
        this.mimeType = source.getMimeType();
        this.containment = source.getContainment();
        this.uri = source.getPayloadURI();

        if (!Utils.isNullOrEmpty(source.getProperties()))
        	source.getProperties().forEach(p -> properties.add(new Property(p)));
        
        setSchemaReference(source.getSchemaReference());
        setDescription(source.getDescription());
    }

    public UserMessageDTO getParentUserMessage() {
    	return parent;
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

    public void addProperty(final IProperty prop) {
        if (prop != null) 
            this.properties.add(new Property(prop));        
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

	@Override
	public void removeProperty(IProperty p2r) {
		properties.removeIf(p -> CompareUtils.areEqual(p, p2r));		
	}
}
