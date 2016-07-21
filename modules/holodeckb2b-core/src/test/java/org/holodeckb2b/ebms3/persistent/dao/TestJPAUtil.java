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

/**
 * A very simple utility class for getting a JPA EntityManager
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class TestJPAUtil {

    private static EntityManagerFactory emf = null;

    public static EntityManager getEntityManager() {
        if (emf == null)
            emf = Persistence.createEntityManagerFactory("holodeckb2b-core-test");

        return emf.createEntityManager();
    }

    public static EntityManager getEntityManagerToAlpha() {
        if (emf == null)
            emf = Persistence.createEntityManagerFactory("holodeckb2b-alpha");

        return emf.createEntityManager();
    }
}
