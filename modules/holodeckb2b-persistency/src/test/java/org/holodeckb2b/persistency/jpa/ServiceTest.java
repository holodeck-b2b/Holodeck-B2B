/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.jpa;

import javax.persistence.EntityManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.persistency.test.wrappers.WService;
import org.holodeckb2b.persistency.util.EntityManagerUtil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link Service} JPA class. Because {@link Service} is an <i>"embeddable"</i> a wrapper class is used for
 * testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 */
public class ServiceTest {

    private static final String T_BASIC_SERVICE_NAME = "http://holodeck-b2b.org/development/testing/Service/1-0010";
    private static final String T_SPECIAL_SERVICE_NAME = "http://holodeck-b2b.org/development/testing/Śêřvįċę/2";

    public ServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws PersistenceException {
        EntityManager em = null;
        try {
            em = EntityManagerUtil.getEntityManager();
            em.getTransaction().begin();
            em.createQuery("DELETE FROM WService").executeUpdate();
            em.getTransaction().commit();
        } finally {
            if (em != null) em.close();
        }

    }

    @Test
    public void testNameOnly() throws PersistenceException {
        EntityManager em = EntityManagerUtil.getEntityManager();
        WService testObject = new WService();

        testObject.e.setName(T_BASIC_SERVICE_NAME);

        em.getTransaction().begin();
        em.persist(testObject);
        em.getTransaction().commit();
        em.close();

        // Retrieve the object again and check value
        em = EntityManagerUtil.getEntityManager();
        WService storedObject = em.find(WService.class, testObject.id);

        assertNotNull(storedObject);
        assertEquals(T_BASIC_SERVICE_NAME, storedObject.e.getName());
        assertNull(storedObject.e.getType());

        em.close();
    }

    @Test
    public void testNameWithSpecialCharacters() throws PersistenceException {
        EntityManager em = EntityManagerUtil.getEntityManager();
        WService testObject = new WService();

        testObject.e.setName(T_SPECIAL_SERVICE_NAME);

        em.getTransaction().begin();
        em.persist(testObject);
        em.getTransaction().commit();
        em.close();

        // Retrieve the object again and check value
        em = EntityManagerUtil.getEntityManager();
        WService storedObject = em.find(WService.class, testObject.id);

        assertNotNull(storedObject);
        assertEquals(T_SPECIAL_SERVICE_NAME, storedObject.e.getName());
        assertNull(storedObject.e.getType());

        em.close();
    }

}
