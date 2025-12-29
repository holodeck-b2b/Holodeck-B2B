/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.workers;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.axiom.om.OMAbstractFactory;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageUnitPurged;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;
import org.holodeckb2b.test.storage.InMemoryMDSProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PurgeOldMessagesWorkerTest {

	private static final String PAYLOAD_1_FILE = "payload1.xml";
	private static final String PAYLOAD_2_FILE = "payload2.jpg";

	private static final String MSGID_1 = "um1-msg-id@test";
	private static final String MSGID_2 = "um2-msg-id@test";
	private static final String MSGID_3 = "um3-msg-id@test";
	private static final String MSGID_4 = "r1-msg-id@test";
	private static final String MSGID_5 = "e1-msg-id@test";
	private static final String MSGID_6 = "um6-msg-id@test";

	private static InMemoryMDSProvider  mdsProvider;
	private static TestEventProcessor	eventProcessor;

	@BeforeAll
	public static void setUpClass() throws Exception {
		HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
		mdsProvider = (InMemoryMDSProvider) testCore.getMetadataStorageProvider();
		eventProcessor = (TestEventProcessor) testCore.getEventProcessor();

		HolodeckB2BCoreInterface.setImplementation(testCore);
	}

	@BeforeEach
	void createDataSet() throws DuplicateMessageIdException, FileNotFoundException, StorageException, IOException {
		mdsProvider.clear();
		eventProcessor.reset();

		createUserMessage(MSGID_1, 28);
		createUserMessage(MSGID_2, 13);
		createUserMessage(MSGID_3,  8);
		createUserMessage(MSGID_4,  4);

		final Receipt rcpt = new Receipt();
		rcpt.setMessageId(MSGID_5);
		MessageProcessingState state = new MessageProcessingState(ProcessingState.DONE);
		Calendar stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -5);
		state.setStartTime(stateTime.getTime());
		rcpt.setProcessingState(state);
		rcpt.setContent(Collections.singletonList(OMAbstractFactory.getOMFactory().createOMElement("Testing", null)));

		mdsProvider.storeMessageUnit(rcpt);

		// Error
		final ErrorMessage error = new ErrorMessage();
		error.setMessageId(MSGID_6);
		state = new MessageProcessingState(ProcessingState.DELIVERED);
		stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -4);
		state.setStartTime(stateTime.getTime());
		error.setProcessingState(state);

		mdsProvider.storeMessageUnit(error);

		assertEquals(6, mdsProvider.getNumberOfStoredMessageUnits());
	}

	private void createUserMessage(String msgId, int days)
			throws DuplicateMessageIdException, StorageException, IOException, FileNotFoundException {

		UserMessage um = new UserMessage();
		um.setMessageId(msgId);

		Payload p1 = new Payload();
		um.addPayload(p1);

		Payload p2 = new Payload();
		um.addPayload(p2);

		MessageProcessingState state = new MessageProcessingState(ProcessingState.DELIVERY_FAILED);
		Calendar stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -days);
		state.setStartTime(stateTime.getTime());
		um.setProcessingState(state);

		mdsProvider.storeMessageUnit(um);
	}

	@Test
	public void testNaNPurgeDelay() {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, "NaN");

		try {
			worker.setParameters(parameters);
		} catch (final Exception e) {
			fail("Worker did not accept wrong delay value");
		}
	}

	@Test
	public void testNothingToPurge() {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		worker.setParameters(null);

		assertDoesNotThrow(() -> worker.doProcessing());

		assertEquals(6, mdsProvider.getNumberOfStoredMessageUnits());
	}

	@Test
	public void testOneToPurge() throws StorageException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 15);
		worker.setParameters(parameters);

		List<String> payloadIds = ((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_1)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList());

		assertDoesNotThrow(() -> worker.doProcessing());

		assertEquals(1, eventProcessor.events.size());
		assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
		assertEquals(MSGID_1, eventProcessor.events.get(0).getSubject().getMessageId());

		assertEquals(5, mdsProvider.getNumberOfStoredMessageUnits());
		assertFalse(mdsProvider.existsMessageId(MSGID_1));

		assertTrue(payloadIds.stream().noneMatch(p -> mdsProvider.existsPayloadId(p)));
	}

	@Test
	public void testMultipleToPurge() throws StorageException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 6);
		worker.setParameters(parameters);

		List<String> payloadIds = ((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_1)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList());
		payloadIds.addAll(((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_2)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList()));
		payloadIds.addAll(((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_3)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList()));

		assertDoesNotThrow(() -> worker.doProcessing());

		assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
		assertEquals(3, eventProcessor.events.size());

		assertTrue(eventProcessor.events.stream().anyMatch(ev -> MSGID_1.equals(ev.getSubject().getMessageId())));
		assertTrue(eventProcessor.events.stream().anyMatch(ev -> MSGID_2.equals(ev.getSubject().getMessageId())));
		assertTrue(eventProcessor.events.stream().anyMatch(ev -> MSGID_3.equals(ev.getSubject().getMessageId())));

		assertEquals(3, mdsProvider.getNumberOfStoredMessageUnits());

		assertTrue(payloadIds.stream().noneMatch(p -> mdsProvider.existsPayloadId(p)));
	}
}
