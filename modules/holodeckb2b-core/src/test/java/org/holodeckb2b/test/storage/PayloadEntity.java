/*
 * Copyright (C) 2019 The Holodeck B2B Team
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
package org.holodeckb2b.test.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;

public class PayloadEntity implements IPayloadEntity {
	private Date					lastChange;

	private String					payloadId;
	private String					parentCoreId;
	private String                  pmodeId;
	private Direction				direction;
	private String                  mimeType;
    private IPayload.Containment    containment;
    private String                  uri;
    private ArrayList<IProperty>    properties = new ArrayList<>();
    private SchemaReference         schemaRef;
    private Description             description;

    public PayloadEntity() {
    	this.payloadId = UUID.randomUUID().toString();
    	this.lastChange = new Date();
    }

	public PayloadEntity(final IPayload source) {
		this();
		copyFrom(source);
	}

    public PayloadEntity(final IPayload source, String pmodeId, Direction direction) {
    	this();
    	copyFrom(source);
		this.pmodeId = pmodeId;
		this.direction = direction;
    }

    public PayloadEntity(final UserMessageEntity parent, final IPayload source) {
    	this();
    	copyFrom(source);
    	this.parentCoreId = parent.getCoreId();
    	this.pmodeId = parent.getPModeId();
    	this.direction = parent.getDirection();
    }


    public void copyFrom(IPayload source) {
    	if (source == null)
    		return;
    	if (source instanceof IPayloadEntity) {
    		this.payloadId = ((IPayloadEntity) source).getPayloadId();
    		this.parentCoreId = ((IPayloadEntity) source).getParentCoreId();
    		this.pmodeId = ((IPayloadEntity) source).getPModeId();
			this.direction = ((IPayloadEntity) source).getDirection();
    	}

        this.mimeType = source.getMimeType();
        this.containment = source.getContainment();
        this.uri = source.getPayloadURI();

        if (!Utils.isNullOrEmpty(source.getProperties()))
        	source.getProperties().forEach(p -> properties.add(new Property(p)));

        setSchemaReference(source.getSchemaReference());
        setDescription(source.getDescription());
    }

    @Override
	public PayloadEntity clone() {
    	return new PayloadEntity(this);
    }

	public Date getLastChanged() {
		return lastChange;
	}

	public void setChanged(Date d) {
		this.lastChange = d;
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

    @Override
	public void setPayloadURI(final String uri) {
        this.uri = uri;
    }

    @Override
    public Collection<IProperty> getProperties() {
        return properties;
    }

    @Override
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
    public String getMimeType() {
        return mimeType;
    }

    @Override
	public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

	@Override
	public void removeProperty(IProperty p2r) {
		properties.removeIf(p -> CompareUtils.areEqual(p, p2r));
	}

	@Override
	public String getPayloadId() {
		return payloadId;
	}

	@Override
	public String getParentCoreId() {
		return parentCoreId;
	}

	@Override
	public String getPModeId() {
		return pmodeId;
	}

	@Override
	public Direction getDirection() {
		return direction;
	}
}
