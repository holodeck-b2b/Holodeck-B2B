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
package org.holodeckb2b.storage.metadata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.jupiter.api.Test;

public class DeleteTests extends BaseProviderTest {

	@Test
	void testRemoveUserMessage() {
		UserMessage um = new UserMessage();
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		um.addPayload(pl);

		UserMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(um));

		assertDoesNotThrow(() -> provider.deleteMessageUnit(stored));

		assertNull(EntityManagerUtil.getEntityManager().find(org.holodeckb2b.storage.metadata.jpa.UserMessage.class, stored.getOID()));
		assertNull(EntityManagerUtil.getEntityManager().find(PayloadInfo.class, stored.getPayloads().iterator().next().getOID()));
	}

	@Test
	void testRemovePayload() {
		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(new Payload(), null));

		assertDoesNotThrow(() -> provider.deletePayloadMetadata(stored));

		assertNull(EntityManagerUtil.getEntityManager().find(PayloadInfo.class, ((PayloadEntity) stored).getOID()));
	}

	@Test
	void testRejectLinkedPayload() {
		UserMessage um = new UserMessage();
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		um.addPayload(pl);

		UserMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(um));

		assertThrows(StorageException.class,
						() -> provider.deletePayloadMetadata(stored.getPayloads().iterator().next()));

		assertNotNull(EntityManagerUtil.getEntityManager().find(PayloadInfo.class,
																	stored.getPayloads().iterator().next().getOID()));
	}

}
