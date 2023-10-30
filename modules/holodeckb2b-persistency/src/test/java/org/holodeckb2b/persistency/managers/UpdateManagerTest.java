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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import org.apache.axis2.AxisFault;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;
import org.holodeckb2b.persistency.entities.ErrorMessageEntity;
import org.holodeckb2b.persistency.entities.MessageUnitEntity;
import org.holodeckb2b.persistency.entities.PayloadEntity;
import org.holodeckb2b.persistency.entities.PullRequestEntity;
import org.holodeckb2b.persistency.entities.ReceiptEntity;
import org.holodeckb2b.persistency.entities.UserMessageEntity;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.Payload;
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
    private static UpdateManager   updManager;
    private static EntityManager   em;

    @BeforeClass
    public static void setUpClass() throws PersistenceException, AxisFault {
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
    public void storeUserMessage() throws PersistenceException, DuplicateMessageIdException {
        // Store a User Message
        UserMessageEntity userMsg = updManager.storeMessageUnit(TestData.userMsg1);

        assertNotNull(userMsg);
        assertNotNull(userMsg.getOID());
        assertNotNull(userMsg.getCoreId());
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
    public void storeReceipt() throws PersistenceException, DuplicateMessageIdException {
        ReceiptEntity receiptEntity = updManager.storeMessageUnit(TestData.receipt6);
        assertNotNull(receiptEntity);
        assertNotNull(receiptEntity.getCoreId());
        assertEquals(TestData.receipt6.getMessageId(), receiptEntity.getMessageId());
        assertEquals(TestData.receipt6.getRefToMessageId(), receiptEntity.getRefToMessageId());
        assertEquals(TestData.receipt6.getTimestamp(), receiptEntity.getTimestamp());

        assertFalse(Utils.isNullOrEmpty(receiptEntity.getContent()));

        assertEquals(TestData.receipt6.getContent().get(0).getQName(), receiptEntity.getContent().get(0).getQName());
        assertEquals(TestData.receipt6.getContent().get(0).getText(), receiptEntity.getContent().get(0).getText());
    }

    @Test
    public void storePullRequest() throws PersistenceException, DuplicateMessageIdException {
        PullRequestEntity pullEntity = updManager.storeMessageUnit(TestData.pull5);
        assertNotNull(pullEntity);
        assertNotNull(pullEntity.getCoreId());
        assertEquals(TestData.pull5.getMessageId(), pullEntity.getMessageId());
        assertEquals(TestData.pull5.getRefToMessageId(), pullEntity.getRefToMessageId());
        assertEquals(TestData.pull5.getTimestamp(), pullEntity.getTimestamp());
    }
    
    @Test
    public void storeSelectivePullRequest() throws PersistenceException, DuplicateMessageIdException {
        PullRequestEntity pullEntity = updManager.storeMessageUnit(TestData.pull6);
        assertNotNull(pullEntity);
        assertNotNull(pullEntity.getCoreId());
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
    public void storeErrorMessage () throws DuplicateMessageIdException, PersistenceException {
    	ErrorMessageEntity errEntity = updManager.storeMessageUnit(TestData.error3);    	
        assertNotNull(errEntity);
        assertNotNull(errEntity.getCoreId());
        
        assertEquals(TestData.error3.getMessageId(), errEntity.getMessageId());
        assertEquals(TestData.error3.getRefToMessageId(), errEntity.getRefToMessageId());
        assertEquals(TestData.error3.getTimestamp(), errEntity.getTimestamp());
        
        Collection<IEbmsError> errors = TestData.error3.getErrors();
        Collection<IEbmsError> storedErrors = errEntity.getErrors();
        
        assertEquals(errors.size(), storedErrors.size());
        for(IEbmsError e : errors)
        	assertTrue(storedErrors.stream().anyMatch(se -> Utils.nullSafeEqual(e.getCategory(), se.getCategory())
        												 && Utils.nullSafeEqual(e.getErrorCode(), se.getErrorCode())
        												 && Utils.nullSafeEqual(e.getMessage(), se.getMessage())
        												 && Utils.nullSafeEqual(e.getErrorDetail(), se.getErrorDetail())
        												 && Utils.nullSafeEqual(e.getSeverity(), se.getSeverity())        			
        											 ));        
    }
    
    @Test
    public void testDuplicateMessageId() throws PersistenceException {
    	UserMessageEntity userMsg = null;
    	try {
    		userMsg = updManager.storeMessageUnit(TestData.userMsg1);
    	} catch (DuplicateMessageIdException duplicateId) {
    		fail();
    	}
    	
    	org.holodeckb2b.common.messagemodel.UserMessage dup = new org.holodeckb2b.common.messagemodel.UserMessage();
    	dup.setMessageId(userMsg.getMessageId());
    	dup.setDirection(Direction.OUT);
    	
    	try {
    		updManager.storeMessageUnit(dup);
    		fail();
    	} catch (DuplicateMessageIdException duplicateId) {    		
    	}    	
    }
    
    @Test
    public void testUpdateUserMessage() throws DuplicateMessageIdException, PersistenceException {
    	UserMessageEntity userMsg = updManager.storeMessageUnit(TestData.userMsg1);

    	ProcessingState newState = ProcessingState.PROCESSING;
    	userMsg.setProcessingState(newState, null);
    	
    	String relatedId = UUID.randomUUID().toString();
    	userMsg.addRelatesTo(relatedId);
    	
    	boolean multiHop = !userMsg.usesMultiHop();
    	userMsg.setMultiHop(multiHop);
    	
    	try {
    		updManager.updateMessageUnit(userMsg);
    	} catch (Exception e) {
    		fail();
    	}
    	assertEquals(newState, userMsg.getCurrentProcessingState().getState());
    	
    	assertEquals(multiHop, userMsg.usesMultiHop());
    	
    	assertTrue(userMsg.getRelatedTo().contains(relatedId));    	
    	
		UserMessage stored = em.find(UserMessage.class, userMsg.getJPAObject().getOID());
	
    	assertEquals(newState, stored.getCurrentProcessingState().getState());
    	
    	assertEquals(multiHop, stored.usesMultiHop());
    	
    	assertTrue(stored.getRelatedTo().contains(relatedId));    	
    }
    
    @Test
    public void testUpdatePayload() throws DuplicateMessageIdException, PersistenceException {
    	UserMessageEntity userMsg = updManager.storeMessageUnit(TestData.userMsg1);
    	
    	PayloadEntity payload = (PayloadEntity) userMsg.getPayloads().iterator().next();
    	
    	String location = UUID.randomUUID().toString();
    	payload.setContentLocation(location);
    	
    	String mt = "application/test";
    	payload.setMimeType(mt);
    	
    	String uri = UUID.randomUUID().toString();
    	payload.setPayloadURI(uri);
    	
    	Property p = new Property("newProp", "newValue");
    	payload.addProperty(p);
    	
    	try {
    		updManager.updatePayload(payload);
    	} catch (Exception e) {
    		fail();
    	}
    	assertEquals(location, payload.getContentLocation());
    	assertEquals(mt, payload.getMimeType());
    	assertEquals(uri, payload.getPayloadURI());
    	
    	assertTrue(payload.getProperties().stream().anyMatch(sp -> CompareUtils.areEqual(p, sp)));
    	
    	assertNotNull(payload.getSchemaReference());
    	
    	Payload stored = em.find(Payload.class, payload.getJPAObject().getOID());
    	
    	assertEquals(location, stored.getContentLocation());
    	assertEquals(mt, stored.getMimeType());
    	assertEquals(uri, stored.getPayloadURI());
    	
    	assertTrue(stored.getProperties().stream().anyMatch(sp -> CompareUtils.areEqual(p, sp)));
    	
    	assertNotNull(stored.getSchemaReference());
    }

    @Test
    public void testKeepCompletelyLoaded() throws DuplicateMessageIdException, PersistenceException {
    	IUserMessageEntity stored = (IUserMessageEntity) 
    						HolodeckB2BCoreInterface.getQueryManager()
    									.getMessageUnitWithCoreId(updManager.storeMessageUnit(TestData.userMsg1)
    																		.getCoreId());
    	
    	HolodeckB2BCoreInterface.getQueryManager().ensureCompletelyLoaded(stored);
    	
    	ProcessingState newState = ProcessingState.OUT_FOR_DELIVERY;
    	
    	stored.setProcessingState(newState, null);
    	
    	updManager.updateMessageUnit(stored);
    	
    	assertTrue(stored.isLoadedCompletely());
    	try {
    		stored.getPayloads();
    	} catch (Exception notCompletelyLoaded) {
    		fail();
    	}
    }    
    
    @SuppressWarnings("rawtypes")
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
}
