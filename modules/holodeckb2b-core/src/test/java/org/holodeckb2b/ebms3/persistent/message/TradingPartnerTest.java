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
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.common.general.IPartyId;
import org.holodeckb2b.ebms3.persistent.general.PartyId;
import org.holodeckb2b.ebms3.persistent.general.TradingPartner;
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
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TradingPartnerTest {
    
    private static final String TEST_ROLE = "testRole";
    
    private static final String TEST_PID = "TrdPrtnr";
    private static final String TEST_PID_TYPE = "TP_type";
    
    EntityManager   em;
    
    public TradingPartnerTest() {
    }

    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<TradingPartner> tps = em.createQuery("from TradingPartner", TradingPartner.class).getResultList();
        
        for(TradingPartner o : tps)
            em.remove(o);
        
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
     * Test of setRole method, of class TradingPartner. Stores a TradingPartner
     * object to the database
     */
    @Test
    public void test1_SetRole() {
        cleanup(); // remove left over items from database before starting test
        System.out.println("setRole");
        TradingPartner instance = new TradingPartner();
        
        instance.setRole(TEST_ROLE);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getRole method, of class TradingPartner. Retrieves object from
     * database and tests for role value
     */
    @Test
    public void test2_GetRole() {
        System.out.println("getRole");
        
        em.getTransaction().begin();
        List<TradingPartner> tps = em.createQuery("from TradingPartner", TradingPartner.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(TEST_ROLE, tps.get(0).getRole());
        
        em.getTransaction().commit();
    }
 
    /**
     * Test of addPartyId method, of class TradingPartner. Adds two partyids to a trading 
     * partner, one with and one without a type
     */
    @Test
    public void test3_AddPartyId() {
        System.out.println("addPartyId");
        
        em.getTransaction().begin();
        List<TradingPartner> tps = em.createQuery("from TradingPartner", TradingPartner.class).getResultList();
        
        TradingPartner instance;
        if (tps.size() == 1)
           instance = tps.get(0);
        else
           instance = new TradingPartner();
        
        PartyId pid0 = new PartyId(); pid0.setId(TEST_PID+"0");
        PartyId pid1 = new PartyId(); pid1.setId(TEST_PID+"1"); pid1.setType(TEST_PID_TYPE);
        
        instance.addPartyId(pid0);
        assertTrue(instance.getPartyIds().size() == 1);
        
        instance.addPartyId(pid1);
        assertTrue(instance.getPartyIds().size() == 2);
        
        em.persist(instance);
        
        em.getTransaction().commit();
    }
    
 
    /**
     * Test of getPartyIds method, of class TradingPartner.
     */
    @Test
    public void test4_GetPartyIds() {
        System.out.println("getPartyId");
        
        em.getTransaction().begin();
        List<TradingPartner> tps = em.createQuery("from TradingPartner", TradingPartner.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        Collection<IPartyId> pids = tps.get(0).getPartyIds();
        
        assertTrue(pids.size() == 2);
        
        for(IPartyId pid : pids) {
            if( pid.getId().equals(TEST_PID+"0"))
                assertNull(pid.getType());
            else if (pid.getId().equals(TEST_PID+"1"))
                assertEquals(TEST_PID_TYPE, pid.getType());
            else
                fail("Unexpected PartId in collection" + pid.getId());            
        }
                    
        em.getTransaction().commit();
    }    

    /**
     * Test of non default constructor with untyped PartyId
     */
    @Test
    public void test5_ConstructorWithUnTypedPartyId() {
        System.out.println("TradingPartner(String partyId, String role)");
        TradingPartner instance = new TradingPartner(TEST_PID+"str", TEST_ROLE+"str");
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        List<TradingPartner> tps = em.createQuery("from TradingPartner", TradingPartner.class).getResultList();
        
        assertTrue(tps.size() == 2);
        assertEquals(TEST_ROLE+"str", tps.get(1).getRole());
        
        Collection<IPartyId> pids = tps.get(1).getPartyIds();
        assertTrue(pids.size() == 1);
        Iterator<IPartyId> it = pids.iterator();
        IPartyId pid = it.next();
        assertEquals(TEST_PID+"str", pid.getId());
        assertNull(pid.getType());
                    
        em.getTransaction().commit();
    }
    
    /**
     * Test of non default constructor with typed PartyId
     */
    @Test
    public void test6_ConstructorWithTypedPartyId() {
        System.out.println("TradingPartner(IPartyId partyId, String role)");
        TradingPartner instance = new TradingPartner(new PartyId(TEST_PID+"typed", TEST_PID_TYPE+"typed"), TEST_ROLE+"typed");
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
        
                em.getTransaction().begin();
        List<TradingPartner> tps = em.createQuery("from TradingPartner", TradingPartner.class).getResultList();
        
        assertTrue(tps.size() == 3);
        assertEquals(TEST_ROLE+"typed", tps.get(2).getRole());
        
        Collection<IPartyId> pids = tps.get(2).getPartyIds();
        assertTrue(pids.size() == 1);
        Iterator<IPartyId> it = pids.iterator();
        IPartyId pid = it.next();
        assertEquals(TEST_PID+"typed", pid.getId());
        assertEquals(TEST_PID_TYPE+"typed", pid.getType());
                    
        em.getTransaction().commit();
    }    

}
