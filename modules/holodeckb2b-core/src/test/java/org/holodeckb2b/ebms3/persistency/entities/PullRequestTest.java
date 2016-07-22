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


import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
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

    EntityManager   em;


    private static final String T_MPC_1 = "http://holodeck-b2b.org/test/pull/mpc1";


    public PullRequestTest() {
    }

    @BeforeClass
    public static void setupClass() {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(null));
    }

    @AfterClass
    public static void cleanup() throws DatabaseException {
        final EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();
        final Collection<PullRequest> tps = em.createQuery("from PullRequest", PullRequest.class).getResultList();

        for(final PullRequest p : tps)
            em.remove(p);

        em.getTransaction().commit();
    }

    @Before
    public void setUp() throws DatabaseException {
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
        final PullRequest instance = new PullRequest();

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
        final List<PullRequest> tps = em.createQuery("from PullRequest", PullRequest.class).getResultList();

        assertTrue(tps.size() == 1);
        assertEquals(T_MPC_1, tps.get(0).getMPC());

        em.getTransaction().commit();
    }


}