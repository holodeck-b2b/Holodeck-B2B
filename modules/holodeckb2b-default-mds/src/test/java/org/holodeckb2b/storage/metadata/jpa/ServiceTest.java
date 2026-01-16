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

import jakarta.persistence.EntityManager;

import org.holodeckb2b.storage.metadata.jpa.wrappers.WService;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.Test;

/**
 * Test of {@link Service} JPA class. Because {@link Service} is an <i>"embeddable"</i> a wrapper class is used for
 * testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class ServiceTest {

    private static final String T_BASIC_SERVICE_NAME = "http://holodeck-b2b.org/development/testing/Service/1-0010";
    private static final String T_SPECIAL_SERVICE_NAME = "http://holodeck-b2b.org/development/testing/Śêřvįċę/2";
    private static final String T_TYPE = "org.holodeckb2b.types.services";

    @Test
    public void testNameAndType() {
        WService w = new WService();
        w.object().setName(T_BASIC_SERVICE_NAME);
        w.object().setType(T_TYPE);

        EntityManagerUtil.save(w);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        WService stored = em.find(WService.class, w.id());

        assertNotNull(stored);
        assertEquals(T_TYPE, stored.object().getType());
        assertEquals(T_BASIC_SERVICE_NAME, stored.object().getName());

        em.close();
    }

    @Test
    public void testNameWithSpecialCharacters() {
        WService w = new WService();
        w.object().setName(T_SPECIAL_SERVICE_NAME);

        EntityManagerUtil.save(w);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        WService stored = em.find(WService.class, w.id());

        assertNotNull(stored);
        assertNull(stored.object().getType());
        assertEquals(T_SPECIAL_SERVICE_NAME, stored.object().getName());
        assertNull(stored.object().getType());

        em.close();
    }

    @Test
    public void testConstructor() {
    	WService w = new WService();

    	w.setObject(new Service(T_SPECIAL_SERVICE_NAME, T_TYPE));

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WService stored = em.find(WService.class, w.id());

    	assertNotNull(stored);
    	assertEquals(T_TYPE, stored.object().getType());
    	assertEquals(T_SPECIAL_SERVICE_NAME, stored.object().getName());

    	em.close();
    }

    @Test
    public void testNameConstructor() {
    	WService w = new WService();

    	w.setObject(new Service(T_BASIC_SERVICE_NAME));

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WService stored = em.find(WService.class, w.id());

    	assertNotNull(stored);
    	assertNull(stored.object().getType());
    	assertEquals(T_BASIC_SERVICE_NAME, stored.object().getName());

    	em.close();
    }

    @Test
    public void testCopyConstructor() {
        WService w = new WService();

        w.setObject(new Service(new org.holodeckb2b.common.messagemodel.Service(T_BASIC_SERVICE_NAME, T_TYPE)));

        EntityManagerUtil.save(w);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        WService stored = em.find(WService.class, w.id());

        assertNotNull(stored);
        assertEquals(T_TYPE, stored.object().getType());
        assertEquals(T_BASIC_SERVICE_NAME, stored.object().getName());

        em.close();
    }

}
