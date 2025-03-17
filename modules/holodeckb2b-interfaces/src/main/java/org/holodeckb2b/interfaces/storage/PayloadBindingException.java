/*
 * Copyright (C) 2025 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.storage;

/**
 * Indicates a problem with the binding of a payload to a User Message, for example when a submitted User Message refers
 * to a payload that is already bound to another User Message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  8.0.0
 * @see IMetadataStorageProvider
 */
public class PayloadBindingException extends StorageException {
	private static final long serialVersionUID = 664316208118322961L;

	/**
	 * The payload that caused the problem.
	 */
	private final IPayloadEntity payload;

	/**
	 * Creates a new PayloadBindingException to indicate that the referenced payload is missing.
	 *
	 * @param missingPayloadId	the referenced PayloadId for which no payload data is found
	 */
	public PayloadBindingException(final String missingPayloadId) {
		super("No payload data found for payloadId: " + missingPayloadId);
		this.payload = null;
	}

	/**
	 * Creates a new PayloadBindingException with the given description and the payload that caused the problem.
	 *
	 * @param payload		The payload that caused the problem
	 * @param description   Description of whether the payload is already or still bound
	 */
	public PayloadBindingException(final IPayloadEntity payload, final String description) {
		super(description);
		this.payload = payload;
	}

	/**
	 * Gets the meta-data of the payload that caused the problem. Using the {@link IPayloadEntity#getParentCoreId()} the
	 * User Message message unit to which the payload is bound can be retrieved.
	 *
	 * @return {@link IPayloadEntity} with the meta-data of the payload that caused the problem or<br/>
	 *								  <code>null</code> if the problem is a missing payload.
	 */
	public IPayloadEntity getPayload() {
		return payload;
	}
}
