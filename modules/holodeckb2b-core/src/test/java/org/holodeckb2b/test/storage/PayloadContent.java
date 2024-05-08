/*
 * Copyright (C) 2024 The Holodeck B2B Team
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

public class PayloadContent implements IPayloadContent {
	private String	payloadId;
	private byte[]	content;
	
	public PayloadContent(String id) {
		this.payloadId = id;
	}
	
	@Override
	public String getPayloadId() {
		return payloadId;
	}

	@Override
	public InputStream getContent() throws StorageException {
		return content != null ? new ByteArrayInputStream(content) : null;
	}

	@Override
	public OutputStream openStorage() throws StorageException {
		return new ContentOutputStream(this);
	}

	class ContentOutputStream extends ByteArrayOutputStream {
		private PayloadContent	container;
		
		public ContentOutputStream(PayloadContent c) {
			container = c;
		}
		
		@Override
		public void close() throws IOException {
			super.close();
			container.content = toByteArray();
		}
	}
}
