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

import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.persistent.wrappers.EAgreementReference;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
public class AgreementReferenceTest {
    
    private static final String T_AGREEMENT_NAME = "test-agree-1";
    private static final String T_AGREEMENT_TYPE = "test-agree-type";
    
    private static final String T_PMODE = "t_pmode_text";
    
    private static final String T_PMODE_2 = "t_pmode_constructor";
    
    private static final String T_AGREEMENT_NAME_2 = "test-agree-2";
    private static final String T_AGREEMENT_TYPE_2 = "test-agree-tp2";
    
    private static final String T_PMODE_3 = "t_copy_construct_pm";
    private static final String T_AGREEMENT_NAME_3 = "test-copy-3";
    private static final String T_AGREEMENT_TYPE_3 = "test-copy-tp3";
    
    
    EntityManager   em;
    
    public AgreementReferenceTest() {
    }

    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        for(EAgreementReference mu : tps)
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
     * Test of setName method, of class AgreementReference.
     */
    @Test
    public void test1_SetName() {
        System.out.println("setName");
        EAgreementReference instance = new EAgreementReference();
        instance.eAgreement.setName(T_AGREEMENT_NAME);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }
    
    /**
     * Test of getName method, of class AgreementReference.
     */
    @Test
    public void test2_GetName() {
        System.out.println("getName");
        
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_AGREEMENT_NAME, tps.get(0).eAgreement.getName());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setType method, of class AgreementReference.
     */
    @Test
    public void test3_SetType() {
        System.out.println("setType");
        
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eAgreement.setType(T_AGREEMENT_TYPE);
        
        em.persist(tps.get(0));
        
        em.getTransaction().commit();
    }


    /**
     * Test of getType method, of class AgreementReference.
     */
    @Test
    public void test4_GetType() {
        System.out.println("getType");
        
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_AGREEMENT_TYPE, tps.get(0).eAgreement.getType());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setPModeId method, of class AgreementReference.
     */
    @Test
    public void test5_SetPModeId() {
        System.out.println("setPModeId");
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eAgreement.setPModeId(T_PMODE);
        
        em.persist(tps.get(0));
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of getPModeId method, of class AgreementReference.
     */
    @Test
    public void test6_GetPModeId() {
        System.out.println("getPModeId");
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_PMODE, tps.get(0).eAgreement.getPModeId());
        
        em.getTransaction().commit();
    }

    /**
     * Test of non default constructor with P-Mode ID
     */
    @Test
    public void test7_PModeConstructor() {
        System.out.println("pModeConstructor");
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eAgreement = new AgreementReference(T_PMODE_2);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_PMODE_2, tps.get(0).eAgreement.getPModeId());
        assertNull(tps.get(0).eAgreement.getName());
        assertNull(tps.get(0).eAgreement.getType());
        
        em.getTransaction().commit();
    }

    /**
     * Test of non default constructor with agreement information 
     */
    @Test
    public void test8_AgreementConstructor() {
        System.out.println("AgreementConstructor");
        em.getTransaction().begin();
        List<EAgreementReference> tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eAgreement = new AgreementReference(T_AGREEMENT_NAME_2, T_AGREEMENT_TYPE_2);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from EAgreementReference", EAgreementReference.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_AGREEMENT_NAME_2, tps.get(0).eAgreement.getName());
        assertEquals(T_AGREEMENT_TYPE_2, tps.get(0).eAgreement.getType());
        assertNull(tps.get(0).eAgreement.getPModeId());
        
        em.getTransaction().commit();
    }
}