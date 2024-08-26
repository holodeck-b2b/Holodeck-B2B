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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.storage.metadata.jpa.UserMessage;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.holodeckb2b.storage.metadata.testhelpers.TestDataSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class QueryTests extends BaseProviderTest {

	@BeforeAll
	static void createTestData() {
		TestDataSet.createTestSet();
	}

	@Test
	void testGetMessageUnitsForPModesInState() {

		List<IMessageUnitEntity> r = assertDoesNotThrow(() ->
											provider.getMessageUnitsForPModesInState(IMessageUnit.class,
																Set.of(TestDataSet.T_PMODE_1, TestDataSet.T_PMODE_2),
																ProcessingState.READY_TO_PUSH));
		assertEquals(2, r.size());
		assertEquals(TestDataSet.T_RECEIPT_1.getCoreId(), r.get(0).getCoreId());
		assertEquals(TestDataSet.T_USERMESSAGE_2.getCoreId(), r.get(1).getCoreId());


		r = assertDoesNotThrow(() -> provider.getMessageUnitsForPModesInState(IUserMessage.class,
															Set.of(TestDataSet.T_PMODE_3),
															ProcessingState.READY_TO_PUSH));
		assertEquals(2, r.size());
		assertEquals(TestDataSet.T_USERMESSAGE_4.getCoreId(), r.get(0).getCoreId());
		assertEquals(TestDataSet.T_USERMESSAGE_3.getCoreId(), r.get(1).getCoreId());

		assertTrue(assertDoesNotThrow(() -> provider.getMessageUnitsForPModesInState(IMessageUnit.class,
															Set.of(TestDataSet.T_PMODE_3),
															ProcessingState.DELIVERY_FAILED)).isEmpty());

		assertTrue(assertDoesNotThrow(() -> provider.getMessageUnitsForPModesInState(IMessageUnit.class,
															Set.of(UUID.randomUUID().toString()),
															ProcessingState.READY_TO_PUSH)).isEmpty());
	}

	@Test
	void testGetMessageUnitsInState() {
		List<IMessageUnitEntity> r = assertDoesNotThrow(() ->
												provider.getMessageUnitsInState(IMessageUnit.class, Direction.IN,
																 		Set.of(ProcessingState.READY_FOR_DELIVERY,
																 			   ProcessingState.DONE)));
		assertEquals(3, r.size());
		assertEquals(TestDataSet.T_USERMESSAGE_5.getCoreId(), r.get(0).getCoreId());
		assertEquals(TestDataSet.T_PULLREQ_1.getCoreId(), r.get(1).getCoreId());
		assertEquals(TestDataSet.T_RECEIPT_2.getCoreId(), r.get(2).getCoreId());

		r = assertDoesNotThrow(() -> provider.getMessageUnitsInState(IPullRequest.class, Direction.IN,
																 		Set.of(ProcessingState.READY_FOR_DELIVERY,
																 			   ProcessingState.DONE)));
		assertEquals(1, r.size());
		assertEquals(TestDataSet.T_PULLREQ_1.getCoreId(), r.get(0).getCoreId());

		assertTrue(assertDoesNotThrow(() -> provider.getMessageUnitsInState(IMessageUnit.class, Direction.OUT,
																 		Set.of(ProcessingState.READY_FOR_DELIVERY,
																 			   ProcessingState.DONE))).isEmpty());

		assertTrue(assertDoesNotThrow(() -> provider.getMessageUnitsInState(IMessageUnit.class, Direction.IN,
																		Set.of(ProcessingState.PROCESSING))).isEmpty());
	}

	@Test
	void testGetMessageUnitsWithId() {
		Collection<IMessageUnitEntity> r = assertDoesNotThrow(() ->
														provider.getMessageUnitsWithId(TestDataSet.T_DUP_MESSAGEID));

		assertEquals(2, r.size());
		assertTrue(r.parallelStream().anyMatch(m -> TestDataSet.T_USERMESSAGE_4.getCoreId().equals(m.getCoreId())));
		assertTrue(r.parallelStream().anyMatch(m -> TestDataSet.T_USERMESSAGE_5.getCoreId().equals(m.getCoreId())));

		r = assertDoesNotThrow(() -> provider.getMessageUnitsWithId(TestDataSet.T_DUP_MESSAGEID, Direction.IN));
		assertEquals(1, r.size());
		assertTrue(r.parallelStream().anyMatch(m -> TestDataSet.T_USERMESSAGE_5.getCoreId().equals(m.getCoreId())));

		assertTrue(assertDoesNotThrow(() -> provider.getMessageUnitsWithId(UUID.randomUUID().toString())).isEmpty());
	}

	@Test
	void testGetMessageUnitsWithLastStateChangedBefore() {
		Collection<IMessageUnitEntity> r = assertDoesNotThrow(() ->
													provider.getMessageUnitsWithLastStateChangedBefore(daysBack(8)));
		assertEquals(1, r.size());
		assertTrue(r.parallelStream().anyMatch(m -> TestDataSet.T_USERMESSAGE_4.getCoreId().equals(m.getCoreId())));

		r = assertDoesNotThrow(() -> provider.getMessageUnitsWithLastStateChangedBefore(daysBack(6)));
		assertEquals(2, r.size());
		assertTrue(r.parallelStream().anyMatch(m -> TestDataSet.T_USERMESSAGE_4.getCoreId().equals(m.getCoreId())));
		assertTrue(r.parallelStream().anyMatch(m -> TestDataSet.T_PULLREQ_1.getCoreId().equals(m.getCoreId())));

		assertEquals(6, assertDoesNotThrow(() ->
											provider.getMessageUnitsWithLastStateChangedBefore(daysBack(4))).size());

		assertTrue(assertDoesNotThrow(() -> provider.getMessageUnitsWithLastStateChangedBefore(daysBack(9))).isEmpty());
	}

	private Date daysBack(int days) {
		return new Date(System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000));
	}

	@Test
	void testGetMessageUnitWithCoreId() {
		assertEquals(TestDataSet.T_USERMESSAGE_4.getCoreId(), assertDoesNotThrow(() ->
							provider.getMessageUnitWithCoreId(TestDataSet.T_USERMESSAGE_4.getCoreId())).getCoreId());

		assertNull(assertDoesNotThrow(() -> provider.getMessageUnitWithCoreId(UUID.randomUUID().toString())));
	}

	@Test
	void testGetNumberOfTransmissions() {
		assertEquals(3, (int) assertDoesNotThrow(() -> provider.getNumberOfTransmissions(TestDataSet.T_USERMESSAGE_1)));

		assertEquals(0, (int) assertDoesNotThrow(() -> provider.getNumberOfTransmissions(TestDataSet.T_USERMESSAGE_5)));
	}

	@Test
	void testIsAlreadyProcessed() {
		EntityManager em = null;
		em = EntityManagerUtil.getEntityManager();
		UserMessage um = new UserMessage();
		um.setMessageId(UUID.randomUUID().toString());
		um.setDirection(Direction.IN);
		em.getTransaction().begin();
		em.persist(um);
		em.getTransaction().commit();
		UserMessageEntity entity = new UserMessageEntity(um);
		for(ProcessingState s : ProcessingState.values()) {
			um.setProcessingState(s, null);
			em.getTransaction().begin();
			um = em.merge(um);
			em.getTransaction().commit();
			if (s == ProcessingState.DELIVERED || s == ProcessingState.FAILURE || s == ProcessingState.OUT_FOR_DELIVERY)
				assertTrue(assertDoesNotThrow(() -> provider.isAlreadyProcessed(entity)));
			else
				assertFalse(assertDoesNotThrow(() -> provider.isAlreadyProcessed(entity)));
		}
		em.close();
	}

	@Test
	void testGetMessageHistory() {
		List<IMessageUnitEntity> r = assertDoesNotThrow(() -> provider.getMessageHistory(new Date(), 100));
		assertEquals(11, r.size());

		r = assertDoesNotThrow(() -> provider.getMessageHistory(daysBack(8), 100));
		assertEquals(1, r.size());

		r = assertDoesNotThrow(() -> provider.getMessageHistory(daysBack(5), 2));
		assertEquals(2, r.size());
		assertEquals(TestDataSet.T_USERMESSAGE_7.getCoreId(), r.get(0).getCoreId());
		assertEquals(TestDataSet.T_USERMESSAGE_6.getCoreId(), r.get(1).getCoreId());
	}

	@Test
	void testCompletelyLoaded() {
		IUserMessageEntity entity = (IUserMessageEntity) assertDoesNotThrow(() ->
					provider.getMessageUnitsForPModesInState(IUserMessage.class,
															 Set.of(TestDataSet.T_PMODE_1),
															 ProcessingState.TRANSPORT_FAILURE)).stream()
									.filter(m -> m.getCoreId().equals(TestDataSet.T_USERMESSAGE_1.getCoreId()))
									.findFirst().orElse(null);
		assertNotNull(entity);
		assertEquals(TestDataSet.T_USERMESSAGE_1.getProcessingStates().size(), entity.getProcessingStates().size());
		for (int i = 0; i < TestDataSet.T_USERMESSAGE_1.getProcessingStates().size(); i++) {
			assertEquals(TestDataSet.T_USERMESSAGE_1.getProcessingStates().get(i).getStartTime(),
						 entity.getProcessingStates().get(i).getStartTime());
			assertEquals(TestDataSet.T_USERMESSAGE_1.getProcessingStates().get(i).getState(),
						 entity.getProcessingStates().get(i).getState());
			assertEquals(TestDataSet.T_USERMESSAGE_1.getProcessingStates().get(i).getDescription(),
						 entity.getProcessingStates().get(i).getDescription());
		}
		assertTrue(CompareUtils.areEqual(TestDataSet.T_USERMESSAGE_1.getSender(), entity.getSender()));
		assertTrue(CompareUtils.areEqual(TestDataSet.T_USERMESSAGE_1.getReceiver(), entity.getReceiver()));
		assertEquals(TestDataSet.T_USERMESSAGE_1.getMessageProperties().size(), entity.getMessageProperties().size());
		assertTrue(TestDataSet.T_USERMESSAGE_1.getMessageProperties().stream().allMatch(p ->
						entity.getMessageProperties().parallelStream().anyMatch(p2 -> CompareUtils.areEqual(p, p2))));
		assertEquals(TestDataSet.T_USERMESSAGE_1.getPayloads().size(), entity.getPayloads().size());
		assertTrue(TestDataSet.T_USERMESSAGE_1.getPayloads().stream().allMatch(p ->
					entity.getPayloads().parallelStream().anyMatch(p2 -> p2.getPayloadId().equals(p.getPayloadId()))));
	}
}
