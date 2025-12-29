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
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.StorageException;

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
	public IPayloadContent createNewPayloadStorage(IPayloadEntity p) {
		PayloadContent pl = new PayloadContent(p.getPayloadId());
		payloads.add(pl);
		return pl;
	}

	@Override
	public IPayloadContent getPayloadContent(IPayloadEntity p) throws StorageException {
		return payloads.stream().filter(pl -> pl.getPayloadId().equals(p.getPayloadId())).findFirst().orElse(null);
	}

	@Override
	public void removePayloadContent(IPayloadEntity p) throws StorageException {
		payloads.removeIf(pl -> pl.getPayloadId().equals(p.getPayloadId()));
	}

}
