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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.axiom.om.OMAbstractFactory;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageUnitPurged;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;
import org.holodeckb2b.test.storage.InMemoryMDSProvider;
import org.holodeckb2b.test.storage.InMemoryPSProvider;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * todo [] refactor the test
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
	private static InMemoryPSProvider   psProvider;
	private static TestEventProcessor	eventProcessor;

	@BeforeClass
	public static void setUpClass() throws Exception {
		HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
		mdsProvider = (InMemoryMDSProvider) testCore.getMetadataStorageProvider();		
		psProvider = (InMemoryPSProvider) testCore.getPayloadStorageProvider();		
		eventProcessor = (TestEventProcessor) testCore.getEventProcessor();
		
		HolodeckB2BCoreInterface.setImplementation(testCore);
	
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

	private static void createUserMessage(String msgId, int days)
			throws DuplicateMessageIdException, StorageException, IOException, FileNotFoundException {
		try (FileInputStream p1content = new FileInputStream(TestUtils.getTestResource(PAYLOAD_1_FILE).toFile());
			 FileInputStream p2content = new FileInputStream(TestUtils.getTestResource(PAYLOAD_2_FILE).toFile())) {
			
			UserMessage um = new UserMessage();
			um.setMessageId(msgId);
			
			Payload p1 = new Payload();
			p1.setContentStream(p1content);
			um.addPayload(p1);
	
			Payload p2 = new Payload();
			p2.setContentStream(p2content);
			um.addPayload(p2);
			
			MessageProcessingState state = new MessageProcessingState(ProcessingState.DELIVERY_FAILED);
			Calendar stateTime = Calendar.getInstance();
			stateTime.add(Calendar.DAY_OF_YEAR, -days);
			state.setStartTime(stateTime.getTime());		
			um.setProcessingState(state);
	
			mdsProvider.storeMessageUnit(um);
		}
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		mdsProvider.clear();
		psProvider.clear();
	}
	
	@Before
	public void clearEvents() {
		eventProcessor.reset();
	}
	
	@Test
	public void test0_NaNPurgeDelay() {
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
	public void test0_NothingToPurge() {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		worker.setParameters(null);
		try {
			worker.doProcessing();
		} catch (final InterruptedException ex) {
			Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
			fail("Exception during processing");
		}

		assertEquals(6, mdsProvider.getNumberOfStoredMessageUnits());
	}

	@Test
	public void test1_OneToPurge() throws StorageException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 15);
		worker.setParameters(parameters);

		List<String> payloadIds = ((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_1)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList());
		
		try {
			worker.doProcessing();
		} catch (final Exception ex) {
			Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
			fail("Exception during processing");
		}
		assertEquals(1, eventProcessor.events.size());
		assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
		assertEquals(MSGID_1, eventProcessor.events.get(0).getSubject().getMessageId());

		assertEquals(5, mdsProvider.getNumberOfStoredMessageUnits());
		assertFalse(mdsProvider.existsMessageId(MSGID_1));
		
		assertTrue(payloadIds.stream().noneMatch(p -> mdsProvider.existsPayloadId(p)));
		assertTrue(payloadIds.stream().noneMatch(p -> psProvider.exists(p)));
	}

	@Test
	public void test2_PayloadAlreadyRemoved() throws StorageException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 6);
		worker.setParameters(parameters);

		List<String> payloadIds = ((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_2)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList());
		payloadIds.addAll(((IUserMessageEntity) mdsProvider.getMessageUnit(MSGID_3)).getPayloads().stream()
															.map(p -> p.getPayloadId()).collect(Collectors.toList()));
		try {
			worker.doProcessing();
		} catch (final Exception ex) {
			Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
			fail("Exception during processing");
		}
		assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
		assertEquals(2, eventProcessor.events.size());
		
		assertTrue(eventProcessor.events.stream().anyMatch(ev -> MSGID_2.equals(ev.getSubject().getMessageId())));
		assertTrue(eventProcessor.events.stream().anyMatch(ev -> MSGID_3.equals(ev.getSubject().getMessageId())));

		assertEquals(3, mdsProvider.getNumberOfStoredMessageUnits());

		assertTrue(payloadIds.stream().noneMatch(p -> mdsProvider.existsPayloadId(p)));
		assertTrue(payloadIds.stream().noneMatch(p -> psProvider.exists(p)));
	}
}
