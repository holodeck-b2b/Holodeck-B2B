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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.common.general.IAuthenticationInfo;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for the PullRequest persistency object
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PullRequestTest {
        
    static class TestAuthInfo implements IAuthenticationInfo, Serializable {
        protected String    value = "TEST";
        public void setValue(String v) {value = v;}
        public String getValue() {return value;}

        @Override
        public boolean equals(IAuthenticationInfo ai) {
           boolean r = false;
           if (ai instanceof TestAuthInfo) {
               return ((TestAuthInfo) ai).getValue().equals(this.value);
           } else
               return false;
        }
    };
    
    EntityManager   em;
    
    private static final IAuthenticationInfo TEST_AUTH_INFO = new TestAuthInfo(); 
    
    private static final String T_MPC_1 = "http://holodeck-b2b.org/test/pull/mpc1";
    
    
    public PullRequestTest() {
    }
    
    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<PullRequest> tps = em.createQuery("from PullRequest", PullRequest.class).getResultList();
        
        for(PullRequest p : tps)
            em.remove(p);
        
        em.getTransaction().commit();
    }
    
    @Before
    public void setUp() {
        em = JPAUtil.getEntityManager();
    }
    
    @After
    public void tearDown() {
        em.close();
        em = null;
    }

    /**
     * Test of setMPC method, of class PullRequest.
     */
    @Test
    public void test01_SetMPC() {
        PullRequest instance = new PullRequest();
        
        instance.setMPC(T_MPC_1);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();        
    }

    /**
     * Test of getMPC method, of class PullRequest.
     */
    @Test
    public void test02_GetMPC() {
        em.getTransaction().begin();
        List<PullRequest> tps = em.createQuery("from PullRequest", PullRequest.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_MPC_1, tps.get(0).getMPC());
        
        em.getTransaction().commit();        
    }

    /**
     * Test of setAuthenticationInfo method, of class PullRequest. Includes test of
     * setAIObject
     */
    @Test
    public void test03_SetAuthenticationInfo() {
        em.getTransaction().begin();
        List<PullRequest> tps = em.createQuery("from PullRequest", PullRequest.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).setAuthenticationInfo(TEST_AUTH_INFO);
        
        em.getTransaction().commit();       
    }

    /**
     * Test of getAuthenticationInfo method, of class PullRequest. Includes test of
     * setAIObject
     */
    @Test
    public void test04_GetAuthenticationInfo() {
        em.getTransaction().begin();
        List<PullRequest> tps = em.createQuery("from PullRequest", PullRequest.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        IAuthenticationInfo ai = tps.get(0).getAuthenticationInfo();
        
        assertNotNull(ai);
        
        assertTrue(ai.equals(TEST_AUTH_INFO));
        
        em.getTransaction().commit();          
    }


}