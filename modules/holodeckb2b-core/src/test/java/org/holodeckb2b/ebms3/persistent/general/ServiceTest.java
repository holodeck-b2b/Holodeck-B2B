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
package org.holodeckb2b.ebms3.persistent.general;

import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.persistency.entities.Service;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.persistent.wrappers.EService;
import org.holodeckb2b.ebms3.persistent.dao.TestJPAUtil;
import org.junit.After;
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
public class ServiceTest {
    
    private static final String T_NAME_1 = "urn:test:service:testid:0";
    private static final String T_NAME_2 = "second-service";
    private static final String T_NAME_3 = "non standard service";
    private static final String T_NAME_4 = "copied service";
    
    private static final String T_SVC_TP_1 = "service-type:id:scheme:1";
    private static final String T_SVC_TP_3 = "service-type:id:scheme:2";
    private static final String T_SVC_TP_4 = "service-type:id:scheme:3";
    
    
    EntityManager   em;
    
    public ServiceTest() {
    }
    
    @Before
    public void setUp() throws DatabaseException {
        em = TestJPAUtil.getEntityManager();
    }
    
    @After
    public void tearDown() {
        em.close();
    }

    /**
     * Test of setName method, of class Service.
     */
    @Test
    public void test1_SetName() {
        System.out.println("setName");
        EService instance = new EService();
        instance.eService.setName(T_NAME_1);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }    
    
    /**
     * Test of getName method, of class Service.
     */
    @Test
    public void test2_GetName() {
        System.out.println("getName");

        em.getTransaction().begin();
        List<EService> tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_NAME_1, tps.get(0).eService.getName());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setType method, of class Service.
     */
    @Test
    public void test3_SetType() {
        System.out.println("setType");
        
        em.getTransaction().begin();
        List<EService> tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eService.setType(T_SVC_TP_1);
        
        em.persist(tps.get(0));
        
        em.getTransaction().commit();
    }

    /**
     * Test of getType method, of class Service.
     */
    @Test
    public void test4_GetType() {
        System.out.println("getType");

        em.getTransaction().begin();
        List<EService> tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_SVC_TP_1, tps.get(0).eService.getType());
        
        em.getTransaction().commit();
    }

    /**
     * Test of non default constructor with only name
     */
    @Test
    public void test5_NameConstructor() {
        System.out.println("NameConstructor");
        em.getTransaction().begin();
        List<EService> tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eService = new Service(T_NAME_2);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_NAME_2, tps.get(0).eService.getName());
        assertNull(tps.get(0).eService.getType());
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of non default constructor with both name and type set
     */
    @Test
    public void test6_ServiceConstructor() {
        System.out.println("ServiceConstructor");
        em.getTransaction().begin();
        List<EService> tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eService = new Service(T_NAME_3, T_SVC_TP_3);
        
        em.persist(tps.get(0));
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from EService", EService.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_NAME_3, tps.get(0).eService.getName());
        assertEquals(T_SVC_TP_3, tps.get(0).eService.getType());
        
        em.getTransaction().commit();
    }    
}