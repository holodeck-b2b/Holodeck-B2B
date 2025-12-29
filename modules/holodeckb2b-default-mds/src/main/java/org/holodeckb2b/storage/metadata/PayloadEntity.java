/*
 * Copyright (C) 2023 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata;

import java.util.Collection;

import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;

/**
 * Is the {@link IPayloadEntity} implementation of the default persistency provider of Holodeck B2B.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class PayloadEntity extends JPAObjectProxy<PayloadInfo> implements IPayloadEntity {

    public PayloadEntity(PayloadInfo p) {
    	super(p);
    }

    @Override
    public void loadCompletely() {
    }

    @Override
    public String getPayloadId() {
    	return jpaEntityObject.getPayloadId();
    }

	@Override
	public String getParentCoreId() {
		return jpaEntityObject.getParentCoreId();
	}

	@Override
	public String getPModeId() {
		return jpaEntityObject.getPModeId();
	}

	@Override
	public Direction getDirection() {
		return jpaEntityObject.getDirection();
	}

	@Override
	public Containment getContainment() {
		return jpaEntityObject.getContainment();
	}

	@Override
	public String getPayloadURI() {
		return jpaEntityObject.getPayloadURI();
	}

	@Override
	public Collection<IProperty> getProperties() {
		return jpaEntityObject.getProperties();
	}

	@Override
	public IDescription getDescription() {
		return jpaEntityObject.getDescription();
	}

	@Override
	public ISchemaReference getSchemaReference() {
		return jpaEntityObject.getSchemaReference();
	}

	@Override
	public String getMimeType() {
		return jpaEntityObject.getMimeType();
	}

	@Override
	public void setMimeType(String mt) {
		jpaEntityObject.setMimeType(mt);
	}

	@Override
	public void setPayloadURI(String uri) {
		jpaEntityObject.setPayloadURI(uri);
	}

	@Override
	public void addProperty(IProperty p) {
		jpaEntityObject.addProperty(p);
	}

	@Override
	public void removeProperty(IProperty p) {
		jpaEntityObject.removeProperty(p);
	}

}
