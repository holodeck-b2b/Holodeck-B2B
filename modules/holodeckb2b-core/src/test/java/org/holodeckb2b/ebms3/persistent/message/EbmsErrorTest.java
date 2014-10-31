/*
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
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.ebms3.persistent.general.Description;
import org.holodeckb2b.ebms3.persistent.wrappers.EError;
import org.holodeckb2b.ebms3.util.JPAUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Tests if EError object can be stored correctly in database
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EbmsErrorTest {
    
    private static final String T_CATEGORY = "error-category-1";
    private static final String T_REF_TO_MSG = "ref-to-message-in-error";
    private static final String T_ERROR_CODE = "error-code";
    private static final IEbmsError.Severity T_SEVERITY = IEbmsError.Severity.FAILURE;
    private static final String T_MESSAGE = "error-short-description";
    private static final String T_ERROR_DETAIL = "error-detail";
    private static final String T_ORIGIN = "test-origin";
    
    private static final Description T_DESCRIPTION = new Description("Lorem ipsum dolor sit amet", "fake");
    
    EntityManager       em;

    public EbmsErrorTest() {
    }

    @AfterClass
    public static void cleanup() {
        EntityManager em = JPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        for(EError mu : tps)
            em.remove(mu);
        
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

    @Test
    public void test01_SetCategory() {
        EError instance = new EError();
       
        instance.eError.setCategory(T_CATEGORY);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test02_GetCategory() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_CATEGORY, tps.get(0).eError.getCategory());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }
    
    @Test
    public void test03_SetRefToMessageInError() {
        EError instance = new EError();
       
        instance.eError.setRefToMessageInError(T_REF_TO_MSG);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test04_GetRefToMessageInError() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_REF_TO_MSG, tps.get(0).eError.getRefToMessageInError());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }   

    @Test
    public void test05_SetErrorCode() {
        EError instance = new EError();
       
        instance.eError.setErrorCode(T_ERROR_CODE);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test06_GetErrorCode() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_ERROR_CODE, tps.get(0).eError.getErrorCode());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }   

    @Test
    public void test07_SetSeverity() {
        EError instance = new EError();
       
        instance.eError.setSeverity(T_SEVERITY);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test08_GetSeverity() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_SEVERITY, tps.get(0).eError.getSeverity());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }   

    @Test
    public void test09_SetMessage() {
        EError instance = new EError();
       
        instance.eError.setMessage(T_MESSAGE);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test10_GetMessage() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_MESSAGE, tps.get(0).eError.getMessage());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }   
    
    @Test
    public void test11_SetErrorDetail() {
        EError instance = new EError();
       
        instance.eError.setErrorDetail(T_ERROR_DETAIL);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test12_GetErrorDetail() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_ERROR_DETAIL, tps.get(0).eError.getErrorDetail());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }   
    
    @Test
    public void test13_SetOrigin() {
        EError instance = new EError();
       
        instance.eError.setOrigin(T_ORIGIN);
        
        em.getTransaction().begin();
        em.persist(instance);
        em.getTransaction().commit();
    }

    @Test
    public void test14_GetOrigin() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_ORIGIN, tps.get(0).eError.getOrigin());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }   

    @Test
    public void test15_SetDescription() {
        em.getTransaction().begin();
        EError instance = new EError();
       
        instance.eError.setDescription(T_DESCRIPTION);
        
        em.persist(instance);
        em.getTransaction().commit();
    }
    
    @Test
    public void test16_GetDescription() {
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_DESCRIPTION.getLanguage(), tps.get(0).eError.getDescription().getLanguage());
        assertEquals(T_DESCRIPTION.getText(), tps.get(0).eError.getDescription().getText());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();  
    }
    
}
