/*
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
package org.holodeckb2b.storage.payloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.StorageException;

/**
 * Is the default Payload Storage Provider's implementation of {@link IPayloadContent}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class PayloadContent implements IPayloadContent {
	/**
	 * The unique identifier assigned to this payload
	 */
	private final String payloadId;
	/**
	 * The file that containing the payload's content
	 */
	private final File	 contentPath;
	/**
	 * The stream used to write the payload's content
	 */
	private ContentOutputStream writeStream;

	/**
	 * Creates a new instance with the given PayloadId and path to the actual data.
	 *
	 * @param payloadId	The unique identifier assigned to this payload
	 * @param path		The file containing the payload's content
	 */
	public PayloadContent(String payloadId, File path) {
		this.payloadId = payloadId;
		this.contentPath = path;
	}

	@Override
	public String getPayloadId() {
		return payloadId;
	}

	@Override
	public InputStream getContent() throws StorageException {
		if (writeStream != null && !writeStream.closed)
			// Content is still written, so not available for reading right now
			return null;
		else
			try {
				return new FileInputStream(contentPath);
			} catch (FileNotFoundException ioError) {
				// No content has yet been written
				return null;
			}
	}

	@Override
	public OutputStream openStorage() throws StorageException {
		if (writeStream == null) {
			if (contentPath.exists())
				throw new StorageException("The payload data is already	stored");
			try {
				writeStream = new ContentOutputStream(contentPath);
			} catch (FileNotFoundException ioError) {
				throw new StorageException("Could not open payload file: " + contentPath.toString(), ioError);
			}
		} else if (writeStream.closed) {
			throw new StorageException("The output stream is already closed");
		}
		return writeStream;
	}

	/**
	 * Is an extented version of a {@link java.io.FileOutputStream} that has an indicator if the stream is closed.
	 */
	class ContentOutputStream extends FileOutputStream {
		/**
		 * Indicator is the stream is closed
		 */
		private boolean closed = false;

		/**
		 * Opens the the stream for writing to the specified file.
		 *
		 * @see java.io.FileOutputStream#FileOutputStream(java.io.File)
		 */
		public ContentOutputStream(File file) throws FileNotFoundException {
			super(file);
		}

		@Override
		public void close() throws IOException {
			super.close();
			closed = true;
		}
	}

}
