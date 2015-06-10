/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistent.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import org.holodeckb2b.common.general.IPartyId;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.general.Property;
import org.holodeckb2b.ebms3.persistent.general.TradingPartner;
import org.holodeckb2b.ebms3.persistent.processing.ProcessingState;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserMessageTest {
    
    private static final String T_MSG_ID = "63768761876278234678624@msg-id.org";
    private static final String T_MSG_ID_1 = "6376dfjk21876278234678624@msg-id.org";
    private static final String T_REF_MSG_ID = "98765432109876543210@ref-msg-id.org";
    
    private static final String T_PMODE_ID = "pmodeid-1";
    
    
    private static final String T_MPC_1 = "http://test-mpc/1";
    
    private static final String T_SENDER1_ID = "urn:holodeckb2b:test:sender1";
    private static final String T_SENDER1_ROLE = "sender";
    
    private static final String T_SENDER2_ID = "urn:holodeckb2b:test:sender2";
    private static final String T_SENDER2_ROLE = "sender2";

    private static final String T_RECEIVER1_ID = "urn:holodeckb2b:test:rcvr1";
    private static final String T_RECEIVER1_ROLE = "rcvr1";
    
    private static final String T_RECEIVER2_ID = "urn:holodeckb2b:test:rcvr2";
    private static final String T_RECEIVER2_ROLE = "rcvr2";
    
    private static final String T_COLLABINFO_SVC = "test_service";
    private static final String T_COLLABINFO_ACTION = "test_action";
    private static final String T_COLLABINFO_PMODE = "test-pmodeid";
    
    private static final String T_PROP_1_NAME = "prop-1";
    private static final String T_PROP_1_VALUE = "val-1";
    private static final String T_PROP_2_NAME = "prop-2";
    private static final String T_PROP_2_VALUE = "val-2";
    private static final String T_PROP_2_TYPE = "type-2";
    private static final String T_PROP_3_NAME = "prop-3";
    private static final String T_PROP_3_VALUE = "val-3";
    
    private static final Date   T_TIMESTAMP = new Date(110, 1, 1, 10, 0);
    
    private static final Payload    T_PAYLOAD_1 = new Payload("/file/test/holodeckb2b/pl1", "text/xml");
    private static final Payload    T_PAYLOAD_2 = new Payload("/file/test/holodeckb2b/pl2", "image/jpeg", "cid:dgjgfhjgfjhghjsghjsg@holodeckb2b.org");
    private static final Payload    T_PAYLOAD_3 = new Payload("/file/test/holodeckb2b/pl3", "application/octet-stream", "cid:gdgdhstshstaakk@holodeckb2b.org");
    
    private static ProcessingState    T_PROCSTATE_1 = new ProcessingState("START");
    private static ProcessingState    T_PROCSTATE_2 = new ProcessingState("FINISH");   
    
    EntityManager   em;
    
    public UserMessageTest() {
    }
    
    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        for(UserMessage mu : tps)
            em.remove(mu);
        
        em.getTransaction().commit();
    }
    
    @Before
    public void setUp() {
        em = JPAUtil.getEntityManager();
    }
    
    @After
    public void tearDown() {
        em.close();
    }

    /**
     * Test of setMPC method, of class UserMessage.
     */
    @Test
    public void test01_SetMPC() {
        UserMessage instance = new UserMessage();
        
        instance.setMPC(T_MPC_1);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }
    
    /**
     * Test of getMPC method, of class UserMessage.
     */
    @Test
    public void test02_GetMPC() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_MPC_1, tps.get(0).getMPC());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setSender method, of class UserMessage.
     */
    @Test
    public void test03_SetSender() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        TradingPartner sender = new TradingPartner(T_SENDER1_ID, T_SENDER1_ROLE);
        
        assertTrue(tps.size() == 1);

        instance = tps.get(0);
        
        instance.setSender(sender);
        
        em.persist(sender);
        em.persist(instance);
        em.getTransaction().commit();
    }
    
    /**
     * Test of getSender method, of class UserMessage.
     */
    @Test
    public void test04_GetSender() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_SENDER1_ROLE, instance.getSender().getRole());
        
        Iterator<IPartyId> pids = instance.getSender().getPartyIds().iterator();
        if (pids.hasNext()) {
            assertEquals(T_SENDER1_ID ,pids.next().getId());
        } else 
            // There should be a partyid
            fail("Not retrieved the correct Sender");
        
        em.getTransaction().commit();
    }

    /**
     * Test if setSender correctly changes the Sender
     */
    @Test
    public void test05_ChangeSender() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        TradingPartner sender = new TradingPartner(T_SENDER2_ID, T_SENDER2_ROLE);
        
        assertTrue(tps.size() == 1);
        
        instance = tps.get(0);
        
        instance.setSender(sender);
        
        em.persist(sender);
        em.persist(instance);
        em.getTransaction().commit();

        em.getTransaction().begin();
        tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        instance = tps.get(0);
        
        assertEquals(T_SENDER2_ROLE, instance.getSender().getRole());
        
        Iterator<IPartyId> pids = instance.getSender().getPartyIds().iterator();
        if (pids.hasNext()) {
            assertEquals(T_SENDER2_ID ,pids.next().getId());
        } else 
            // There should be a partyid
            fail("Not retrieved the correct Sender");
        
        em.getTransaction().commit();
    }    
    
    /**
     * Test of setReceiver method, of class UserMessage.
     */
    @Test
    public void test06_SetReceiver() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        TradingPartner receiver = new TradingPartner(T_RECEIVER1_ID, T_RECEIVER1_ROLE);
        
        assertTrue(tps.size() == 1);

        instance = tps.get(0);
        
        instance.setReceiver(receiver);
        
        em.persist(receiver);
        em.persist(instance);
        em.getTransaction().commit();
    }
    
    /**
     * Test of getReceiver method, of class UserMessage.
     */
    @Test
    public void test07_GetReceiver() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_RECEIVER1_ROLE, instance.getReceiver().getRole());
        
        Iterator<IPartyId> pids = instance.getReceiver().getPartyIds().iterator();
        if (pids.hasNext()) {
            assertEquals(T_RECEIVER1_ID ,pids.next().getId());
        } else 
            // There should be a partyid
            fail("Not retrieved the correct Sender");
        
        em.getTransaction().commit();
    }

    /**
     * Test if setReceiver correctly changes the Receiver
     */
    @Test
    public void test08_ChangeReceiver() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        TradingPartner receiver = new TradingPartner(T_SENDER2_ID, T_SENDER2_ROLE);
        
        assertTrue(tps.size() == 1);
        
        instance = tps.get(0);
        
        instance.setReceiver(receiver);
        
        em.persist(receiver);
        em.persist(instance);
        em.getTransaction().commit();

        em.getTransaction().begin();
        tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        instance = tps.get(0);
        
        assertEquals(T_SENDER2_ROLE, instance.getReceiver().getRole());
        
        Iterator<IPartyId> pids = instance.getReceiver().getPartyIds().iterator();
        if (pids.hasNext()) {
            assertEquals(T_SENDER2_ID ,pids.next().getId());
        } else 
            // There should be a partyid
            fail("Not retrieved the correct Sender");
        
        em.getTransaction().commit();
    }  

    /**
     * Test of setCollaborationInfo method, of class UserMessage.
     */
    @Test
    public void test09_SetCollaborationInfo() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        CollaborationInfo colInfo = new CollaborationInfo(T_COLLABINFO_SVC, T_COLLABINFO_ACTION, T_COLLABINFO_PMODE);
        
        assertTrue(tps.size() == 1);

        instance = tps.get(0);
        
        instance.setCollaborationInfo(colInfo);
        
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getCollaborationInfo method, of class UserMessage.
     */
    @Test
    public void test10_GetCollaborationInfo() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_COLLABINFO_SVC, instance.getCollaborationInfo().getService().getName());
        assertEquals(T_COLLABINFO_ACTION, instance.getCollaborationInfo().getAction());
        assertEquals(T_COLLABINFO_PMODE, instance.getCollaborationInfo().getAgreement().getPModeId());
    }

    /**
     * Test of setMessageProperties method, of class UserMessage.
     */
    @Test
    public void test11_SetMessageProperties() {
        em.getTransaction().begin();

        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        Collection<Property> props = new HashSet<Property>();
        
        props.add(new Property(T_PROP_1_NAME, T_PROP_1_VALUE));
        props.add(new Property(T_PROP_2_NAME, T_PROP_2_VALUE, T_PROP_2_TYPE));
        
        instance.setMessageProperties(props);
        
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getMessageProperties method, of class UserMessage.
     */
    @Test
    public void test12_GetMessageProperties() {
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        assertEquals(1, tps.size());
        Collection<IProperty> properties = tps.get(0).getMessageProperties();
        
        assertEquals(2, properties.size());
        
        for(IProperty p : properties) {
            if (p.getName().equals(T_PROP_1_NAME)) {
                assertEquals(T_PROP_1_VALUE, p.getValue());
                assertNull(p.getType());
            } else {
                assertEquals(T_PROP_2_NAME, p.getName());
                assertEquals(T_PROP_2_VALUE, p.getValue());
                assertEquals(T_PROP_2_TYPE, p.getType());
            }
        }
    }

    /**
     * Test of addMessageProperty method, of class UserMessage.
     */
    @Test
    public void test13_AddMessageProperty() {
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertEquals(1, tps.size());
        assertEquals(2, tps.get(0).getMessageProperties().size());
        
        em.getTransaction().begin();
        instance = tps.get(0);
        instance.addMessageProperty(new Property(T_PROP_3_NAME, T_PROP_3_VALUE));
        em.getTransaction().commit();
        
        tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        assertEquals(1, tps.size());
        Collection<IProperty> properties = tps.get(0).getMessageProperties();
        
        assertEquals(3, properties.size());
        for(IProperty p : properties) {
            if (p.getName().equals(T_PROP_1_NAME)) {
                assertEquals(T_PROP_1_VALUE, p.getValue());
                assertNull(p.getType());
            } else if (p.getName().equals(T_PROP_2_NAME)) {
                assertEquals(T_PROP_2_VALUE, p.getValue());
                assertEquals(T_PROP_2_TYPE, p.getType());
            } else {
                assertEquals(T_PROP_3_NAME, p.getName());
                assertEquals(T_PROP_3_VALUE, p.getValue());
                assertNull(p.getType());
            }
        }
    }

    /**
     * Test of setPayloads method, of class UserMessage.
     */
    @Test
    public void test14_SetPayloads() {
        List<Payload>   pls = new ArrayList<Payload>();
        
        pls.add(T_PAYLOAD_1); pls.add(T_PAYLOAD_2);
        
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).setPayloads(pls);
         
        em.getTransaction().commit();
    }
   
    /**
     * Test of getPayloads method, of class UserMessage.
     */
    @Test
    public void test15_GetPayloads() {
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        assertTrue(tps.size() == 1);       
        
        assertEquals(2, tps.get(0).getPayloads().size());
        
        for(IPayload pl : tps.get(0).getPayloads()) {
            if(pl.getContentLocation().equals(T_PAYLOAD_1.getContentLocation()))
                assertEquals(T_PAYLOAD_1.getMimeType(), pl.getMimeType());
            else if (pl.getContentLocation().equals(T_PAYLOAD_2.getContentLocation())) {
                assertEquals(T_PAYLOAD_2.getMimeType(), pl.getMimeType());
                assertEquals(T_PAYLOAD_2.getPayloadURI(), pl.getPayloadURI());
            }
        }
    }
    
     /**
     * Test of addPayload method, of class UserMessage.
     */
    @Test
    public void test16_AddPayload() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).addPayload(T_PAYLOAD_3);
        
        em.getTransaction().commit();

        tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        assertEquals(1, tps.size());       
        
        assertEquals(3, tps.get(0).getPayloads().size());
        
        for(IPayload pl : tps.get(0).getPayloads()) {
            if(pl.getContentLocation().equals(T_PAYLOAD_1.getContentLocation()))
                assertEquals(T_PAYLOAD_1.getMimeType(), pl.getMimeType());
            else if (pl.getContentLocation().equals(T_PAYLOAD_2.getContentLocation())) {
                assertEquals(T_PAYLOAD_2.getMimeType(), pl.getMimeType());
                assertEquals(T_PAYLOAD_2.getPayloadURI(), pl.getPayloadURI());
            } if (pl.getContentLocation().equals(T_PAYLOAD_3.getContentLocation())) {
                assertEquals(T_PAYLOAD_3.getMimeType(), pl.getMimeType());
                assertEquals(T_PAYLOAD_3.getPayloadURI(), pl.getPayloadURI());
            }
        }
    }
    
    /**
     * Test of setTimestamp method inherited from MessageUnit
     */
    @Test
    public void test17_SetTimestamp() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        instance.setTimestamp(T_TIMESTAMP);
        
        em.getTransaction().commit();       
    }
    
    /**
     * Test of getTimestamp method inherited from MessageUnit
     */
    @Test
    public void test18_GetTimestamp() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_TIMESTAMP, instance.getTimestamp());
        
        em.getTransaction().commit();         
    }
    
    /**
     * Test of setMessageId method inherited from MessageUnit
     */
    @Test
    public void test19_SetMessageId() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        instance.setMessageId(T_MSG_ID);
        
        em.getTransaction().commit();       
    }
    
    /**
     * Test of getMessageId method inherited from MessageUnit
     */
    @Test
    public void test20_GetMessageId() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_MSG_ID, instance.getMessageId());
        
        em.getTransaction().commit();         
    }

        /**
     * Test of setRefToMessageId method inherited from MessageUnit
     */
    @Test
    public void test21_SetRefToMessageId() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        instance.setRefToMessageId(T_REF_MSG_ID);
        
        em.getTransaction().commit();       
    }
    
    /**
     * Test of getRefToMessageId method inherited from MessageUnit
     */
    @Test
    public void test22_GetRefToMessageId() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_REF_MSG_ID, instance.getRefToMessageId());
        
        em.getTransaction().commit();         
    }
    
    
    
    /**
     * Test of addProcessingState method inherited from MessageUnit
     */
    @Test
    public void test23_AddProcessingState() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        instance.addProcessingState(T_PROCSTATE_1);
        
        em.persist(T_PROCSTATE_1);
        
        em.getTransaction().commit();         
    }

    /**
     * Test of setProcessingState method inherited from MessageUnit
     */
    @Test
    public void test24_SetProcessingState() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        T_PROCSTATE_2.setStartTime(new Date());
        
        instance.setProcessingState(T_PROCSTATE_2);
        
        em.persist(T_PROCSTATE_2);
        
        em.getTransaction().commit();         
    }   
    
    /**
     * Test of getStates method inherited from MessageUnit
     */
    @Test
    public void test25_GetProcessingStates() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertTrue(instance.getProcessingStates().size() == 2);
        assertEquals(T_PROCSTATE_2.getName(), instance.getProcessingStates().get(0).getName());
        assertEquals(T_PROCSTATE_1.getName(), instance.getProcessingStates().get(1).getName());
        
        em.getTransaction().commit();
    }
    
        /**
     * Test of setPMode method inherited from MessageUnit
     */
    @Test
    public void test26_SetPMode() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        instance.setPMode(T_PMODE_ID);
        
        em.getTransaction().commit();       
    }
    
    /**
     * Test of getPMode method inherited from MessageUnit
     */
    @Test
    public void test27_GetPMode() {
        em.getTransaction().begin();
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertEquals(T_PMODE_ID, instance.getPMode());
        
        em.getTransaction().commit();         
    }
    
    /**
     * Test of isDelivered query
     */
    @Test
    public void test28_isDeliveredQuery() {
        em.getTransaction().begin();
        
        List<UserMessage> tps = em.createQuery("from UserMessage", UserMessage.class).getResultList();
        UserMessage instance = tps.get(0);
        
        String msgId = instance.getMessageId();
        
        instance.setProcessingState(new ProcessingState(ProcessingStates.DELIVERED));
        
        em.persist(instance);
        
        em.getTransaction().commit();
        
        Boolean result = new Boolean(em.createNamedQuery("UserMessage.isDelivered",
                                            String.class)
                                            .setParameter("msgId", msgId)
                                            .getSingleResult());
        
        assertTrue(result);
    }
    
    /**
     * Test of numOfRetransmits query
     */
    @Test
    public void test29_numOfRetransmitsQuery() {
        
        em.getTransaction().begin();
        UserMessage instance = new UserMessage();
        
        instance.setMessageId(T_MSG_ID_1);
        instance.setProcessingState(new ProcessingState(ProcessingStates.SUBMITTED));
        instance.setProcessingState(new ProcessingState(ProcessingStates.READY_TO_PUSH));
        instance.setProcessingState(new ProcessingState(ProcessingStates.PROCESSING));
        instance.setProcessingState(new ProcessingState(ProcessingStates.AWAITING_RECEIPT));
        
        em.persist(instance);
        
        em.getTransaction().commit();
        
        Long numberOfTransmits = null;
        
        try {
            numberOfTransmits = em.createNamedQuery("UserMessage.numOfTransmits",
                    Long.class)
                    .setParameter("msgId", T_MSG_ID_1)
                    .getSingleResult();
            
        } catch (NoResultException nr) {
            fail();
        }
        
        assertEquals(1, numberOfTransmits.intValue());
        
        em.getTransaction().begin();
        
        instance.setProcessingState(new ProcessingState(ProcessingStates.READY_TO_PUSH));
        instance.setProcessingState(new ProcessingState(ProcessingStates.PROCESSING));
        instance.setProcessingState(new ProcessingState(ProcessingStates.AWAITING_RECEIPT));
        instance.setProcessingState(new ProcessingState(ProcessingStates.READY_TO_PUSH));
        instance.setProcessingState(new ProcessingState(ProcessingStates.PROCESSING));
        instance.setProcessingState(new ProcessingState(ProcessingStates.AWAITING_RECEIPT));
        
        em.merge(instance);
        
        em.getTransaction().commit();
        
        try {
            numberOfTransmits = em.createNamedQuery("UserMessage.numOfTransmits",
                    Long.class)
                    .setParameter("msgId", T_MSG_ID_1)
                    .getSingleResult();
            
        } catch (NoResultException nr) {
            fail();
        }
        
        assertEquals(3, numberOfTransmits.intValue());
        
    }
}
