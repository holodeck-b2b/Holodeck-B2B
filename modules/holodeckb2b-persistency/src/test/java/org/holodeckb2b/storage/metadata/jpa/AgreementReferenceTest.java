/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.persistence.EntityManager;

import org.holodeckb2b.storage.metadata.jpa.wrappers.WAgreementReference;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.Test;

/**
 * Test of {@link AgreementReference} JPA class. Because {@link AgreementReference} is an <i>"embeddable"</i> a wrapper
 * class is used for testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class AgreementReferenceTest {

    private static final String T_BASIC_AGREEMENT_NAME = "AgreementName-1-0010";
    private static final String T_SPECIAL_AGREEMENT_NAME = "A0010:z98s/gsta/t65w/ØĦ";
    private static final String T_TYPE = "org.holodeckb2b.types";
    private static final String T_PMODEID = "pm-test-agreement";

    @Test
    public void testNameAndType() {
        WAgreementReference w = new WAgreementReference();

        w.object().setName(T_BASIC_AGREEMENT_NAME);
        w.object().setType(T_TYPE);

        EntityManagerUtil.save(w);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        WAgreementReference stored = em.find(WAgreementReference.class, w.id());

        assertNotNull(stored);
        assertNull(stored.object().getPModeId());
        assertEquals(T_TYPE, stored.object().getType());
        assertEquals(T_BASIC_AGREEMENT_NAME, stored.object().getName());

        em.close();
    }

    @Test
    public void testNameWithSpecialCharacters() {
        WAgreementReference w = new WAgreementReference();

        w.object().setName(T_SPECIAL_AGREEMENT_NAME);

        EntityManagerUtil.save(w);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        WAgreementReference stored = em.find(WAgreementReference.class, w.id());

        assertNotNull(stored);
        assertEquals(T_SPECIAL_AGREEMENT_NAME, stored.object().getName());

        em.close();
    }

    @Test
    public void testPModeId() {
    	WAgreementReference w = new WAgreementReference();

    	w.object().setPModeId(T_PMODEID);

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WAgreementReference stored = em.find(WAgreementReference.class, w.id());

    	assertNotNull(stored);
    	assertEquals(T_PMODEID, stored.object().getPModeId());

    	em.close();
    }

    @Test
    public void testConstructor() {
    	WAgreementReference w = new WAgreementReference();

    	w.setObject(new AgreementReference(T_BASIC_AGREEMENT_NAME, T_TYPE, T_PMODEID));

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WAgreementReference stored = em.find(WAgreementReference.class, w.id());

    	assertNotNull(stored);
    	assertEquals(T_BASIC_AGREEMENT_NAME, stored.object().getName());
    	assertEquals(T_TYPE, stored.object().getType());
    	assertEquals(T_PMODEID, stored.object().getPModeId());

    	em.close();

    }

    @Test
    public void testCopyConstructor() {
    	WAgreementReference w = new WAgreementReference();

    	w.setObject(new AgreementReference(new org.holodeckb2b.common.messagemodel.AgreementReference(
    			T_BASIC_AGREEMENT_NAME, T_TYPE, T_PMODEID)));

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WAgreementReference stored = em.find(WAgreementReference.class, w.id());

    	assertNotNull(stored);
    	assertEquals(T_BASIC_AGREEMENT_NAME, stored.object().getName());
    	assertEquals(T_TYPE, stored.object().getType());
    	assertEquals(T_PMODEID, stored.object().getPModeId());

    	em.close();

    }
}
