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
package org.holodeckb2b.storage.metadata.testhelpers;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.holodeckb2b.interfaces.storage.providers.StorageException;


/**
 * Is a helper class to easily get hold of the JPA <code>EntityManager</code> to access the database where the message
 * unit meta-data is stored.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class EntityManagerUtil {
    private static EntityManagerFactory instance;

    /**
     * Creates a new <code>EntityManagerFactory</code> for the test database.
     *
     * @return	the new <code>EntityManagerFactory</code>
     */
    public static EntityManagerFactory createEntityManagerFactory() {
    	instance =  Persistence.createEntityManagerFactory("holodeckb2b-test");
		return instance;
    }

    /**
     * Saves the given entity object to test database.
     *
     * @param e	 the entity object to save, the object must have the <code>@Entity</code> annotation
     * @throws StorageException when saving the object fails
     */
    public static void save(Object e) {
    	EntityManager em = null;
    	try {
	    	em = getEntityManager();
	        em.getTransaction().begin();
	        em.persist(e);
	        em.getTransaction().commit();
	        em.close();
    	} catch (Throwable t) {
    		throw new RuntimeException("Could not save object", t);
    	} finally {
			if (em != null && em.isOpen())
				em.close();
		}
    }

    /**
     * @return the EntityManagerFactory for the test database
     */
    static EntityManagerFactory getEntityManagerFactory() {
    	if (instance == null)
    		instance = createEntityManagerFactory();
    	return instance;
    }

    /**
     * Gets a JPA {@link EntityManager} to execute database operations.
     *
     * @return  An <code>EntityManager</code> to access the database
     * @throws  StorageException   When exception occurs getting hold of an EntityManager object
     */
    public static EntityManager getEntityManager() {
       try {
           // The class is loaded upon first call
           return getEntityManagerFactory().createEntityManager();
       } catch (final Exception e) {
           // Oh oh, something went wrong creating the entity manager
           throw new RuntimeException("Error while creating the EntityManager", e);
       }
    }


}
