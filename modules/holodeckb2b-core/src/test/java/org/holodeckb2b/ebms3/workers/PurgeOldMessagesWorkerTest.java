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
package org.holodeckb2b.ebms3.workers;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.events.IMessageUnitPurgedEvent;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.persistency.jpa.*;
import org.holodeckb2b.persistency.util.EntityManagerUtil;
import org.junit.*;
import org.junit.runners.MethodSorters;

import javax.persistence.EntityManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;


/**
 * todo [] refactor the test
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PurgeOldMessagesWorkerTest {
    
    private static final String PAYLOAD_1_FILE = "payload1.xml";
    private static final String PAYLOAD_1_MIMETYPE = "text/xml";

    private static final String PAYLOAD_2_FILE = "payload2.jpg";
    private static final String PAYLOAD_2_MIMETYPE = "image/jpeg";

    private static final String MSGID_1 = "um1-msg-id@test";
    private static final String MSGID_2 = "um2-msg-id@test";
    private static final String MSGID_3 = "um3-msg-id@test";
    private static final String MSGID_4 = "r1-msg-id@test";
    private static final String MSGID_5 = "e1-msg-id@test";
    private static final String MSGID_6 = "um6-msg-id@test";

    private static HolodeckB2BTestCore core;

    private static String basePath = PurgeOldMessagesWorkerTest.class
            .getClassLoader().getResource("purgetest/").getPath();

    @BeforeClass
    public static void setUpClass() {
        core = new HolodeckB2BTestCore(basePath);
        HolodeckB2BCoreInterface.setImplementation(core);

        try {
            // todo The next line is temporary. We need to clean tests resources correctly
            // todo Currently some tests does not correctly clean the resources which leads
            // todo to failure of this test without pre cleaning of the resources while running tests altogether
            TestUtils.cleanOldMessageUnitEntities();

            final EntityManager em = EntityManagerUtil.getEntityManager();

            em.getTransaction().begin();

            UserMessage um;
            Path targetPath;
            MessageUnitProcessingState state;
            Calendar stateTime = Calendar.getInstance();

            // Ensure tmp directory is available
            new File(basePath + "tmp").mkdir();

            um = new UserMessage();
            um.setMessageId(MSGID_1);
            targetPath = Paths.get(basePath, "tmp", "1-" + PAYLOAD_1_FILE);
            Files.copy(Paths.get(basePath, PAYLOAD_1_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
            Payload p1 = new Payload();
            p1.setContentLocation(targetPath.toString());
            p1.setMimeType(PAYLOAD_1_MIMETYPE);
            um.addPayload(p1);
            targetPath = Paths.get(basePath, "tmp", "1-" + PAYLOAD_2_FILE);
            Files.copy(Paths.get(basePath, PAYLOAD_2_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
            Payload p2 = new Payload();
            p2.setContentLocation(targetPath.toString());
            p2.setMimeType(PAYLOAD_2_MIMETYPE);
            um.addPayload(p1);
            um.addPayload(p2);
            state = new MessageUnitProcessingState();//new ProcessingState("TEST");
            stateTime.add(Calendar.DAY_OF_YEAR, -20);
            state.setStartTime(stateTime.getTime());
            um.setProcessingState(state);
            em.persist(um);

            um = new UserMessage();
            um.setMessageId(MSGID_2);
            targetPath = Paths.get(basePath, "tmp", "2-" + PAYLOAD_1_FILE);
            p1 = new Payload();
            p1.setContentLocation(targetPath.toString());
            p1.setMimeType(PAYLOAD_1_MIMETYPE);
            um.addPayload(p1);
            state = new MessageUnitProcessingState();//new ProcessingState("TEST");
            stateTime = Calendar.getInstance();
            stateTime.add(Calendar.DAY_OF_YEAR, -13);
            state.setStartTime(stateTime.getTime());
            um.setProcessingState(state);
            em.persist(um);

            um = new UserMessage();
            um.setMessageId(MSGID_3);
            targetPath = Paths.get(basePath, "tmp", "3-" + PAYLOAD_1_FILE);
            Files.copy(Paths.get(basePath, PAYLOAD_1_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
            p1 = new Payload();
            p1.setContentLocation(targetPath.toString());
            p1.setMimeType(PAYLOAD_1_MIMETYPE);
            um.addPayload(p1);
            state = new MessageUnitProcessingState();//new ProcessingState("TEST");
            stateTime = Calendar.getInstance();
            stateTime.add(Calendar.DAY_OF_YEAR, -8);
            state.setStartTime(stateTime.getTime());
            um.setProcessingState(state);
            em.persist(um);

            final Receipt rcpt = new Receipt();
            rcpt.setMessageId(MSGID_4);
            state = new MessageUnitProcessingState();//new ProcessingState("TEST");
            stateTime = Calendar.getInstance();
            stateTime.add(Calendar.DAY_OF_YEAR, -8);
            state.setStartTime(stateTime.getTime());
            rcpt.setProcessingState(state);
            final OMFactory factory = OMAbstractFactory.getOMFactory();
            final OMElement root = factory.createOMElement("root", null);
            final OMElement elt11 = factory.createOMElement("fakeContent",null);
            elt11.setText("No real Receipt");
//            rcpt.setContent(root.getChildElements());
            em.persist(rcpt);

            final ErrorMessage error = new ErrorMessage();
            error.setMessageId(MSGID_5);
            state = new MessageUnitProcessingState();//new ProcessingState("TEST");
            stateTime = Calendar.getInstance();
            stateTime.add(Calendar.DAY_OF_YEAR, -4);
            state.setStartTime(stateTime.getTime());
            error.setProcessingState(state);
            em.persist(error);

            um = new UserMessage();
            um.setMessageId(MSGID_6);
            targetPath = Paths.get(basePath, "tmp", "6-" + PAYLOAD_1_FILE);
            Files.copy(Paths.get(basePath, PAYLOAD_1_FILE), targetPath, StandardCopyOption.REPLACE_EXISTING);
            p1 = new Payload();
            p1.setContentLocation(targetPath.toString());
            p1.setMimeType(PAYLOAD_1_MIMETYPE);
            um.addPayload(p1);
            state = new MessageUnitProcessingState();//new ProcessingState("TEST");
            stateTime = Calendar.getInstance();
            stateTime.add(Calendar.DAY_OF_YEAR, -4);
            state.setStartTime(stateTime.getTime());
            um.setProcessingState(state);
            em.persist(um);

            em.getTransaction().commit();

            final List<MessageUnit> muInDB =
                    em.createQuery("from MessageUnit", MessageUnit.class)
                            .getResultList();
            assertEquals(6, muInDB.size());

            em.close();

        } catch (final Exception ex) {
            Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName())
                    .log(Level.SEVERE, "Could not setup test", ex);
            fail("Test could not be prepared!");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TestUtils.cleanOldMessageUnitEntities();
        core.getPModeSet().removeAll();
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
    public void test0_NothingToPurge() throws PersistenceException {
        final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

        worker.setParameters(null);
        try {
            worker.doProcessing();
        } catch (final InterruptedException ex) {
            Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception during processing");
        }

        final EntityManager em = EntityManagerUtil.getEntityManager();
        final List<MessageUnit> muInDB = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();

        assertEquals(6, muInDB.size());
    }

    @Test
    public void test1_OneToPurge() throws PersistenceException {
        final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 15);
        worker.setParameters(parameters);

        try {
            // Enable event processing
            final HolodeckB2BTestCore hb2bTestCore = new HolodeckB2BTestCore("");
            final TestEventProcessor eventProcessor = new TestEventProcessor();
            hb2bTestCore.setEventProcessor(eventProcessor);
            HolodeckB2BCoreInterface.setImplementation(hb2bTestCore);

            worker.doProcessing();

            assertTrue(eventProcessor.allPurgeEvents);
            assertEquals(1, eventProcessor.msgIdsPurged.size());
            assertEquals(MSGID_1, eventProcessor.msgIdsPurged.get(0));
        } catch (final Exception ex) {
            Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception during processing");
        }

        final EntityManager em = EntityManagerUtil.getEntityManager();
        final List<MessageUnit> muInDB = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();

        assertEquals(5, muInDB.size());

        boolean m1Deleted = true;
        for (final MessageUnit mu : muInDB)
            m1Deleted &= !MSGID_1.equals(mu.getMessageId());
        assertTrue(m1Deleted);

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
            // Enable event processing
            final HolodeckB2BTestCore hb2bTestCore = new HolodeckB2BTestCore("");
            final TestEventProcessor eventProcessor = new TestEventProcessor();
            hb2bTestCore.setEventProcessor(eventProcessor);
            HolodeckB2BCoreInterface.setImplementation(hb2bTestCore);

            worker.doProcessing();

            assertTrue(eventProcessor.allPurgeEvents);
            assertEquals(1, eventProcessor.msgIdsPurged.size());
            assertEquals(MSGID_2, eventProcessor.msgIdsPurged.get(0));
        } catch (final Exception ex) {
            Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception during processing");
        }

        final EntityManager em = EntityManagerUtil.getEntityManager();
        final List<MessageUnit> muInDB = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();

        assertEquals(4, muInDB.size());

        boolean m2Deleted = true;
        for (final MessageUnit mu : muInDB)
            m2Deleted &= !MSGID_2.equals(mu.getMessageId());

        assertTrue(m2Deleted);
    }

    @Test
    public void test3_EventsOnlyForUserMsg() throws PersistenceException {
        final PurgeOldMessagesWorker worker = new PurgeOldMessagesWorker();

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(PurgeOldMessagesWorker.P_PURGE_AFTER_DAYS, 5);
        worker.setParameters(parameters);

        try {
            // Enable event processing
            final HolodeckB2BTestCore hb2bTestCore = new HolodeckB2BTestCore("");
            final TestEventProcessor eventProcessor = new TestEventProcessor();
            hb2bTestCore.setEventProcessor(eventProcessor);
            HolodeckB2BCoreInterface.setImplementation(hb2bTestCore);

            worker.doProcessing();

            assertTrue(eventProcessor.allPurgeEvents);
            assertEquals(1, eventProcessor.msgIdsPurged.size());
            assertEquals(MSGID_3, eventProcessor.msgIdsPurged.get(0));
        } catch (final Exception ex) {
            Logger.getLogger(PurgeOldMessagesWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception during processing");
        }

        final EntityManager em = EntityManagerUtil.getEntityManager();
        final List<MessageUnit> muInDB = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();

        assertEquals(2, muInDB.size());

        boolean m3Deleted = true;
        boolean m4Deleted = true;
        for (final MessageUnit mu : muInDB) {
            m3Deleted &= !MSGID_3.equals(mu.getMessageId());
            m4Deleted &= !MSGID_4.equals(mu.getMessageId());
        }
        assertTrue(m3Deleted);
        assertTrue(m4Deleted);

        assertFalse(Paths.get(basePath, "tmp", "3-" + PAYLOAD_1_FILE).toFile().exists());
    }

    class TestEventProcessor implements IMessageProcessingEventProcessor {

        boolean allPurgeEvents = false;
        ArrayList<String> msgIdsPurged = new ArrayList<>();

        @Override
        public void raiseEvent(final IMessageProcessingEvent event, final MessageContext msgContext) {
            allPurgeEvents = event instanceof IMessageUnitPurgedEvent;
            if (allPurgeEvents) {
                assertNotNull(event.getSubject());
                msgIdsPurged.add(event.getSubject().getMessageId());
            }
        }
    }
}
