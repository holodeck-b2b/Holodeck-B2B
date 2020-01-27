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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.axiom.om.OMAbstractFactory;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.common.testhelpers.TestStorageManager;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageUnitPurged;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.inmemory.QueryManager;
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

	private static String basePath = TestUtils.getPath("purgetest");
	
	private static QueryManager	queryManager;
	
	private static TestEventProcessor eventProcessor;

	@BeforeClass
	public static void setUpClass() throws Exception {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(basePath));
		IUpdateManager updManager =  ((TestStorageManager) HolodeckB2BCore.getStorageManager()).getUpdateManager();
		queryManager = (QueryManager) HolodeckB2BCore.getQueryManager();
		eventProcessor = (TestEventProcessor) HolodeckB2BCore.getEventProcessor();
		
		// Make tmp directory for payload data
		new File(basePath + "/tmp").mkdir();

		Path targetPath;
		MessageProcessingState state;
		Calendar stateTime = Calendar.getInstance();

		UserMessage um;
		
		// UserMessage 1		
		um = new UserMessage();
		um.setMessageId(MSGID_1);

		Payload p1 = new Payload();
		targetPath = Paths.get(basePath, "tmp", "1-" + PAYLOAD_1_FILE);
		Files.copy(Paths.get(basePath, PAYLOAD_1_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
		p1.setContentLocation(targetPath.toString());
		um.addPayload(p1);

		Payload p2 = new Payload();
		targetPath = Paths.get(basePath, "tmp", "1-" + PAYLOAD_2_FILE);
		Files.copy(Paths.get(basePath, PAYLOAD_2_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
		p2.setContentLocation(targetPath.toString());
		um.addPayload(p2);
		
		state = new MessageProcessingState(ProcessingState.DELIVERY_FAILED);
		stateTime.add(Calendar.DAY_OF_YEAR, -20);
		state.setStartTime(stateTime.getTime());		
		um.setProcessingState(state);

		updManager.storeMessageUnit(um);
		
		// UserMessage 2		
		um = new UserMessage();
		um.setMessageId(MSGID_2);

		// Simulate payload file already deleted by not making copy
		p1 = new Payload();
		targetPath = Paths.get(basePath, "tmp", "2-" + PAYLOAD_1_FILE);
		p1.setContentLocation(targetPath.toString());
		um.addPayload(p1);

		state = new MessageProcessingState(ProcessingState.AWAITING_RECEIPT);
		stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -13);
		state.setStartTime(stateTime.getTime());
		um.setProcessingState(state);

		updManager.storeMessageUnit(um);
		
		// UserMessage 3
		um = new UserMessage();
		um.setMessageId(MSGID_3);
		
		p1 = new Payload();
		targetPath = Paths.get(basePath, "tmp", "3-" + PAYLOAD_1_FILE);
		Files.copy(Paths.get(basePath, PAYLOAD_1_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
		p1.setContentLocation(targetPath.toString());
		um.addPayload(p1);
		
		state = new MessageProcessingState(ProcessingState.DELIVERED);
		stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -8);
		state.setStartTime(stateTime.getTime());
		um.setProcessingState(state);
		
		updManager.storeMessageUnit(um);

		// User Message 4
		um = new UserMessage();
		um.setMessageId(MSGID_6);
		targetPath = Paths.get(basePath, "tmp", "6-" + PAYLOAD_1_FILE);
		Files.copy(Paths.get(basePath, PAYLOAD_1_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
		p1 = new Payload();
		p1.setContentLocation(targetPath.toString());
		um.addPayload(p1);
		state = new MessageProcessingState(ProcessingState.DUPLICATE);
		stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -4);
		state.setStartTime(stateTime.getTime());
		um.setProcessingState(state);
		
		updManager.storeMessageUnit(um);
		
		// Receipt
		final Receipt rcpt = new Receipt();
		rcpt.setMessageId(MSGID_4);
		state = new MessageProcessingState(ProcessingState.DONE);
		stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -8);
		state.setStartTime(stateTime.getTime());
		rcpt.setProcessingState(state);
		rcpt.setContent(Collections.singletonList(OMAbstractFactory.getOMFactory().createOMElement("Testing", null)));
		
		updManager.storeMessageUnit(rcpt);
		
		// Error
		final ErrorMessage error = new ErrorMessage();
		error.setMessageId(MSGID_5);
		state = new MessageProcessingState(ProcessingState.DELIVERED);
		stateTime = Calendar.getInstance();
		stateTime.add(Calendar.DAY_OF_YEAR, -4);
		state.setStartTime(stateTime.getTime());
		error.setProcessingState(state);

		updManager.storeMessageUnit(error);
		
		assertEquals(6, queryManager.getNumberOfStoredMessageUnits());
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		TestUtils.cleanOldMessageUnitEntities();
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

		assertEquals(6, queryManager.getNumberOfStoredMessageUnits());
	}

	@Test
	public void test1_OneToPurge() throws PersistenceException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 15);
		worker.setParameters(parameters);

		try {
			worker.doProcessing();

			assertEquals(1, eventProcessor.events.size());
			assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
			assertEquals(MSGID_1, eventProcessor.events.get(0).getSubject().getMessageId());
		} catch (final Exception ex) {
			Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
			fail("Exception during processing");
		}

		assertEquals(5, queryManager.getNumberOfStoredMessageUnits());
		assertTrue(Utils.isNullOrEmpty(queryManager.getMessageUnitsWithId(MSGID_1)));
		assertFalse(Paths.get(basePath, "tmp", "1-" + PAYLOAD_1_FILE).toFile().exists());
		assertFalse(Paths.get(basePath, "tmp", "2-" + PAYLOAD_2_FILE).toFile().exists());
	}

	@Test
	public void test2_PayloadAlreadyRemoved() throws PersistenceException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 10);
		worker.setParameters(parameters);

		try {
			worker.doProcessing();

			assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
			assertEquals(1, eventProcessor.events.size());
			assertEquals(MSGID_2, eventProcessor.events.get(0).getSubject().getMessageId());
		} catch (final Exception ex) {
			Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
			fail("Exception during processing");
		}

		assertEquals(4, queryManager.getNumberOfStoredMessageUnits());
		assertTrue(Utils.isNullOrEmpty(queryManager.getMessageUnitsWithId(MSGID_2)));
	}

	@Test
	public void test3_EventsOnlyForUserMsg() throws PersistenceException {
		final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

		final HashMap<String, Object> parameters = new HashMap<>();
		parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 5);
		worker.setParameters(parameters);

		try {
			worker.doProcessing();

			assertTrue(eventProcessor.events.stream().allMatch(e -> e instanceof IMessageUnitPurged));
			assertEquals(1, eventProcessor.events.size());
			assertEquals(MSGID_3, eventProcessor.events.get(0).getSubject().getMessageId());
		} catch (final Exception ex) {
			Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
			fail("Exception during processing");
		}

		assertEquals(2, queryManager.getNumberOfStoredMessageUnits());
		assertTrue(Utils.isNullOrEmpty(queryManager.getMessageUnitsWithId(MSGID_3)));
		assertTrue(Utils.isNullOrEmpty(queryManager.getMessageUnitsWithId(MSGID_4)));
		assertFalse(Paths.get(basePath, "tmp", "3-" + PAYLOAD_1_FILE).toFile().exists());
	}
}
