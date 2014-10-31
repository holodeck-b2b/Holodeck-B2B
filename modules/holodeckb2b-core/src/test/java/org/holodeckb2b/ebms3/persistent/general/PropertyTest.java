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
package org.holodeckb2b.ebms3.persistent.general;

import org.holodeckb2b.ebms3.persistent.general.Property;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.persistent.wrappers.EProperty;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PropertyTest {
    
    private static final String TEST_NAME_1 = "prop-1";
    private static final String TEST_VALUE_1 = "value-1";
    private static final String TEST_TYPE_1 = "type-1";
    
    private static final String TEST_NAME_2 = "prop-2";
    private static final String TEST_VALUE_2 = "value-2";

    private static final String TEST_NAME_3 = "prop-3";
    private static final String TEST_VALUE_3 = "value-3";
    private static final String TEST_TYPE_3 = "type-3";
    
    
    EntityManager   em;
    
    public PropertyTest() {
    }
    
    @Before
    public void setUp() {
        em = JPAUtil.getEntityManager();
    }
    
    @After
    public void tearDown() {
        em.close();
    }

    /**
     * Test of setName method, of class Property.
     */
    @Test
    public void test1_SetName() {
        EProperty instance = new EProperty();
        
        instance.eProperty.setName(TEST_NAME_1);
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getName method, of class Property.
     */
    @Test
    public void test2_GetName() {
        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(TEST_NAME_1, tps.get(0).eProperty.getName());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setValue method, of class Property.
     */
    @Test
    public void test3_SetValue() {
        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eProperty.setValue(TEST_VALUE_1);
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of getValue method, of class Property.
     */
    @Test
    public void test4_GetValue() {
        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(TEST_VALUE_1, tps.get(0).eProperty.getValue());
        
        em.getTransaction().commit();
    }

    /**
     * Test of setType method, of class Property.
     */
    @Test
    public void test5_SetType() {
        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        System.out.println("Property test, setType, size= " + tps.size());
        
        assertTrue(tps.size() == 1);
        
        tps.get(0).eProperty.setType(TEST_TYPE_1);
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of getType method, of class Property.
     */
    @Test
    public void test6_GetType() {
        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(TEST_TYPE_1, tps.get(0).eProperty.getType());
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of constructor with name value pair
     */
    @Test
    public void test7_NVPairConstructor() {
        EProperty instance = new EProperty();
        
        instance.eProperty = new Property(TEST_NAME_2, TEST_VALUE_2);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();

        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        assertTrue(tps.size() == 2);
        assertEquals(TEST_NAME_2, tps.get(1).eProperty.getName());
        assertEquals(TEST_VALUE_2, tps.get(1).eProperty.getValue());
        
        em.getTransaction().commit();
        
    }

    /**
     * Test of constructor with name value pair and type
     */
    @Test
    public void test8_CompleteConstructor() {
        EProperty instance = new EProperty();
        
        instance.eProperty = new Property(TEST_NAME_3, TEST_VALUE_3, TEST_TYPE_3);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();

        em.getTransaction().begin();
        List<EProperty> tps = em.createQuery("from EProperty", EProperty.class).getResultList();
        
        assertTrue(tps.size() == 3);
        assertEquals(TEST_NAME_3, tps.get(2).eProperty.getName());
        assertEquals(TEST_VALUE_3, tps.get(2).eProperty.getValue());
        assertEquals(TEST_TYPE_3, tps.get(2).eProperty.getType());
        
        em.getTransaction().commit();
    }
    
}