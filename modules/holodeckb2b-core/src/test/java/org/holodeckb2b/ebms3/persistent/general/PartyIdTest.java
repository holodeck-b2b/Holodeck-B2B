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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;

import org.holodeckb2b.ebms3.persistency.entities.PartyId;
import org.holodeckb2b.ebms3.persistent.dao.TestJPAUtil;
import org.holodeckb2b.ebms3.persistent.wrappers.EPartyId;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PartyIdTest {

    private static final String T_PARTYID_1 = "urn:test:party-id:testid:0";
    private static final String T_PARTYID_2 = "second-partyid";
    private static final String T_PARTYID_3 = "non standard partyid";
    private static final String T_PARTYID_4 = "copied partyid";

    private static final String T_PARTY_TP_1 = "partyid-type:id:scheme:1";

    private static final String T_PARTY_TP_3 = "partyid-type:id:scheme:2";
    private static final String T_PARTY_TP_4 = "partyid-type:id:scheme:3";


    EntityManager   em;

    public PartyIdTest() {
    }

    @Before
    public void setUp() {
        em = TestJPAUtil.getEntityManager();
    }

    @After
    public void tearDown() {
        em.close();
    }

    /**
     * Test of setId method, of class PartyId.
     */
    @Test
    public void test1_SetId() {
        System.out.println("setId");
        final EPartyId instance = new EPartyId();
        instance.ePartyId.setId(T_PARTYID_1);

        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getId method, of class PartyId.
     */
    @Test
    public void test2_GetId() {
        System.out.println("getId");

        em.getTransaction().begin();
        final List<EPartyId> tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);
        assertEquals(T_PARTYID_1, tps.get(0).ePartyId.getId());

        em.getTransaction().commit();
    }

    /**
     * Test of setType method, of class PartyId.
     */
    @Test
    public void test3_SetType() {
        System.out.println("setType");

        em.getTransaction().begin();
        final List<EPartyId> tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);

        tps.get(0).ePartyId.setType(T_PARTY_TP_1);

        em.persist(tps.get(0));

        em.getTransaction().commit();
    }

    /**
     * Test of getType method, of class PartyId.
     */
    @Test
    public void test4_GetType() {
        System.out.println("getType");

        em.getTransaction().begin();
        final List<EPartyId> tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);
        assertEquals(T_PARTY_TP_1, tps.get(0).ePartyId.getType());

        em.getTransaction().commit();
    }

    /**
     * Test of non default constructor with only id
     */
    @Test
    public void test5_IdConstructor() {
        System.out.println("IdConstructor");
        em.getTransaction().begin();
        List<EPartyId> tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);

        tps.get(0).ePartyId = new PartyId(T_PARTYID_2);

        em.persist(tps.get(0));
        em.getTransaction().commit();

        em.getTransaction().begin();

        tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);
        assertEquals(T_PARTYID_2, tps.get(0).ePartyId.getId());
        assertNull(tps.get(0).ePartyId.getType());

        em.getTransaction().commit();
    }

    /**
     * Test of non default constructor with both id and type set
     */
    @Test
    public void test6_PartyIdConstructor() {
        System.out.println("PartyIdConstructor");
        em.getTransaction().begin();
        List<EPartyId> tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);

        tps.get(0).ePartyId = new PartyId(T_PARTYID_3, T_PARTY_TP_3);

        em.persist(tps.get(0));
        em.getTransaction().commit();

        em.getTransaction().begin();

        tps = em.createQuery("from EPartyId", EPartyId.class).getResultList();

        assertTrue(tps.size() == 1);
        assertEquals(T_PARTYID_3, tps.get(0).ePartyId.getId());
        assertEquals(T_PARTY_TP_3, tps.get(0).ePartyId.getType());

        em.getTransaction().commit();
    }

}