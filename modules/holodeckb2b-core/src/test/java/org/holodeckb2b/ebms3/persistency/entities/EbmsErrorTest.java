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

import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.persistency.entities.Description;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.persistent.wrappers.EError;
import org.holodeckb2b.ebms3.persistent.dao.TestJPAUtil;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
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
    private static final String T_LARGE_ERROR_DETAIL = "com.flame.shared.exceptions.PackingException: Message verification,\n" +
"                    authorisation, or decryption fail ed: com.sun.xml.wss.XWSSecurityException:\n" +
"                    javax.xml.crypto.MarshalException: java.security.NoSuchAlgorithmException: no\n" +
"                    such algorithm:\n" +
"                    http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-C ontent- be8\n" +
"                    Signature-Transform for provider XMLDSig at\n" +
"                    com.flame.connection.impl.ebXML.AS4.Session.run(Session.java:630) at\n" +
"                    java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:471) at\n" +
"                    java.util.concurrent.FutureTask.run(FutureTask.java:262) at\n" +
"                    java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)\n" +
"                    at\n" +
"                    java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)\n" +
"                    at java.lang.Thread.run(Thread.java:745) Caused by:\n" +
"                    com.sun.xml.wss.XWSSecurityException: com.sun.xml.wss.XWSSecurityException:\n" +
"                    javax.xml.crypto.MarshalException: java.security.NoSuchAlgorithmException: no\n" +
"                    such algorithm:\n" +
"                    http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content- be8\n" +
"                    Signature-Transform for provider XMLDSig at\n" +
"                    com.sun.xml.wss.impl.misc.XWSSProcessor2_0Impl.verifyInboundMessage(XWSSProcessor2_0Impl.java:158)\n" +
"                    at\n" +
"                    com.flame.packaging.wrapping.ebXML.AS4.PackageManager.verifyAndDecrypt(PackageManager.java:1419)\n" +
"                    at\n" +
"                    com.flame.packaging.wrapping.ebXML.AS4.PackageManager.securityPass(PackageManager.java:2272)\n" +
"                    at com.flame.connection.impl.ebXML.AS4.Session.run(Session.java:609) ... 5 more\n" +
"                    Caused by: com.sun.xml.wss.XWSSecurityException:\n" +
"                    javax.xml.crypto.MarshalException: java.security.NoSuchAlgorithmException: no\n" +
"                    such algorithm:\n" +
"                    http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content- be8\n" +
"                    Signature-Transform for provider XMLDSig at\n" +
"                    com.sun.xml.wss.impl.dsig.SignatureProcessor.verify(SignatureProcessor.java:916)\n" +
"                    at com.sun.xml.wss.impl.filter.SignatureFilter.process(SignatureFilter.java:638)\n" +
"                    at\n" +
"                    com.sun.xml.wss.impl.SecurityRecipient.pProcessOnce(SecurityRecipient.java:1199)\n" +
"                    at com.sun.xml.wss.impl.SecurityRecipient.pProcess(SecurityRecipient.java:1283)\n" +
"                    at\n" +
"                    com.sun.xml.wss.impl.SecurityRecipient.processMessagePolicy(SecurityRecipient.java:782)\n" +
"                    at\n" +
"                    com.sun.xml.wss.impl.SecurityRecipient.validateMessage(SecurityRecipient.java:261)\n" +
"                    at\n" +
"                    com.sun.xml.wss.impl.misc.XWSSProcessor2_0Impl.verifyInboundMessage(XWSSProcessor2_0Impl.java:156)\n" +
"                    ... 8 more Caused by: javax.xml.crypto.MarshalException:\n" +
"                    java.security.NoSuchAlgorithmException: no such algorithm:\n" +
"                    http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content- be8\n" +
"                    Signature-Transform for provider XMLDSig at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMTransform.&lt;init>(DOMTransform.java:80) at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMReference.&lt;init>(DOMReference.java:204) at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMSignedInfo.&lt;init>(DOMSignedInfo.java:177) at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMXMLSignature.&lt;init>(DOMXMLSignature.java:141)\n" +
"                    at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMXMLSignatureFactory.unmarshal(DOMXMLSignatureFactory.java:173)\n" +
"                    at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMXMLSignatureFactory.unmarshalXMLSignature(DOMXMLSignatureFactory.java:137)\n" +
"                    at\n" +
"                    com.sun.xml.wss.impl.dsig.SignatureProcessor.verify(SignatureProcessor.java:775)\n" +
"                    ... 14 more Caused by: java.security.NoSuchAlgorithmException: no such\n" +
"                    algorithm:\n" +
"                    http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content- be8\n" +
"                    Signature-Transform for provider XMLDSig at\n" +
"                    sun.security.jca.GetInstance.getService(GetInstance.java:100) at\n" +
"                    javax.xml.crypto.dsig.TransformService.getInstance(TransformService.java:210) at\n" +
"                    org.jcp.xml.dsig.internal.dom.DOMTransform.&lt;init>(DOMTransform.java:78) ...\n" +
"                    20 more";
    
    private static final String T_ORIGIN = "test-origin";
    
    private static final Description T_DESCRIPTION = new Description("Lorem ipsum dolor sit amet", "fake");
    private static final Description T_LARGE_DESCRIPTION = new Description("Message verification, authorisation, or decryption\n" +
"                    failed: com.sun.xml.wss.XWSSecurityException: javax.xml.crypto.MarshalException:\n" +
"                    java.security.NoSuchAlgorithmException: no such algorithm:\n" +
"                    http://docs.oasis-open.org/wss/oasis-wss-SwAProfil e-1.1#Attachment-Content- be8\n" +
"                    Signature-Transform for provider XMLDSig", "fake");
    
    EntityManager       em;

    public EbmsErrorTest() {
    }

    @AfterClass
    public static void cleanup() throws DatabaseException {
        EntityManager em = TestJPAUtil.getEntityManager();
        
        em.getTransaction().begin();
        Collection<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        for(EError mu : tps)
            em.remove(mu);
        
        em.getTransaction().commit();
    }       
    
    @Before
    public void setUp() throws DatabaseException {
        em = TestJPAUtil.getEntityManager();
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
       
        instance.eError.setShortDescription(T_MESSAGE);
        
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
    
    @Test
    public void test17_LargeDescription() {
        em.getTransaction().begin();
        EError instance = new EError();
       
        instance.eError.setDescription(T_LARGE_DESCRIPTION);
        
        em.persist(instance);        
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_LARGE_DESCRIPTION.getLanguage(), tps.get(0).eError.getDescription().getLanguage());
        assertEquals(T_LARGE_DESCRIPTION.getText(), tps.get(0).eError.getDescription().getText());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();         
    }
    
    @Test
    public void test18_LargeErrorDetail() {
        em.getTransaction().begin();
        EError instance = new EError();
       
        instance.eError.setErrorDetail(T_LARGE_ERROR_DETAIL);
        
        em.persist(instance);        
        em.getTransaction().commit();        
        
        em.getTransaction().begin();
        List<EError> tps = em.createQuery("from EError", EError.class).getResultList();
        
        assertTrue(tps.size() == 1);
        assertEquals(T_LARGE_ERROR_DETAIL, tps.get(0).eError.getErrorDetail());
        
        em.remove(tps.get(0));
        
        em.getTransaction().commit();         
    }
}
