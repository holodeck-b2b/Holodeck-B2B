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
package org.holodeckb2b.ebms3.persistent.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.module.HolodeckB2BCoreImpl;


/**
 * Helper class to easily access the database.
 * <p>This class is intended for use by the Holodeck B2B core only. DO NOT USE in
 * custom code!
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class JPAUtil {
    
    /*
     * The EntityManagerFactory to create EntityManagers
     */
    private static EntityManagerFactory emf;
    
    /**
     * Gets a {@see EntityManager} for the JPA persistency unit named in the configuration.
     * 
     * @return  An <code>EntityManager</code> to access the database
     * @throws  DatabaseException   
     */
    public static EntityManager getEntityManager() throws DatabaseException {
       // Initialize list of EMF's if not already done
       if (emf == null)
           emf = getEntityManagerFactory(); 
      
       // Create an EntityManager using the found factory
       EntityManager em = null;
       
       try {
           em = emf.createEntityManager();
       } catch (Exception e) {
           // Oh oh, something went wrong creating the entity manager
           throw new DatabaseException("Error while creating the EntityManager", e);
       }
       
       return em;
    }
    
    private static synchronized EntityManagerFactory getEntityManagerFactory() throws DatabaseException {        
        if (emf != null)
            return emf;
        else
            return Persistence.createEntityManagerFactory(HolodeckB2BCoreImpl.getPersistencyUnit());
    }
}
