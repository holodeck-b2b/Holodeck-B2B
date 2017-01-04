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


import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for the Receipt persistency object
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReceiptTest {

    EntityManager   em;

    private static final String T_CONTENT_1 =   "<content>" +
                                                "<confirmation>\n" +
                                                "    <from>Party_X</from>\n" +
                                                "    <message>Success</message>\n" +
                                                "</confirmation>\n" +
                                                "</content>";
    private static final String T_CONTENT_2 =   "<content xmlns:e=\"http://sample.ns/test\">" +
                                                "<confirmation>\n" +
                                                "    <e:from>Party_X</e:from>\n" +
                                                "    <message>Success</message>\n" +
                                                "</confirmation>" +
                                                "<confirmation>\n" +
                                                "    <e:from>Party_X</e:from>\n" +
                                                "    <message>Success</message>\n" +
                                                "</confirmation>\n" +
                                                "</content>";


    public ReceiptTest() {
    }

    @BeforeClass
    public static void setupClass() {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(null));
    }

    @AfterClass
    public static void cleanup() throws DatabaseException {
        final EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();
        final Collection<Receipt> tps = em.createQuery("from Receipt", Receipt.class).getResultList();

        for(final Receipt r : tps)
            em.remove(r);

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
     * Test of setContent method with just 1 XML element as content.
     */
    @Test
    public void test01_SetContent() {
        final Receipt instance = new Receipt();

        final OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(T_CONTENT_1));
        // Parse document and get root element
        final OMElement contentElement = builder.getDocumentElement();

        instance.setContent(contentElement.getChildElements());

        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getContent method. With just 1 XML element
     */
    @Test
    public void test02_GetContent() {
        em.getTransaction().begin();
        final List<Receipt> tps = em.createQuery("from Receipt", Receipt.class).getResultList();

        assertTrue(tps.size() == 1);

        final ArrayList<OMElement> content = tps.get(0).getContent();

        assertTrue(content.size() == 1);
        assertEquals(content.get(0).getLocalName(), "confirmation");
        assertNull(content.get(0).getNamespaceURI());

        em.remove(tps.get(0));
        em.getTransaction().commit();
    }

    /**
     * Test of setContent method with 2 XML elements and namespace as content.
     */
    @Test
    public void test03_SetContent() {
        final Receipt instance = new Receipt();

        final OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(T_CONTENT_2));
        // Parse document and get root element
        final OMElement contentElement = builder.getDocumentElement();

        instance.setContent(contentElement.getChildElements());

        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    /**
     * Test of getContent method. With just 1 XML element
     */
    @Test
    public void test04_GetContent() {
        em.getTransaction().begin();
        final List<Receipt> tps = em.createQuery("from Receipt", Receipt.class).getResultList();

        assertTrue(tps.size() == 1);

        final ArrayList<OMElement> content = tps.get(0).getContent();

        assertTrue(content.size() == 2);
        assertEquals(content.get(0).getLocalName(), "confirmation");
        assertNull(content.get(0).getNamespaceURI());

        assertEquals(content.get(0).getFirstElement().getLocalName(), "from");
        assertEquals(content.get(0).getFirstElement().getNamespaceURI(), "http://sample.ns/test");

        em.getTransaction().commit();
    }


}