/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistency.entities;

import org.holodeckb2b.ebms3.persistency.entities.AgreementReference;
import org.holodeckb2b.ebms3.persistency.entities.CollaborationInfo;
import org.holodeckb2b.ebms3.persistency.entities.Service;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.persistent.wrappers.ECollaborationInfo;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CollaborationInfoTest {
    
    private static final String T_SVC_NAME_1 = "test-service:0";
    
    private static final String T_ACTION_1 = "test-action:0";
    
    private static final String T_AGREEMENT_NAME_1 = "test-agree-1";
    private static final String T_AGREEMENT_TYPE_1 = "test-agree-tp1";
    
    private static final String T_PMODE_1 = "pmode-id0";
    
    private static final String T_SVC_NAME_2 = "test-service:1";
    private static final String T_ACTION_2 = "test-action:1";
    private static final String T_PMODE_2 = "pmode-id1";

    private static final String T_SVC_NAME_3 = "test-service:2";
    private static final String T_ACTION_3 = "test-action:2";
    private static final String T_AGREEMENT_NAME_3 = "test-agree-2";
    private static final String T_AGREEMENT_TYPE_3 = "test-agree-tp2";
    
    private static final String T_SVC_NAME_4 = "test-service:3";
    private static final String T_ACTION_4 = "test-action:3";
    private static final String T_AGREEMENT_NAME_4 = "test-agree-3";
    private static final String T_AGREEMENT_TYPE_4 = "test-agree-tp3";
    
    EntityManager   em;
    
    public CollaborationInfoTest() {
    }

    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        for(ECollaborationInfo mu : tps)
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
     * Test of setService method, of class CollaborationInfo.
     */
    @Test
    public void test1_SetService() {
        System.out.println("setService");
        Service service = new Service(T_SVC_NAME_1);
        
        ECollaborationInfo instance = new ECollaborationInfo();
        instance.eCollaborationInfo.setService(service);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();                
    }

    /**
     * Test of getService method, of class CollaborationInfo.
     */
    @Test
    public void test2_GetService() {
        System.out.println("getService");
        
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_SVC_NAME_1, tps.get(0).eCollaborationInfo.getService().getName());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setAction method, of class CollaborationInfo.
     */
    @Test
    public void test3_SetAction() {
        System.out.println("setAction");
        
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        tps.get(0).eCollaborationInfo.setAction(T_ACTION_1);
        
        em.getTransaction().commit();
    }

    /**
     * Test of getAction method, of class CollaborationInfo.
     */
    @Test
    public void test4_GetAction() {
        System.out.println("getAction");
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_ACTION_1, tps.get(0).eCollaborationInfo.getAction());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setAgreement method, of class CollaborationInfo.
     */
    @Test
    public void test5_SetAgreement() {
        System.out.println("setAgreement");
        AgreementReference ref = new AgreementReference(T_AGREEMENT_NAME_1, T_AGREEMENT_TYPE_1);
        
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        tps.get(0).eCollaborationInfo.setAgreement(ref);
        em.getTransaction().commit();
    }

    /**
     * Test of getAgreement method, of class CollaborationInfo.
     */
    @Test
    public void test6_GetAgreement() {
        System.out.println("getAgreement");
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_AGREEMENT_NAME_1, tps.get(0).eCollaborationInfo.getAgreement().getName());
        assertEquals(T_AGREEMENT_TYPE_1, tps.get(0).eCollaborationInfo.getAgreement().getType());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setPModeId method, of class CollaborationInfo.
     */
    @Test
    public void test7_SetPModeId() {
        System.out.println("setPModeId");
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eCollaborationInfo.setPModeId(T_PMODE_1);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_PMODE_1, tps.get(0).eCollaborationInfo.getAgreement().getPModeId());
        
        assertEquals(T_AGREEMENT_NAME_1, tps.get(0).eCollaborationInfo.getAgreement().getName());
        assertEquals(T_AGREEMENT_TYPE_1, tps.get(0).eCollaborationInfo.getAgreement().getType());
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of non default constructor with only strings 
     */
    @Test
    public void test8_StringConstructor() {
        System.out.println("StringConstructor");
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eCollaborationInfo = new CollaborationInfo(T_SVC_NAME_2, T_ACTION_2, T_PMODE_2);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_SVC_NAME_2, tps.get(0).eCollaborationInfo.getService().getName());
        assertEquals(T_ACTION_2, tps.get(0).eCollaborationInfo.getAction());
        assertEquals(T_PMODE_2, tps.get(0).eCollaborationInfo.getAgreement().getPModeId());
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of non default constructor with service and agreement objects
     */
    @Test
    public void test9_ObjectConstructor() {
        System.out.println("ObjectConstructor");
        
        Service svcObj = new Service(T_SVC_NAME_3);
        AgreementReference refObj = new AgreementReference(T_AGREEMENT_NAME_3, T_AGREEMENT_TYPE_3);
        
        em.getTransaction().begin();
        List<ECollaborationInfo> tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eCollaborationInfo = new CollaborationInfo(svcObj, T_ACTION_3, refObj);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from ECollaborationInfo", ECollaborationInfo.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_SVC_NAME_3, tps.get(0).eCollaborationInfo.getService().getName());
        assertEquals(T_ACTION_3, tps.get(0).eCollaborationInfo.getAction());
        assertEquals(T_AGREEMENT_NAME_3, tps.get(0).eCollaborationInfo.getAgreement().getName());
        assertEquals(T_AGREEMENT_TYPE_3, tps.get(0).eCollaborationInfo.getAgreement().getType());
        
        em.getTransaction().commit();
    }
    
}