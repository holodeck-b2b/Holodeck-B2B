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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;

import org.holodeckb2b.ebms3.persistent.dao.TestJPAUtil;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for class ErrorMessage
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ErrorMessageTest {

    private static EbmsError   T_ERROR_1;
    private static EbmsError   T_ERROR_2;
    private static EbmsError   T_ERROR_3;
    private static EbmsError   T_ERROR_4;

    EntityManager       em;

    public ErrorMessageTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        // Create some errors
        T_ERROR_1 = new EbmsError();
        T_ERROR_1.setCategory("error-category-1");
        T_ERROR_1.setErrorCode("error-code-1");
        T_ERROR_1.setDescription(new Description("Lorem ipsum dolor sit amet", "lang-1"));

        T_ERROR_2 = new EbmsError();
        T_ERROR_2.setCategory("error-category-2");
        T_ERROR_2.setErrorCode("error-code-2");
        T_ERROR_2.setRefToMessageInError("error-2");
        T_ERROR_2.setDescription(new Description("Lorem ipsum dolor sit amet", "lang-2"));

        T_ERROR_3 = new EbmsError();
        T_ERROR_3.setCategory("error-category-3");
        T_ERROR_3.setErrorCode("error-code-3");
        T_ERROR_3.setRefToMessageInError("error-3");
        T_ERROR_3.setDescription(new Description("Lorem ipsum dolor sit amet", "lang-3"));

        T_ERROR_4 = new EbmsError();
        T_ERROR_4.setCategory("error-category-4");
        T_ERROR_4.setErrorCode("error-code-4");
        T_ERROR_4.setRefToMessageInError("error-4");
        T_ERROR_4.setDescription(new Description("Lorem ipsum dolor sit amet", "lang-4"));
    }

    @AfterClass
    public static void cleanup() {
        final EntityManager em = TestJPAUtil.getEntityManager();

        em.getTransaction().begin();
        final Collection<ErrorMessage> tps = em.createQuery("from ErrorMessage", ErrorMessage.class).getResultList();

        for(final ErrorMessage o : tps)
            em.remove(o);

        em.getTransaction().commit();
    }

    @Before
    public void setUp() {
        em = TestJPAUtil.getEntityManager();
    }

    @After
    public void tearDown() {
        em.close();
    }

    @Test
    public void test01_SetErrors() {
        final ErrorMessage instance = new ErrorMessage();

        final HashSet<EbmsError>  errorList = new HashSet<>();
        errorList.add(T_ERROR_1);
        errorList.add(T_ERROR_2);
        errorList.add(T_ERROR_3);

        instance.setErrors(errorList);
        instance.setRefToMessageId("signal-reference");

        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test02_GetErrors() {

        final List<ErrorMessage> tps = em.createQuery("from ErrorMessage", ErrorMessage.class).getResultList();

        assertTrue(tps.size() == 1);

        final Collection<IEbmsError> errors = tps.get(0).getErrors();

        assertTrue(errors.size() == 3);

        for(final IEbmsError e : errors) {
            final String cat = e.getCategory();
            if (T_ERROR_1.getCategory().equals(cat)) {
                assertEquals(T_ERROR_1.getErrorCode(), e.getErrorCode());
                assertEquals(T_ERROR_1.getDescription().getLanguage(), e.getDescription().getLanguage());
            } else if (T_ERROR_2.getCategory().equals(cat)) {
                assertEquals(T_ERROR_2.getErrorCode(), e.getErrorCode());
                assertEquals(T_ERROR_2.getDescription().getLanguage(), e.getDescription().getLanguage());
            } else if (T_ERROR_3.getCategory().equals(cat)) {
                assertEquals(T_ERROR_3.getErrorCode(), e.getErrorCode());
                assertEquals(T_ERROR_3.getDescription().getLanguage(), e.getDescription().getLanguage());
            }
        }
    }

    @Test
    public void test03_AddError() {
        em.getTransaction().begin();

        List<ErrorMessage> tps = em.createQuery("from ErrorMessage", ErrorMessage.class).getResultList();
        assertTrue(tps.size() == 1);
        Collection<IEbmsError> errors = tps.get(0).getErrors();
        assertTrue(errors.size() == 3);

        tps.get(0).addError(T_ERROR_4);

        em.getTransaction().commit();

        em.close();

        tps = null;
        errors = null;

        em = TestJPAUtil.getEntityManager();

        tps = em.createQuery("from ErrorMessage", ErrorMessage.class).getResultList();
        assertTrue(tps.size() == 1);
        errors = tps.get(0).getErrors();
        assertTrue(errors.size() == 4);

        for(final IEbmsError e : errors) {
            final String cat = e.getCategory();
            if (T_ERROR_4.getCategory().equals(cat)) {
                assertEquals(T_ERROR_4.getErrorCode(), e.getErrorCode());
                assertEquals(T_ERROR_4.getDescription().getLanguage(), e.getDescription().getLanguage());
            }
        }
    }

    @Test
    public void test04_findResponseQuery() {
        em.getTransaction().begin();

        final HashSet<EbmsError>  errorList = new HashSet<>();
        errorList.add(T_ERROR_4);

        final ErrorMessage instance = new ErrorMessage();
        instance.setMessageId("ref-result");
        instance.setErrors(errorList);

        em.persist(instance);
        em.getTransaction().commit();

        Collection<ErrorMessage> result = em.createNamedQuery("ErrorMessage.findResponsesTo",
                ErrorMessage.class)
                .setParameter("refToMsgId", "error-4")
                .getResultList();

        assertEquals(1, result.size());
        assertEquals("ref-result", result.iterator().next().getMessageId());

        result = em.createNamedQuery("ErrorMessage.findResponsesTo",
                ErrorMessage.class)
                .setParameter("refToMsgId", "error-2")
                .getResultList();

        assertEquals(0, result.size());

        result = em.createNamedQuery("ErrorMessage.findResponsesTo",
                ErrorMessage.class)
                .setParameter("refToMsgId", "signal-reference")
                .getResultList();

        assertEquals(1, result.size());
    }

}
