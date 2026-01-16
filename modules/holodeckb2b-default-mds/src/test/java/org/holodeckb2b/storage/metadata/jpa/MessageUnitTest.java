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
package org.holodeckb2b.storage.metadata.jpa;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;

import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.jupiter.api.Test;

public class MessageUnitTest {

	@Test
	void testSetProcState() {
		Receipt r = new Receipt();
		r.setProcessingState(ProcessingState.CREATED, null);

		EntityManagerUtil.save(r);

		// Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();

		Receipt stored = em.find(Receipt.class, r.getOID());

		assertEquals(1, stored.getProcessingStates().size());
		assertEquals(ProcessingState.CREATED, stored.getCurrentProcessingState().getState());
		assertNull(stored.getCurrentProcessingState().getDescription());
		assertEquals(0, ((MessageUnitProcessingState) stored.getCurrentProcessingState()).getSeqNumber());

		final String descr = "Start processing";
		stored.setProcessingState(ProcessingState.PROCESSING, descr);

		em.getTransaction().begin();
		em.merge(stored);
		em.getTransaction().commit();
		em.close();

		em = EntityManagerUtil.getEntityManager();

		Receipt updated = em.find(Receipt.class, r.getOID());

		assertEquals(ProcessingState.PROCESSING, updated.getCurrentProcessingState().getState());
		assertEquals(descr, updated.getCurrentProcessingState().getDescription());
		assertEquals(1, ((MessageUnitProcessingState) updated.getCurrentProcessingState()).getSeqNumber());

		assertEquals(2, updated.getProcessingStates().size());
		assertEquals(ProcessingState.CREATED, updated.getProcessingStates().get(0).getState());
		assertEquals(ProcessingState.PROCESSING, updated.getProcessingStates().get(1).getState());
	}

	@Test
	void testSetPModeId() {
		final String pModeId_1 = "pm-1";
		final String pModeId_2 = "pm-2";

		PullRequest pr = new PullRequest();
		pr.setPModeId(pModeId_1);

		EntityManagerUtil.save(pr);

		// Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();

        PullRequest stored = em.find(PullRequest.class, pr.getOID());

		assertEquals(pModeId_1, stored.getPModeId());

		stored.setPModeId(pModeId_2);

		em.getTransaction().begin();
		em.merge(stored);
		em.getTransaction().commit();
		em.close();

		em = EntityManagerUtil.getEntityManager();
		PullRequest updated = em.find(PullRequest.class, pr.getOID());

		assertEquals(pModeId_2, updated.getPModeId());
	}

	@Test
	void testOptimisticLockingAttr() {
		PullRequest pr = new PullRequest();
		pr.setPModeId(UUID.randomUUID().toString());

		EntityManagerUtil.save(pr);

        EntityManager em1 = EntityManagerUtil.getEntityManager();
        EntityManager em2 = EntityManagerUtil.getEntityManager();

        PullRequest upd1 = em1.find(PullRequest.class, pr.getOID());
        PullRequest upd2 = em2.find(PullRequest.class, pr.getOID());

		upd1.setCoreId(UUID.randomUUID().toString());
		assertDoesNotThrow(() -> {
			em1.getTransaction().begin();
			em1.merge(upd1);
			em1.getTransaction().commit();
		});

		upd2.setMessageId(UUID.randomUUID().toString());

		RollbackException ex = assertThrows(RollbackException.class, () -> {
			em2.getTransaction().begin();
			em2.merge(upd2);
			em2.getTransaction().commit();
		});
		assertTrue(ex.getCause() instanceof OptimisticLockException);

		em1.close();
		em2.close();
	}

	@Test
	void testOptimisticLockingCollection() {
		PullRequest pr = new PullRequest();
		pr.setProcessingState(ProcessingState.SUBMITTED, null);

		EntityManagerUtil.save(pr);

		EntityManager em1 = EntityManagerUtil.getEntityManager();
		EntityManager em2 = EntityManagerUtil.getEntityManager();

		PullRequest upd1 = em1.find(PullRequest.class, pr.getOID());
		PullRequest upd2 = em2.find(PullRequest.class, pr.getOID());

		upd1.setProcessingState(ProcessingState.PROCESSING, null);
		assertDoesNotThrow(() -> {
			em1.getTransaction().begin();
			em1.merge(upd1);
			em1.getTransaction().commit();
		});

		upd2.setProcessingState(ProcessingState.FAILURE, "some error");

		RollbackException ex = assertThrows(RollbackException.class, () -> {
			em2.getTransaction().begin();
			em2.merge(upd2);
			em2.getTransaction().commit();
		});
		assertTrue(ex.getCause() instanceof OptimisticLockException);

		em1.close();
		em2.close();
	}

}
