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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

public class InMemoryPSProvider implements IPayloadStorageProvider {

	private Set<PayloadContent>		payloads = Collections.synchronizedSet(new HashSet<PayloadContent>());
	
	@Override
	public String getName() {
		return "In Memory Test Payload Storage Provider";
	}

	@Override
	public void init(IConfiguration config) throws StorageException {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}
	
	public void clear() {
		payloads.clear();
	}
	
	public int getPayloadCount() {
		return payloads.size();
	}
	
	public boolean exists(String payloadId) {
		return payloads.parallelStream().anyMatch(p -> payloadId.equals(p.getPayloadId()));
	}

	@Override
	public IPayloadContent createNewPayloadStorage(String payloadId, IPMode pmode, Direction direction) {
		PayloadContent p = new PayloadContent(payloadId);
		payloads.add(p);
		return p;
	}

	@Override
	public IPayloadContent getPayloadContent(String payloadId) throws StorageException {
		return payloads.stream().filter(p -> p.getPayloadId().equals(payloadId)).findFirst().orElse(null);
	}

	@Override
	public void removePayloadContent(String payloadId) throws StorageException {
		payloads.removeIf(p -> p.getPayloadId().equals(payloadId));
	}

}
