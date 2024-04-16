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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.holodeckb2b.common.errors.FailedAuthentication;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.providers.AlreadyChangedException;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.holodeckb2b.storage.metadata.testhelpers.TestMDSProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UpdateTests {
	private static TestMDSProvider 	provider;

	@BeforeAll
	static void setupTest() throws StorageException {
		provider = new TestMDSProvider();
		provider.init(null);
	}

	@Test
	void testUpdatePModeId() {
		ErrorMessage em = new ErrorMessage(new FailedAuthentication());
		ErrorMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(em));

		final String newPModeId = UUID.randomUUID().toString();
		stored.setPModeId(newPModeId);

		assertDoesNotThrow(() -> provider.updateMessageUnit(stored));

		assertEquals(newPModeId, stored.getPModeId());

		org.holodeckb2b.storage.metadata.jpa.ErrorMessage dbObj = EntityManagerUtil.getEntityManager()
										.find(org.holodeckb2b.storage.metadata.jpa.ErrorMessage.class, stored.getOID());

		assertEquals(newPModeId, dbObj.getPModeId());
	}

	@Test
	void testUpdateMultihop() {
		UserMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(new UserMessage()));
		assertFalse(stored.usesMultiHop());

		stored.setMultiHop(true);

		assertDoesNotThrow(() -> provider.updateMessageUnit(stored));

		assertTrue(stored.usesMultiHop());

		org.holodeckb2b.storage.metadata.jpa.UserMessage dbObj = EntityManagerUtil.getEntityManager()
										.find(org.holodeckb2b.storage.metadata.jpa.UserMessage.class, stored.getOID());

		assertTrue(dbObj.usesMultiHop());
	}

	@Test
	void testSetProcessingState() {
		ReceiptEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(new Receipt()));

		final ProcessingState state1 = ProcessingState.PROCESSING;
		stored.setProcessingState(state1, null);

		assertDoesNotThrow(() -> provider.updateMessageUnit(stored));

		assertNotNull(stored.getCurrentProcessingState().getStartTime());
		assertEquals(state1, stored.getCurrentProcessingState().getState());
		assertNull(stored.getCurrentProcessingState().getDescription());

		org.holodeckb2b.storage.metadata.jpa.Receipt dbObj = EntityManagerUtil.getEntityManager()
											.find(org.holodeckb2b.storage.metadata.jpa.Receipt.class, stored.getOID());

		assertNotNull(dbObj.getCurrentProcessingState().getStartTime());
		assertEquals(state1, dbObj.getCurrentProcessingState().getState());
		assertNull(dbObj.getCurrentProcessingState().getDescription());

		final ProcessingState state2 = ProcessingState.FAILURE;
		final String descr2 = "Due to some error";
		stored.setProcessingState(state2, descr2);

		assertDoesNotThrow(() -> provider.updateMessageUnit(stored));

		assertEquals(state2, stored.getCurrentProcessingState().getState());
		// Because an initial state will be set when message unit is stored, there are 3 states!
		assertEquals(3, stored.getProcessingStates().size());
		assertEquals(state2, stored.getProcessingStates().get(2).getState());
		assertEquals(descr2, stored.getProcessingStates().get(2).getDescription());

		dbObj = EntityManagerUtil.getEntityManager()
											.find(org.holodeckb2b.storage.metadata.jpa.Receipt.class, stored.getOID());

		assertEquals(state2, dbObj.getCurrentProcessingState().getState());
		assertEquals(3, dbObj.getProcessingStates().size());
		assertEquals(state2, dbObj.getProcessingStates().get(2).getState());
		assertEquals(descr2, dbObj.getProcessingStates().get(2).getDescription());
	}

	@Test
	void testErrorSetLeg() {
		ErrorMessage em = new ErrorMessage(new FailedAuthentication());
		ErrorMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(em));

		stored.setLeg(Label.REQUEST);

		assertDoesNotThrow(() -> provider.updateMessageUnit(stored));

		assertEquals(Label.REQUEST, stored.getLeg());

		org.holodeckb2b.storage.metadata.jpa.ErrorMessage dbObj = EntityManagerUtil.getEntityManager()
										.find(org.holodeckb2b.storage.metadata.jpa.ErrorMessage.class, stored.getOID());

		assertEquals(Label.REQUEST, dbObj.getLeg());
	}

	@Test
	void testErrorSetSOAPFault() {
		ErrorMessage em = new ErrorMessage(new FailedAuthentication());
		ErrorMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(em));

		assertFalse(stored.shouldHaveSOAPFault());

		stored.setAddSOAPFault(true);

		assertDoesNotThrow(() -> provider.updateMessageUnit(stored));

		assertTrue(stored.shouldHaveSOAPFault());

		org.holodeckb2b.storage.metadata.jpa.ErrorMessage dbObj = EntityManagerUtil.getEntityManager()
										.find(org.holodeckb2b.storage.metadata.jpa.ErrorMessage.class, stored.getOID());

		assertTrue(dbObj.shouldHaveSOAPFault());
	}

	@Test
	void testPayloadSetParent() {
		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(new Payload()));
		assertNull(stored.getParentCoreId());

		final String newParentCoreId = UUID.randomUUID().toString();
		stored.setParentCoreId(newParentCoreId);

		assertDoesNotThrow(() -> provider.updatePayloadMetadata(stored));

		assertEquals(newParentCoreId, stored.getParentCoreId());

		org.holodeckb2b.storage.metadata.jpa.PayloadInfo dbObj = EntityManagerUtil.getEntityManager()
															.find(PayloadInfo.class, ((PayloadEntity) stored).getOID());

		assertEquals(newParentCoreId, dbObj.getParentCoreId());
	}

	@Test
	void testPayloadSetMimeType() {
		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(new Payload()));
		assertNull(stored.getMimeType());

		final String newMimetype = "new/type";
		stored.setMimeType(newMimetype);

		assertDoesNotThrow(() -> provider.updatePayloadMetadata(stored));

		assertEquals(newMimetype, stored.getMimeType());

		org.holodeckb2b.storage.metadata.jpa.PayloadInfo dbObj = EntityManagerUtil.getEntityManager()
															.find(PayloadInfo.class, ((PayloadEntity) stored).getOID());

		assertEquals(newMimetype, dbObj.getMimeType());
	}

	@Test
	void testPayloadSetURI() {
		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(new Payload()));
		assertNull(stored.getPayloadURI());

		final String newPayloadURI = "cid:attachment";
		stored.setPayloadURI(newPayloadURI);

		assertDoesNotThrow(() -> provider.updatePayloadMetadata(stored));

		assertEquals(newPayloadURI, stored.getPayloadURI());

		org.holodeckb2b.storage.metadata.jpa.PayloadInfo dbObj = EntityManagerUtil.getEntityManager()
															.find(PayloadInfo.class, ((PayloadEntity) stored).getOID());

		assertEquals(newPayloadURI, dbObj.getPayloadURI());
	}

	@Test
	void testPayloadAddProperty() {
		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(new Payload()));
		assertTrue(Utils.isNullOrEmpty(stored.getProperties()));

		IProperty prop = new Property("addedP", "someExtraInfo");
		stored.addProperty(prop);

		assertDoesNotThrow(() -> provider.updatePayloadMetadata(stored));

		assertEquals(1, stored.getProperties().size());
		assertTrue(CompareUtils.areEqual(prop, stored.getProperties().iterator().next()));

		org.holodeckb2b.storage.metadata.jpa.PayloadInfo dbObj = EntityManagerUtil.getEntityManager()
															.find(PayloadInfo.class, ((PayloadEntity) stored).getOID());

		assertEquals(1, dbObj.getProperties().size());
		assertTrue(CompareUtils.areEqual(prop, dbObj.getProperties().iterator().next()));
	}

	@Test
	void testPayloadRemoveProperty() {
		Payload payload = new Payload();
		IProperty prop1 = new Property("prop1", "someExtraInfo");
		IProperty prop2 = new Property("prop2", "someMoreExtraInfo");
		payload.addProperty(prop1);
		payload.addProperty(prop2);

		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(payload));
		assertEquals(2, stored.getProperties().size());

		stored.removeProperty(prop1);

		assertDoesNotThrow(() -> provider.updatePayloadMetadata(stored));

		assertEquals(1, stored.getProperties().size());
		assertTrue(CompareUtils.areEqual(prop2, stored.getProperties().iterator().next()));

		org.holodeckb2b.storage.metadata.jpa.PayloadInfo dbObj = EntityManagerUtil.getEntityManager()
															.find(PayloadInfo.class, ((PayloadEntity) stored).getOID());

		assertEquals(1, dbObj.getProperties().size());
		assertTrue(CompareUtils.areEqual(prop2, dbObj.getProperties().iterator().next()));
	}

	@Test
	void testRejectAlreadyChanged() {
		Receipt receipt = new Receipt();
		receipt.setProcessingState(ProcessingState.CREATED);
		ReceiptEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(receipt));

		EntityManager em = EntityManagerUtil.getEntityManager();
		org.holodeckb2b.storage.metadata.jpa.Receipt dbObj = em
											.find(org.holodeckb2b.storage.metadata.jpa.Receipt.class, stored.getOID());
		dbObj.setPModeId("pm-new-id");
		em.getTransaction().begin();
		em.persist(dbObj);
		em.getTransaction().commit();
		em.close();

		stored.setProcessingState(ProcessingState.READY_TO_PUSH, null);
		assertThrows(AlreadyChangedException.class, () -> provider.updateMessageUnit(stored));

		dbObj = EntityManagerUtil.getEntityManager()
					.find(org.holodeckb2b.storage.metadata.jpa.Receipt.class, stored.getOID());
		assertEquals(receipt.getCurrentProcessingState().getState(), dbObj.getCurrentProcessingState().getState());
	}
}
