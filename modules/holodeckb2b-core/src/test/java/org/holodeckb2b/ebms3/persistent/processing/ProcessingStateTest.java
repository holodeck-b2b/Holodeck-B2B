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
package org.holodeckb2b.ebms3.persistent.processing;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
public class ProcessingStateTest {
    
    private static final String T_NAME_1 = "s782345892345645876452387621478612786";
    
    private static final String T_NAME_2 = "f359741398457687543658746587568761211";
    private static final Date   T_START_2 = new Date(110, 1, 1, 10, 0);
    
    EntityManager       em;

    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<ProcessingState> tps = em.createQuery("from ProcessingState", ProcessingState.class).getResultList();
        
        for(ProcessingState mu : tps)
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
    
    public ProcessingStateTest() {
    }
    
    /**
     * Test of non default constructor
     */
    @Test
    public void test01_Constructor() {
        ProcessingState instance = new ProcessingState(T_NAME_1);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();    
        
        try {
            em.getTransaction().begin();
            List<ProcessingState> tps = em.createQuery("from ProcessingState", ProcessingState.class).getResultList();

            assertTrue(tps.size() >= 1);

            for(ProcessingState p : tps) {
                if( p.getName() != null && p.getName().equals(T_NAME_1)) {
                    assertNotNull(p.getStartTime());
                    return;
                }
            }

            fail();
        } finally {
            em.getTransaction().commit();    
        }
    }
    
    /**
     * Test of setName method, of class ProcessingState.
     */
    @Test
    public void test02_SetName() {
        ProcessingState instance = new ProcessingState();
        
        instance.setName(T_NAME_2);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();        

        try {
            em.getTransaction().begin();
            List<ProcessingState> tps = em.createQuery("from ProcessingState", ProcessingState.class).getResultList();

            assertTrue(tps.size() >= 1);

            for(ProcessingState p : tps) {
                if( p.getName() != null && p.getName().equals(T_NAME_2)) {
                    assertNull(p.getStartTime());
                    return;
                }
            }

            fail();
        } finally {
            em.getTransaction().commit();    
        }
        
    }
    
    @Test
    /**
     * Method renamed due to problem with JUnit execution 
     */
    public void test03_SetStartTime() {
        em.getTransaction().begin();
        List<ProcessingState> tps = em.createQuery("from ProcessingState", ProcessingState.class).getResultList();
        tps.get(1).setStartTime(T_START_2);
        em.getTransaction().commit();        
    }

    /**
     * Test of getStartTime method, of class ProcessingState.
     */
    @Test
    public void test04_GetStartTime() {
        em.getTransaction().begin();
        List<ProcessingState> tps = em.createQuery("from ProcessingState", ProcessingState.class).getResultList();
        assertEquals(T_START_2, tps.get(1).getStartTime());
        em.getTransaction().commit();        
    }    
}