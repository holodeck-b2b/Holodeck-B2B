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
package org.holodeckb2b.ebms3.persistent.message;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.common.messagemodel.IPayload.Containment;
import org.holodeckb2b.ebms3.persistent.general.Description;
import org.holodeckb2b.ebms3.persistent.general.Property;
import org.holodeckb2b.ebms3.persistent.general.SchemaReference;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PayloadTest {
    
    private static final String T_URI = "http://www.testsite.holodeckb2b.org/payloaduri/767676767-1111";
    private static final String T_LOCATION = "/opt/holodeckb2b/data/payload/2013/01";
    private static final String T_MIMETYPE = "text/xml";

    private static final String T_LOCATION_2 = "/opt/holodeckb2b/data/payload/2013/02";
    private static final String T_MIMETYPE_2 = "image/jpeg";

    private static final String T_URI_3 = "http://www.testsite.holodeckb2b.org/payloaduri/767676767-3333";
    private static final String T_LOCATION_3 = "/opt/holodeckb2b/data/payload/2013/03";
    private static final String T_MIMETYPE_3 = "application/octet-stream";

    private static final String T_PROP_1_NAME = "prop-1";
    private static final String T_PROP_1_VALUE = "val-1";
    private static final String T_PROP_2_NAME = "prop-2";
    private static final String T_PROP_2_VALUE = "val-2";
    private static final String T_PROP_2_TYPE = "type-2";
    private static final String T_PROP_3_NAME = "prop-3";
    private static final String T_PROP_3_VALUE = "val-3";
    
    private static final Description T_DESCRIPTION = new Description("Lorem ipsum dolor sit amet", "fake");
    private static final SchemaReference T_SCHEMAREF = new SchemaReference("http://www.testsite.holodeckb2b.org/schemaref","http://www.testsite.holodeckb2b.org/schemaref", "1.0");

    private static final Containment    T_CONTAINMENT = IPayload.Containment.ATTACHMENT;
    
    EntityManager       em;
    
    public PayloadTest() {
    }
    
    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        for(Payload o : tps)
            em.remove(o);
        
        em.getTransaction().commit();
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
     * Test of getPayloadURI method, of class Payload.
     */
    @Test
    public void test01_SetPayloadURI() {
        Payload instance = new Payload();
        
        instance.setPayloadURI(T_URI);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }
    
    /**
     * Test of getPayloadURI method, of class Payload.
     */
    @Test
    public void test02_GetPayloadURI() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_URI, tps.get(0).getPayloadURI());
        
        em.getTransaction().commit();        
    }

    
    /**
     * Test of setContentLocation method, of class Payload.
     */
    @Test
    public void test03_SetContentLocation() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        tps.get(0).setContentLocation(T_LOCATION);
        
        em.getTransaction().commit();  
    }
    
    /**
     * Test of getContentLocation method, of class Payload.
     */
    @Test
    public void test04_GetContentLocation() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_LOCATION, tps.get(0).getContentLocation());
        
        em.getTransaction().commit();         
    }

    /**
     * Test of setMimeType method, of class Payload.
     */
    @Test
    public void test05_SetMimeType() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        tps.get(0).setMimeType(T_MIMETYPE);
        
        em.getTransaction().commit();     
    }
    
    /**
     * Test of getMimeType method, of class Payload.
     */
    @Test
    public void test06_GetMimeType() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_MIMETYPE, tps.get(0).getMimeType());
        
        em.getTransaction().commit();     
    }

    /**
     * Test of setMessageProperties method, of class UserMessage.
     */
    @Test
    public void test07_SetProperties() {
        Collection<Property> props = new HashSet<Property>();
        
        props.add(new Property(T_PROP_1_NAME, T_PROP_1_VALUE));
        props.add(new Property(T_PROP_2_NAME, T_PROP_2_VALUE, T_PROP_2_TYPE));
        
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        Payload instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        instance.setProperties(props);
        
        em.getTransaction().commit();
    }
    
    @Test
    public void test07a_SetEmptyProps() {
        Payload instance = new Payload();
        
        instance.setProperties(null);
    }

    /**
     * Test of getMessageProperties method, of class UserMessage.
     */
    @Test
    public void test08_GetProperties() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        Collection<IProperty> properties = tps.get(0).getProperties();
        
        assertTrue(properties.size() == 2);
        
        for(IProperty p : properties) {
            if (p.getName().equals(T_PROP_1_NAME)) {
                assertEquals(T_PROP_1_VALUE, p.getValue());
                assertNull(p.getType());
            } else {
                assertEquals(T_PROP_2_NAME, p.getName());
                assertEquals(T_PROP_2_VALUE, p.getValue());
                assertEquals(T_PROP_2_TYPE, p.getType());
            }
        }
        
        em.getTransaction().commit();
    }

    /**
     * Test of addMessageProperty method, of class UserMessage.
     */
    @Test
    public void test09_AddProperty() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        Payload instance;
        
        assertTrue(tps.size() == 1);
        instance = tps.get(0);
        
        assertTrue(instance.getProperties().size() == 2);
        
        instance.addProperty(new Property(T_PROP_3_NAME, T_PROP_3_VALUE));
        
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        
        tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        Collection<IProperty> properties = tps.get(0).getProperties();
        
        assertTrue(properties.size() == 3);
        
        for(IProperty p : properties) {
            if (p.getName().equals(T_PROP_1_NAME)) {
                assertEquals(T_PROP_1_VALUE, p.getValue());
                assertNull(p.getType());
            } else if (p.getName().equals(T_PROP_2_NAME)) {
                assertEquals(T_PROP_2_VALUE, p.getValue());
                assertEquals(T_PROP_2_TYPE, p.getType());
            } else {
                assertEquals(T_PROP_3_NAME, p.getName());
                assertEquals(T_PROP_3_VALUE, p.getValue());
                assertNull(p.getType());
            }
        }
        
        em.getTransaction().commit();
    }

    
    /**
     * Test of setDescription method, of class Payload.
     */
    @Test
    public void test10_SetDescription() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        tps.get(0).setDescription(T_DESCRIPTION);
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of getDescription method, of class Payload.
     */
    @Test
    public void test11_GetDescription() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_DESCRIPTION.getLanguage(), tps.get(0).getDescription().getLanguage());
        assertEquals(T_DESCRIPTION.getText(), tps.get(0).getDescription().getText());
        
        
        em.getTransaction().commit();  
    }

    /**
     * Test of setSchemaReference method, of class Payload.
     */
    @Test
    public void test12_SetSchemaReference() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        tps.get(0).setSchemaReference(T_SCHEMAREF);
        
        em.getTransaction().commit();
    }

    /**
     * Test of getSchemaReference method, of class Payload.
     */
    @Test
    public void test13_GetSchemaReference() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_SCHEMAREF.getLocation(), tps.get(0).getSchemaReference().getLocation());
        assertEquals(T_SCHEMAREF.getNamespace(), tps.get(0).getSchemaReference().getNamespace());
        assertEquals(T_SCHEMAREF.getVersion(), tps.get(0).getSchemaReference().getVersion());
        
        
        em.getTransaction().commit();  
    }

    /**
     * Test of setContainment method, of class Payload.
     */
    @Test
    public void test14_SetContainment() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        tps.get(0).setContainment(T_CONTAINMENT);
        
        em.getTransaction().commit();  
    }
    
    /**
     * Test of getContainment method, of class Payload.
     */
    @Test
    public void test15_GetContainment() {
        em.getTransaction().begin();
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_CONTAINMENT, tps.get(0).getContainment());
        
        em.getTransaction().commit();         
    }
    
    /**
     * Test of non default constructor to create simple Payload object without URI
     */
    @Test
    public void test16_EssentialConstructor() {
        em.getTransaction().begin();
        
        Payload p2 = new Payload(T_LOCATION_2, T_MIMETYPE_2);
        
        em.persist(p2);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 2);
        assertEquals(T_LOCATION_2, tps.get(1).getContentLocation());
        assertEquals(T_MIMETYPE_2, tps.get(1).getMimeType());
        
        em.getTransaction().commit();
    }
    
    /**
     * Test of non default constructor to create simple Payload object with URI
     */
    @Test
    public void test17_URIConstructor() {
        em.getTransaction().begin();
        
        Payload p3 = new Payload(T_LOCATION_3, T_MIMETYPE_3, T_URI_3);
        
        em.persist(p3);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        
        List<Payload> tps = em.createQuery("from Payload", Payload.class).getResultList();
        
        assertTrue(tps.size() == 3);
        assertEquals(T_LOCATION_3, tps.get(2).getContentLocation());
        assertEquals(T_MIMETYPE_3, tps.get(2).getMimeType());
        assertEquals(T_URI_3, tps.get(2).getPayloadURI());
        
        em.getTransaction().commit();
    }    
}