/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import org.hibernate.LazyInitializationException;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.util.CompareUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IUpdateManager;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.entities.ErrorMessageEntity;
import org.holodeckb2b.persistency.entities.MessageUnitEntity;
import org.holodeckb2b.persistency.entities.PullRequestEntity;
import org.holodeckb2b.persistency.entities.ReceiptEntity;
import org.holodeckb2b.persistency.entities.UserMessageEntity;
import org.holodeckb2b.persistency.jpa.ErrorMessage;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.Receipt;
import org.holodeckb2b.persistency.jpa.UserMessage;
import org.holodeckb2b.persistency.test.TestData;
import org.holodeckb2b.persistency.test.TestProvider;
import org.holodeckb2b.persistency.util.EntityManagerUtil;
import org.holodeckb2b.persistency.util.JPAEntityHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Is the test class for the {@link IUpdateManager} implementation of the default persistency provider.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class UpdateManagerTest {

    private static final String          T_NEW_PMODE_ID_1 = "PMODE-NEW-01";
    private static final boolean         T_NEW_MULTI_HOP  = true;
    private static final ILeg.Label      T_NEW_LEG_LABEL = ILeg.Label.REPLY;
    private static final String          T_NEW_CONTENT_LOC = "/root/holodeckb2b/test/payload/anotherfile.png";
    private static final String          T_NEW_PL_PROP_NAME = "MimeType";
    private static final String          T_NEW_PL_PROP_VALUE = "application/gzip";
    private static final boolean         T_NEW_SHOULD_HAVE_FAULT = true;
    private static final ProcessingState T_NEW_PROC_STATE_1 = ProcessingState.PROCESSING;
    private static final ProcessingState T_NEW_PROC_STATE_2 = ProcessingState.SENDING;

    private static UpdateManager   updManager;

    private static EntityManager   em;

    public UpdateManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws PersistenceException {
        em = EntityManagerUtil.getEntityManager();
        updManager = new UpdateManager();
        
        HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
        testCore.setPersistencyProvider(new TestProvider());
        HolodeckB2BCoreInterface.setImplementation(testCore);
    }

    @Before
    public void setUp() {
        // Clean database
        try {
            // First clean the database
            em.getTransaction().begin();
            final Collection<MessageUnit> allMU = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();
            for(final MessageUnit mu : allMU) {
                // The refresh is needed to ensure the EM does not use a cached object!
                em.refresh(mu);
                em.remove(mu);
            }
            em.getTransaction().commit();
        } catch(Exception e) {
            Logger.getLogger(UpdateManagerTest.class.getName()).log(Level.SEVERE, null, e);
            if (em != null)
                em.close();
        }
    }

    @After
    public void shutDown() {
        try {
            // Rollback any active transaction and close entity manager
            if (em != null && em.isOpen() && em.getTransaction().isActive())
                em.getTransaction().rollback();
        } catch (Exception e) {
            Logger.getLogger(UpdateManagerTest.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    @Test
    public void storeUserMessage() throws PersistenceException {
        // Store a User Message
        UserMessageEntity userMsg =
                updManager.storeMessageUnit(new org.holodeckb2b.common.messagemodel.UserMessage(TestData.userMsg1));

        assertNotNull(userMsg);
        assertNotNull(userMsg.getOID());
        assertEquals(TestData.userMsg1.getPModeId(), userMsg.getPModeId());
        assertEquals(TestData.userMsg1.getMessageId(), userMsg.getMessageId());
        assertEquals(TestData.userMsg1.getRefToMessageId(), userMsg.getRefToMessageId());
        assertEquals(TestData.userMsg1.getDirection(), userMsg.getDirection());
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getSender(), userMsg.getSender()));
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getReceiver(), userMsg.getReceiver()));
        assertFalse(Utils.isNullOrEmpty(userMsg.getProcessingStates()));
        assertEquals(TestData.userMsg1.getCurrentProcessingState().getStartTime(),
                userMsg.getCurrentProcessingState().getStartTime());
        assertEquals(TestData.userMsg1.getCurrentProcessingState().getState(),
                userMsg.getCurrentProcessingState().getState());

        assertFalse(Utils.isNullOrEmpty(userMsg.getPayloads()));
        assertEquals(1, userMsg.getPayloads().size());
        IPayload savedPayload = userMsg.getPayloads().iterator().next();
        IPayload orgPayload = TestData.userMsg1.getPayloads().iterator().next();
        assertEquals(orgPayload.getContainment(), savedPayload.getContainment());
        assertFalse(Utils.isNullOrEmpty(savedPayload.getProperties()));
        assertEquals(1, savedPayload.getProperties().size());
        assertTrue(CompareUtils.areEqual(orgPayload.getProperties().iterator().next(),
                savedPayload.getProperties().iterator().next()));
        assertEquals(orgPayload.getSchemaReference().getNamespace(), savedPayload.getSchemaReference().getNamespace());
        assertNull(savedPayload.getSchemaReference().getLocation());
        assertNotNull(savedPayload.getDescription());
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getCollaborationInfo().getService(),
                userMsg.getCollaborationInfo().getService()));
        assertFalse(Utils.isNullOrEmpty(userMsg.getMessageProperties()));
        assertEquals(1, userMsg.getMessageProperties().size());
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getMessageProperties().iterator().next(),
                userMsg.getMessageProperties().iterator().next()));
    }

    @Test
    public void storeReceipt() throws PersistenceException {
        ReceiptEntity receiptEntity =
                updManager.storeMessageUnit(new org.holodeckb2b.common.messagemodel.Receipt(TestData.receipt6));
        assertNotNull(receiptEntity);

        assertEquals(TestData.receipt6.getMessageId(), receiptEntity.getMessageId());
        assertEquals(TestData.receipt6.getRefToMessageId(), receiptEntity.getRefToMessageId());
        assertEquals(TestData.receipt6.getTimestamp(), receiptEntity.getTimestamp());

        assertFalse(Utils.isNullOrEmpty(receiptEntity.getContent()));

        assertEquals(TestData.receipt6.getContent().get(0).getQName(), receiptEntity.getContent().get(0).getQName());
        assertEquals(TestData.receipt6.getContent().get(0).getText(), receiptEntity.getContent().get(0).getText());
    }

    @Test
    public void storePullRequest() throws PersistenceException {
        PullRequestEntity pullEntity = updManager.storeMessageUnit(TestData.pull5);
        assertNotNull(pullEntity);

        assertEquals(TestData.pull5.getMessageId(), pullEntity.getMessageId());
        assertEquals(TestData.pull5.getRefToMessageId(), pullEntity.getRefToMessageId());
        assertEquals(TestData.pull5.getTimestamp(), pullEntity.getTimestamp());

        pullEntity = updManager.storeMessageUnit(TestData.pull6);
        assertNotNull(pullEntity);
        assertTrue(pullEntity instanceof ISelectivePullRequest);

        assertEquals(TestData.pull6.getMessageId(), pullEntity.getMessageId());
        assertEquals(TestData.pull6.getRefToMessageId(), pullEntity.getRefToMessageId());
        assertEquals(TestData.pull6.getTimestamp(), pullEntity.getTimestamp());

        assertEquals(TestData.pull6.getReferencedMessageId(),
                        ((ISelectivePullRequest) pullEntity).getReferencedMessageId());

        assertNull(((ISelectivePullRequest) pullEntity).getConversationId());
        assertNull(((ISelectivePullRequest) pullEntity).getAgreementRef());
        assertNull(((ISelectivePullRequest) pullEntity).getService());
        assertNull(((ISelectivePullRequest) pullEntity).getAction());

        pullEntity = updManager.storeMessageUnit(TestData.pull7);
        assertNotNull(pullEntity);
        assertTrue(pullEntity instanceof ISelectivePullRequest);

        assertEquals(TestData.pull7.getMessageId(), pullEntity.getMessageId());
        assertEquals(TestData.pull7.getRefToMessageId(), pullEntity.getRefToMessageId());
        assertEquals(TestData.pull7.getTimestamp(), pullEntity.getTimestamp());

        assertNull(((ISelectivePullRequest) pullEntity).getReferencedMessageId());
        assertEquals(TestData.pull7.getConversationId() ,((ISelectivePullRequest) pullEntity).getConversationId());
        assertNotNull(((ISelectivePullRequest) pullEntity).getAgreementRef());
        assertEquals(TestData.pull7.getAgreementRef().getName(),
                                                    ((ISelectivePullRequest) pullEntity).getAgreementRef().getName());
        assertEquals(TestData.pull7.getAgreementRef().getType(),
                                                    ((ISelectivePullRequest) pullEntity).getAgreementRef().getType());
        assertNotNull(((ISelectivePullRequest) pullEntity).getService());
        assertTrue(CompareUtils.areEqual(TestData.pull7.getService(), ((ISelectivePullRequest) pullEntity).getService()));
        assertNull(((ISelectivePullRequest) pullEntity).getAction());

    }



    @Test
    public void setProcessingState() throws PersistenceException, InterruptedException {
        // Add a message unit to the database so we can change it
        em.getTransaction().begin();
        UserMessage userMsgJPA = new UserMessage(TestData.userMsg1);
        em.persist(userMsgJPA);
        final UserMessageEntity userMsg = new UserMessageEntity(userMsgJPA);
        em.getTransaction().commit();
        // Check it accidentially has not already the new value
        assertNotEquals(T_NEW_PROC_STATE_1, userMsg.getCurrentProcessingState().getState());

        // Update to a new state
        ProcUpdater updater1 = new ProcUpdater(userMsg,
                                               userMsg.getCurrentProcessingState().getState(), T_NEW_PROC_STATE_1);
        new Thread(updater1).start();
        synchronized(updater1) { updater1.wait(); }
        // Check the new processing state is set
        assertTrue(updater1.s);
        assertEquals(T_NEW_PROC_STATE_1, userMsg.getCurrentProcessingState().getState());
        assertEquals(2, userMsg.getProcessingStates().size());
        // And check database
        em.refresh(userMsgJPA);
        assertEquals(T_NEW_PROC_STATE_1, userMsgJPA.getCurrentProcessingState().getState());
        assertEquals(2, userMsgJPA.getProcessingStates().size());

        // Test that update is rejected when not in correct state
        new Thread(updater1).start();
        synchronized(updater1) { updater1.wait(); }
        assertFalse(updater1.s);
        assertEquals(T_NEW_PROC_STATE_1, userMsg.getCurrentProcessingState().getState());
        assertEquals(2, userMsg.getProcessingStates().size());
        // And check database
        em.refresh(userMsgJPA);
        assertEquals(T_NEW_PROC_STATE_1, userMsgJPA.getCurrentProcessingState().getState());
        assertEquals(2, userMsgJPA.getProcessingStates().size());

        // Test parallel update (one should be rejected)
        updater1 = new ProcUpdater(userMsg, userMsg.getCurrentProcessingState().getState(), T_NEW_PROC_STATE_2);
        ProcUpdater updater2 = new ProcUpdater(userMsg,
                                               userMsg.getCurrentProcessingState().getState(), T_NEW_PROC_STATE_2);
        new Thread(updater1).start();
        new Thread(updater2).start();
        synchronized(updater1) { updater1.wait(); }
        if (updater2.s == null)
            synchronized(updater2) { updater2.wait(); }
        // One should have failed!
        assertFalse(updater1.s && updater2.s);
        // But stat should have changed
        assertEquals(T_NEW_PROC_STATE_2, userMsg.getCurrentProcessingState().getState());
        assertEquals(3, userMsg.getProcessingStates().size());
        // And check database
        em.refresh(userMsgJPA);
        assertEquals(T_NEW_PROC_STATE_2, userMsgJPA.getCurrentProcessingState().getState());
        assertEquals(3, userMsgJPA.getProcessingStates().size());
    }

    @Test
    public void testStoreMessageUnit1() throws Exception {

    }

    class ProcUpdater implements Runnable {
        private     MessageUnitEntity   m;
        private     ProcessingState     c, n;

        Boolean s = null;

        ProcUpdater(final MessageUnitEntity m, final ProcessingState c, final ProcessingState n) {
            this.m = m;
            this.c = c;
            this.n = n;
        }

        @Override
        public void run() {
            try {
                s = updManager.setProcessingState(m, c, n);
            } catch (PersistenceException ex) {
                s = false;
            }
            synchronized(this) { notify(); }
        }
    }

    @Test
    public void deleteMessageUnit() throws PersistenceException {
        // First create some records
        TestData.createTestSet();

        em.getTransaction().begin();
        final List<MessageUnitEntity> allMsgUnits = JPAEntityHelper.wrapInEntity(em.createQuery("from MessageUnit",
                                                                                      MessageUnit.class)
                                                                                     .getResultList());
        assertFalse(Utils.isNullOrEmpty(allMsgUnits));
        int totalMsgUnits = allMsgUnits.size();
        int numberDeleted = 0;
        for(MessageUnitEntity msgUnit : allMsgUnits) {
            updManager.deleteMessageUnit(msgUnit);
            numberDeleted += 1;
            // Count number of message units left
            long currentCount = em.createQuery("select count(*) from MessageUnit", Long.class).getSingleResult();
            assertEquals(totalMsgUnits - numberDeleted, currentCount);
            try {
                em.refresh((em.find(MessageUnit.class, msgUnit.getOID())));
                fail("MessageUnit not removed");
            } catch (EntityNotFoundException removed) {
                // Okay
            }
        }
    }

    @Test
    public void setPModeId() throws PersistenceException {
        // Add a message unit to the database so we can change it
        em.getTransaction().begin();
        UserMessage userMsgJPA = new UserMessage(TestData.userMsg1);
        em.persist(userMsgJPA);
        UserMessageEntity userMsg = new UserMessageEntity(userMsgJPA);
        em.getTransaction().commit();
        // Check it accidentially has not already the new value
        assertNotEquals(T_NEW_PMODE_ID_1, userMsg.getPModeId());
        // Perform the update
        updManager.setPModeId(userMsg, T_NEW_PMODE_ID_1);
        // Check update in entity object
        assertEquals(T_NEW_PMODE_ID_1, userMsg.getPModeId());
        // Check that database is updated
        em.refresh(userMsgJPA);
        assertEquals(T_NEW_PMODE_ID_1, userMsgJPA.getPModeId());
    }

    @Test
    public void setMultiHop() throws PersistenceException {
        // Add a message unit to the database so we can change it
        em.getTransaction().begin();
        ErrorMessage errorMsgJPA = new ErrorMessage(TestData.error3);
        em.persist(errorMsgJPA);
        ErrorMessageEntity errorMsg = new ErrorMessageEntity(errorMsgJPA);
        em.getTransaction().commit();
        // Check it accidentially has not already the new value
        assertNotEquals(T_NEW_MULTI_HOP, errorMsg.usesMultiHop());
        // Perform the update
        updManager.setMultiHop(errorMsg, T_NEW_MULTI_HOP);
        // Check update in entity object
        assertEquals(T_NEW_MULTI_HOP, errorMsg.usesMultiHop());
        // Check that database is updated
        em.refresh(errorMsgJPA);
        assertEquals(T_NEW_MULTI_HOP, errorMsgJPA.usesMultiHop());
    }

    @Test
    public void setLeg() throws PersistenceException {
        // Add a message unit to the database so we can change it
        em.getTransaction().begin();
        Receipt receiptJPA = new Receipt(TestData.receipt1);
        em.persist(receiptJPA);
        ReceiptEntity receiptMsg = new ReceiptEntity(receiptJPA);
        em.getTransaction().commit();
        // Check it accidentially has not already the new value
        assertNotEquals(T_NEW_LEG_LABEL, receiptMsg.getLeg());
        // Perform the update
        updManager.setLeg(receiptMsg, T_NEW_LEG_LABEL);
        // Check update in entity object
        assertEquals(T_NEW_LEG_LABEL, receiptMsg.getLeg());
        // Check that database is updated
        em.refresh(receiptJPA);
        assertEquals(T_NEW_LEG_LABEL, receiptJPA.getLeg());
    }

    @Test
    public void setPayloadInformation() throws PersistenceException {
        // Because payload data is lazily loaded, we need to completely load the JPA object using the QueryManager
        QueryManager    queryManager = new QueryManager();

        // Add a message unit to the database so we can change it
        em.getTransaction().begin();
        UserMessage userMsgJPA = new UserMessage(TestData.userMsg5);
        em.persist(userMsgJPA);
        UserMessageEntity userMsg = new UserMessageEntity(userMsgJPA);
        em.getTransaction().commit();
        // Check it accidentially has not already the new value
        queryManager.ensureCompletelyLoaded(userMsg);
        assertTrue(Utils.isNullOrEmpty(userMsg.getPayloads()));
        // Perform the update
        Collection<IPayload>    newPayloads = new ArrayList<>();
        newPayloads.add(TestData.payload2);
        newPayloads.add(TestData.payload3);
        updManager.setPayloadInformation(userMsg, newPayloads);
        // Check update in entity object
        assertFalse(Utils.isNullOrEmpty(userMsg.getPayloads()));
        assertEquals(2, userMsg.getPayloads().size());
        // Check that database is updated
        em.refresh(userMsgJPA);
        assertFalse(Utils.isNullOrEmpty(userMsgJPA.getPayloads()));
        assertEquals(2, userMsgJPA.getPayloads().size());

        // Change one of the payloads in the updated set
        Iterator<IPayload> it = userMsg.getPayloads().iterator();
        Payload p1 = new Payload(it.next());
        p1.addProperty(new Property(T_NEW_PL_PROP_NAME, T_NEW_PL_PROP_VALUE));
        Payload p2 = new Payload(it.next());
        p2.setContentLocation(T_NEW_CONTENT_LOC);
        newPayloads = new ArrayList<>(2);
        newPayloads.add(p1); newPayloads.add(p2);

        updManager.setPayloadInformation(userMsg, newPayloads);
        // Check update in entity object
        assertFalse(Utils.isNullOrEmpty(userMsg.getPayloads()));
        assertEquals(2, userMsg.getPayloads().size());
        for (IPayload p : userMsg.getPayloads()) {
            if (p.getContainment() == p1.getContainment()) {
                assertFalse(Utils.isNullOrEmpty(p.getProperties()));
                assertEquals(1, p.getProperties().size());
                assertTrue(CompareUtils.areEqual(p.getProperties().iterator().next(),
                                                 p1.getProperties().iterator().next()));
            } else if (p.getContainment() == p2.getContainment()) {
                assertTrue(Utils.isNullOrEmpty(p.getProperties()));
                assertEquals(p2.getContentLocation(), p.getContentLocation());
            } else
                fail("unknown payload");
        }
        // Check that database is updated
        em.refresh(userMsgJPA);
        assertFalse(Utils.isNullOrEmpty(userMsgJPA.getPayloads()));
        assertEquals(2, userMsgJPA.getPayloads().size());
        for (IPayload p : userMsgJPA.getPayloads()) {
            if (p.getContainment() == p1.getContainment()) {
                assertFalse(Utils.isNullOrEmpty(p.getProperties()));
                assertEquals(1, p.getProperties().size());
                assertTrue(CompareUtils.areEqual(p.getProperties().iterator().next(),
                                                 p1.getProperties().iterator().next()));
            } else if (p.getContainment() == p2.getContainment()) {
                assertTrue(Utils.isNullOrEmpty(p.getProperties()));
                assertEquals(p2.getContentLocation(), p.getContentLocation());
            } else
                fail("unknown payload");
        }
    }

    @Test
    public void setAddSOAPFault() throws PersistenceException {
        // Add a message unit to the database so we can change it
        em.getTransaction().begin();
        ErrorMessage errorMsgJPA = new ErrorMessage(TestData.error4);
        em.persist(errorMsgJPA);
        ErrorMessageEntity errorMsg = new ErrorMessageEntity(errorMsgJPA);
        em.getTransaction().commit();
        // Check it accidentially has not already the new value
        assertNotEquals(T_NEW_SHOULD_HAVE_FAULT, errorMsg.shouldHaveSOAPFault());
        // Perform the update
        updManager.setAddSOAPFault(errorMsg, T_NEW_SHOULD_HAVE_FAULT);
        // Check update in entity object
        assertEquals(T_NEW_SHOULD_HAVE_FAULT, errorMsg.shouldHaveSOAPFault());
        // Check that database is updated
        em.refresh(errorMsgJPA);
        assertEquals(T_NEW_SHOULD_HAVE_FAULT, errorMsgJPA.shouldHaveSOAPFault());
    }

    @Test
    public void sameLoadedState() throws PersistenceException {
        // Store a User Message
        UserMessageEntity userMsg =
                    updManager.storeMessageUnit(new org.holodeckb2b.common.messagemodel.UserMessage(TestData.userMsg1));
        assertTrue(userMsg.isLoadedCompletely());

        // Update it and check it is still loaded completely
        updManager.setProcessingState(userMsg, userMsg.getCurrentProcessingState().getState(), T_NEW_PROC_STATE_1);

        assertEquals(T_NEW_PROC_STATE_1, userMsg.getCurrentProcessingState().getState());
        assertTrue(userMsg.isLoadedCompletely());
        assertFalse(Utils.isNullOrEmpty(userMsg.getPayloads()));

        // Also check that when the indicator is not set previously the object does not load on update
        em.getTransaction().begin();
        ErrorMessage errorMsgJPA = new ErrorMessage(TestData.error4);
        em.persist(errorMsgJPA);
        em.getTransaction().commit();
        ErrorMessageEntity errorMsg = new ErrorMessageEntity(em.find(ErrorMessage.class, errorMsgJPA.getOID()));
        assertFalse(errorMsg.isLoadedCompletely());

        // Update it and check it is still not loaded completely
        updManager.setProcessingState(errorMsg, errorMsg.getCurrentProcessingState().getState(), T_NEW_PROC_STATE_2);
        assertEquals(T_NEW_PROC_STATE_2, errorMsg.getCurrentProcessingState().getState());
        assertFalse(errorMsg.isLoadedCompletely());
        try {
            Utils.isNullOrEmpty(errorMsg.getErrors());
            fail();
        } catch (LazyInitializationException notLoaded) {
            // This is expected!
        }
    }
}
