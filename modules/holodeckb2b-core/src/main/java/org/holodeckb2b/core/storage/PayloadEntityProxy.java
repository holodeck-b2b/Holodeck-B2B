/**
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is a proxy to the {@link IPayloadEntity} object managed by the {@link IMetadataStorageProvider} that also implements
 * the {@link #getContent()} method to access the payload's content.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public class PayloadEntityProxy implements IPayloadEntity {
	/**
	 * The meta-data of the payload, as managed by the {@link IMetadataStorageProvider}
	 */
	private IPayloadEntity  source;
	/**
	 * The content of the payload, as managed by the {@link IPayloadStorageProvider}
	 */
	private IPayloadContent	content;

	PayloadEntityProxy(IPayloadEntity source) {
		if (source instanceof PayloadEntityProxy)
			throw new IllegalArgumentException("Cannot create proxy of proxy");
		else
			this.source = source;
			this.content = null;
	}

	IPayloadEntity getSource() {
		return source;
	}

	/**
	 * Sets the related {@link IPayloadContent} managed by the <i>Payload Storage Provider</i>.
	 *
	 * @param content	the managed content
	 * @throws IllegalStateException when the content was already set
	 */
	void setContent(IPayloadContent content) throws IllegalStateException {
		if (this.content != null)
			throw new IllegalStateException("Content already set");
		this.content = content;
	}

	/**
	 * Indicates whether the content object for this payload has been loaded from the <i>Payload Storage Provider</i>
	 * and therefore it is safe to call {@link #getContent()}.
	 *
	 * @return 	<code>true</code> if the content is available,<br/><code>false</code> if not
	 */
	boolean isContentAvailable() {
		return content != null;
	}

	@Override
	public InputStream getContent() throws IOException {
		try {
			if (content == null)
				// The payload content has not been loaded yet, use provider to get access
				content = ((QueryManager) HolodeckB2BCore.getQueryManager()).retrievePayloadContent(getPayloadId());

			return content.getContent();
		} catch (StorageException e) {
			throw new IOException("Could not open payload content", e);
		}
	}

	@Override
	public Containment getContainment() {
		return source.getContainment();
	}

	@Override
	public String getPayloadURI() {
		return source.getPayloadURI();
	}

	@Override
	public Collection<IProperty> getProperties() {
		return source.getProperties();
	}

	@Override
	public IDescription getDescription() {
		return source.getDescription();
	}

	@Override
	public ISchemaReference getSchemaReference() {
		return source.getSchemaReference();
	}

	@Override
	public String getMimeType() {
		return source.getMimeType();
	}

	@Override
	public String getPayloadId() {
		return source.getPayloadId();
	}

	@Override
	public String getParentCoreId() {
		return source.getParentCoreId();
	}

	@Override
	public void setParentCoreId(String coreId) {
		source.setParentCoreId(coreId);
	}

	@Override
	public void setMimeType(String mt) {
		source.setMimeType(mt);
	}

	@Override
	public void setPayloadURI(String uri) {
		source.setPayloadURI(uri);
	}

	@Override
	public void addProperty(IProperty p) {
		source.addProperty(p);

	}

	@Override
	public void removeProperty(IProperty p) {
		source.removeProperty(p);
	}
}
