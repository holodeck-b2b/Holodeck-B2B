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
package org.holodeckb2b.ebms3.submit.mmd;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.persistent.message.Payload;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageMetaDataTest {
    
    private static final String T_UM1_MPC = "http://holodeck-b2b/test";
    private static final String T_UM1_TIMESTAMP = "2013-07-15T00:00:00.000+02:00";
    private static final String T_UM1_MESSAGEID = "holodeckb2b-mmd-t1-msg1@local.test";
    private static final String T_UM1_REFTOMSGID = "holodeckb2b-mmd-t1-msg2@local.test";
    private static final String T_UM1_SENDER_PID = "urn:org:holodeckb2b:test:sender1";
    private static final String T_UM1_SENDER_ROLE = "TestSender";
    private static final String T_UM1_RECEIVER_PID = "recvr-2";
    private static final String T_UM1_RECEIVER_PID_TYPE = "holodeckb2b-test";
    private static final String T_UM1_RECEIVER_ROLE = "TestReceiver";
    private static final String T_UM1_PMODEID = "um1-pmode-1";
    private static final String T_UM1_SERVICE = "urn:org:holodeckb2b:test:service1";
    private static final String T_UM1_ACTION = "Test";
    private static final String T_UM1_CONVID = "conv-1";
    private static final String T_UM1_MSGPROP1_NAME = "msgprop-1";
    private static final String T_UM1_MSGPROP1_VALUE = "msgvalue-1";
    private static final String T_UM1_MSGPROP2_NAME = "msgprop-2";
    private static final String T_UM1_MSGPROP2_VALUE = "msgvalue-2";
    private static final String T_UM1_MSGPROP1_TYPE = "proptype";
    private static final IPayload.Containment T_UM1_PAYLD1_CONTAINMENT = IPayload.Containment.BODY;
    private static final String T_UM1_PAYLD1_LOC = "/files/out/testsample.xml";
    private static final String T_UM1_PAYLD1_PROP_NAME = "payload-prop-1";
    private static final String T_UM1_PAYLD1_PROP_VALUE = "payload-value-1";
    private static final String T_UM1_PAYLD1_SCHEMA_LOC = "http://holodeck-b2b/loc";
    private static final String T_UM1_PAYLD1_SCHEMA_NS = "http://holodeck-b2b/ns";
    private static final String T_UM1_PAYLD1_SCHEMA_VER = "2.0";
    private static final IPayload.Containment T_UM1_PAYLD2_CONTAINMENT = IPayload.Containment.EXTERNAL;
    private static final String T_UM1_PAYLD2_LOC = "/files/out/testsample2.xml";
    
    
    
    
    
    
    public MessageMetaDataTest() {
    }
    
    @Before
    public void setUp() {

    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of createFromFile method with minimal MMD content. Note that this MMD file can not
     * be used for submitting message because it contains too few information to determine the PMode
     * to use.
     */
    @Test
    public void test_Minimal() throws Exception {
        String path = this.getClass().getClassLoader().getResource("mmdtest/minimal.xml").getPath();
        File   f = new File(path);
        
        try {
            MessageMetaData mmd = MessageMetaData.createFromFile(f);
            
            assertNotNull(mmd);
            assertNotNull(mmd.getCollaborationInfo());
            assertEquals("q3KuGFmr", mmd.getCollaborationInfo().getConversationId());
            
            assertNull(mmd.getPayloads());
            
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    /**
     * Test of createFromFile method with full MMD content.
     */
    @Test
    public void test_CreateFromFile() throws Exception {
        String path = this.getClass().getClassLoader().getResource("mmdtest/mmdtest2.xml").getPath();
        File   f = new File(path);
        
        try {
            MessageMetaData mmd = MessageMetaData.createFromFile(f);
            
            assertNotNull(mmd);
            assertEquals(org.holodeckb2b.common.util.Utils.fromXMLDateTime("2014-08-14T15:50:00Z"), mmd.getTimestamp());
            assertEquals("H8MQXJ", mmd.getMessageId());
            assertEquals("UGA08vL5hsdNfC-sGoV2RD", mmd.getRefToMessageId());
            
            assertNotNull(mmd.getSender());
            assertEquals("igI0jjZy0QVYc44Zns", mmd.getSender().getRole());
            assertEquals(1, mmd.getSender().getPartyIds().size());
            
            IPartyId pid = mmd.getSender().getPartyIds().iterator().next();
            assertEquals("I16WRl8Mo_5", pid.getId());
            assertEquals("CC", pid.getType());
            
            assertNotNull(mmd.getReceiver());
            assertEquals("T4omv4", mmd.getReceiver().getRole());
            assertEquals(2, mmd.getReceiver().getPartyIds().size());
            
            Iterator<IPartyId> it = mmd.getReceiver().getPartyIds().iterator();
            pid = it.next();
            assertEquals("_UP0ICvTAvWOkY", pid.getId());
            assertEquals("VNHvbKxy36LcdCsRJ-d2smcYaS0y", pid.getType());
            pid = it.next();
            assertEquals("bwQ1SfXHesS", pid.getId());
            assertEquals("zt9y8HHLVXpF", pid.getType());
            
            assertNotNull(mmd.getCollaborationInfo());
            assertNotNull(mmd.getCollaborationInfo().getAgreement());
            assertEquals("vDhYgevSzmZsm",mmd.getCollaborationInfo().getAgreement().getName());
            assertEquals("B4Q1nI",mmd.getCollaborationInfo().getAgreement().getType());
            assertEquals("LQPFs8kqibFl2PcRR68VOyvlnDqno0",mmd.getCollaborationInfo().getAgreement().getPModeId());
                    
            assertNotNull(mmd.getCollaborationInfo().getService());
            assertEquals("wa7Ef3UUgJ4tvq3MJrV-utk", mmd.getCollaborationInfo().getService().getType());
            assertEquals("UDbjJ8cZ-yjv_d0CirtQQPM", mmd.getCollaborationInfo().getService().getName());
            
            assertEquals("bjEgOvXs", mmd.getCollaborationInfo().getAction());
            assertEquals("12XbXGUw", mmd.getCollaborationInfo().getConversationId());
            
            assertNotNull(mmd.getMessageProperties());
            assertEquals(2, mmd.getMessageProperties().size());
            
            Iterator<IProperty> props = mmd.getMessageProperties().iterator();
            IProperty p = props.next();
            assertEquals("ldchmmpEV36tF89ljfaz44egY9O", p.getName());
            assertEquals("jlcBKSKrudaXcqkbNVPzTcIFo.4Dr", p.getType());
            assertEquals("_o54KWF_pPsp7QMc5", p.getValue());
            
            p = props.next();
            assertEquals("g6IBh.1Q9FO", p.getName());
            assertNull(p.getType());
            assertEquals("Ifn.XlWFNuxK7E8Jh8JNNwJ", p.getValue());
            
            assertNotNull(mmd.getPayloads());
            assertEquals(2, mmd.getPayloads().size());
            Iterator<IPayload> pls = mmd.getPayloads().iterator();
            IPayload pl = pls.next();
            
            assertEquals(IPayload.Containment.ATTACHMENT, pl.getContainment());
            assertEquals("http://MnjwHeFz/", pl.getPayloadURI());
            assertEquals("KCOxwbyEnmRtpniIokx1espl", pl.getMimeType());
            assertEquals("http://uOnVVjda/", pl.getContentLocation());
            assertNotNull(pl.getSchemaReference());
            assertEquals("http://BDneqLNN/", pl.getSchemaReference().getLocation());
            assertEquals("2.0", pl.getSchemaReference().getVersion());
            assertEquals("http://hkjahiuwuh.cgfdff/uuudh/111.22/jhh", pl.getSchemaReference().getNamespace());
            
            assertNotNull(pl.getDescription());
            assertEquals("zh-CHS", pl.getDescription().getLanguage());
            assertEquals("Q4Q89UfwVUx\n" +
                        "            \n" +
                        "            djhkjfhkjh", pl.getDescription().getText());
            
            assertNotNull(pl.getProperties());
            assertEquals(2, pl.getProperties().size());
            props = pl.getProperties().iterator();
            p = props.next();
            assertEquals("X1.i-pmAbCHuVR1IcAxdHyyz", p.getName());
            assertEquals("uNi39AW-61c0g5OF", p.getType());
            assertEquals("P_5-4f", p.getValue());
            p = props.next();
            assertEquals("k.bl_p7Ey94OwjywF0t8OxNuAs", p.getName());
            assertNull(p.getType());
            assertEquals("P5bRGnLlYzlyPyiDI1IPLILM", p.getValue());
            
            pl = pls.next();
            assertEquals(IPayload.Containment.EXTERNAL, pl.getContainment());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

        /**
     * Test of createFromFile method with full MMD content.
     */
    @Test
    public void test_DeleteIndicator() throws Exception {
        try {
            String path = this.getClass().getClassLoader().getResource("mmdtest/mmdtest2.xml").getPath();
            File   f = new File(path);
            MessageMetaData mmd = MessageMetaData.createFromFile(f);            
            assertNotNull(mmd);
            assertEquals(org.holodeckb2b.common.util.Utils.fromXMLDateTime("2014-08-14T15:50:00Z"), mmd.getTimestamp());
            assertEquals("H8MQXJ", mmd.getMessageId());
            assertEquals("UGA08vL5hsdNfC-sGoV2RD", mmd.getRefToMessageId());

            assertFalse(mmd.shouldDeleteFilesAfterSubmit());                    
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

        try {
            String path = this.getClass().getClassLoader().getResource("mmdtest/mmdtest3.xml").getPath();
            File   f = new File(path);
            MessageMetaData mmd = MessageMetaData.createFromFile(f);            
            assertNotNull(mmd);
            assertEquals(org.holodeckb2b.common.util.Utils.fromXMLDateTime("2015-12-21T15:50:00Z"), mmd.getTimestamp());
            assertEquals("n-soaDLzuliyRmzSlBe7", mmd.getMessageId());
            assertNull(mmd.getRefToMessageId());

            assertTrue(mmd.shouldDeleteFilesAfterSubmit());                    
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }
    
    /**
     * Test of constructor to create a MMD document for the user message described by 
     * an {@see IUserMessage} object.
     * For this test we use a {@see UserMessage} object from the persistency package. 
     */
    @Test
    public void test_CreateFromObject() {
        UserMessage um = new UserMessage();
        
        um.setMPC(T_UM1_MPC);
        um.setMessageId(T_UM1_MESSAGEID);
        um.setRefToMessageId(T_UM1_REFTOMSGID);
        
        org.holodeckb2b.ebms3.persistent.message.CollaborationInfo ci = new org.holodeckb2b.ebms3.persistent.message.CollaborationInfo();
        ci.setAction(T_UM1_ACTION);
        ci.setConversationId(T_UM1_CONVID);
        org.holodeckb2b.ebms3.persistent.general.Service svc = new org.holodeckb2b.ebms3.persistent.general.Service(T_UM1_SERVICE);
        ci.setService(svc);
        org.holodeckb2b.ebms3.persistent.message.AgreementReference agreeRef = new org.holodeckb2b.ebms3.persistent.message.AgreementReference(T_UM1_PMODEID);
        ci.setAgreement(agreeRef);
        um.setCollaborationInfo(ci);
        
        um.addMessageProperty(new org.holodeckb2b.ebms3.persistent.general.Property(T_UM1_MSGPROP1_NAME, T_UM1_MSGPROP1_VALUE, T_UM1_MSGPROP1_TYPE));
        um.addMessageProperty(new org.holodeckb2b.ebms3.persistent.general.Property(T_UM1_MSGPROP2_NAME, T_UM1_MSGPROP2_VALUE));
        
        Payload pl = new Payload();
        pl.setContainment(T_UM1_PAYLD1_CONTAINMENT);
        pl.setContentLocation(T_UM1_PAYLD1_LOC);
        pl.addProperty(new org.holodeckb2b.ebms3.persistent.general.Property(T_UM1_PAYLD1_PROP_NAME, T_UM1_PAYLD1_PROP_VALUE));
        pl.setSchemaReference(new org.holodeckb2b.ebms3.persistent.general.SchemaReference(T_UM1_PAYLD1_SCHEMA_LOC, T_UM1_PAYLD1_SCHEMA_NS, T_UM1_PAYLD1_SCHEMA_VER));
        um.addPayload(pl);
        
        pl = new Payload();
        pl.setContainment(T_UM1_PAYLD2_CONTAINMENT);
        pl.setContentLocation(T_UM1_PAYLD2_LOC);
        
        MessageMetaData mmd = new MessageMetaData(um);
        
        assertEqualUM(mmd, um);        
    }
    
    
    @Test
    public void test_WriteToFile() {
        UserMessage um = new UserMessage();
        
        um.setMPC(T_UM1_MPC);
        um.setMessageId(T_UM1_MESSAGEID);
        um.setRefToMessageId(T_UM1_REFTOMSGID);
        
        org.holodeckb2b.ebms3.persistent.message.CollaborationInfo ci = new org.holodeckb2b.ebms3.persistent.message.CollaborationInfo();
        ci.setAction(T_UM1_ACTION);
        ci.setConversationId(T_UM1_CONVID);
        org.holodeckb2b.ebms3.persistent.general.Service svc = new org.holodeckb2b.ebms3.persistent.general.Service(T_UM1_SERVICE);
        ci.setService(svc);
        org.holodeckb2b.ebms3.persistent.message.AgreementReference agreeRef = new org.holodeckb2b.ebms3.persistent.message.AgreementReference(T_UM1_PMODEID);
        ci.setAgreement(agreeRef);
        um.setCollaborationInfo(ci);
        
        MessageMetaData mmd = new MessageMetaData(um);
        String path = this.getClass().getClassLoader().getResource("mmdtest").getPath();
        File   f = new File(path+"/mmd_writetest.xml");
        
        if (f.exists())
            f.delete();
        
        try {
            mmd.writeToFile(f);
        } catch (Exception e) {
            fail("Writing MMD without payload info failed: " + e.getMessage());
        }
        MessageMetaData mmd2 = null;
        try {
            mmd2 = MessageMetaData.createFromFile(f);
        } catch (Exception e) {
            fail("Reading saved data (no payloads) failed: " + e.getMessage());
        }

        if (mmd2 != null)
            assertEqualUM(mmd, mmd2);
        else
            fail("MMD without payloads not read!");
        
        um.addMessageProperty(new org.holodeckb2b.ebms3.persistent.general.Property(T_UM1_MSGPROP1_NAME, T_UM1_MSGPROP1_VALUE, T_UM1_MSGPROP1_TYPE));
        um.addMessageProperty(new org.holodeckb2b.ebms3.persistent.general.Property(T_UM1_MSGPROP2_NAME, T_UM1_MSGPROP2_VALUE));
        
        Payload pl = new Payload();
        pl.setContainment(T_UM1_PAYLD1_CONTAINMENT);
        pl.setContentLocation(T_UM1_PAYLD1_LOC);
        pl.addProperty(new org.holodeckb2b.ebms3.persistent.general.Property(T_UM1_PAYLD1_PROP_NAME, T_UM1_PAYLD1_PROP_VALUE));
        pl.setSchemaReference(new org.holodeckb2b.ebms3.persistent.general.SchemaReference(T_UM1_PAYLD1_SCHEMA_LOC, T_UM1_PAYLD1_SCHEMA_NS, T_UM1_PAYLD1_SCHEMA_VER));
        um.addPayload(pl);
        
        pl = new Payload();
        pl.setContainment(T_UM1_PAYLD2_CONTAINMENT);
        pl.setContentLocation(T_UM1_PAYLD2_LOC);
        
        mmd = new MessageMetaData(um);
        
        if (f.exists())
            f.delete();
        
        try {
            mmd.writeToFile(f);
        } catch (Exception e) {
            fail("Writing MMD with payload ino failed: " + e.getMessage());
        }
        
        mmd2 = null;
        try {
            mmd2 = MessageMetaData.createFromFile(f);
        } catch (Exception e) {
            fail("Reading saved data (with payload info) failed: " + e.getMessage());
        }

        if (mmd2 != null)
            assertEqualUM(mmd, mmd2);
        else
            fail("MMD with payload info not read!");
        
        mmd.setDeleteFilesAfterSubmit(true);
        f.delete();
        try {
            mmd.writeToFile(f);
        } catch (Exception e) {
            fail("Writing MMD with delete flag set failed: " + e.getMessage());
        }
        try {
            mmd2 = MessageMetaData.createFromFile(f);
        } catch (Exception e) {
            fail("Reading saved data (with delete flag set) failed: " + e.getMessage());
        }     
        
        assertNotNull(mmd2);
        assertTrue(mmd2.shouldDeleteFilesAfterSubmit());
    }
    
    
    /**
     * Helper method to check that the given user messages are equal.
     * 
     * @param um1
     * @param um2 
     */
    private void assertEqualUM(IUserMessage um1, IUserMessage um2) {
        
        assertEquals(um1.getMPC(), um2.getMPC());
        assertEquals(um1.getMessageId(), um2.getMessageId());
        assertEquals(um1.getRefToMessageId(), um2.getRefToMessageId());
        
        ITradingPartner sender1 = um1.getSender();
        ITradingPartner sender2 = um2.getSender();
        if (sender1 != null) {
            assertNotNull(sender2);
            assertEquals(sender1.getRole(), sender2.getRole());
            
            Collection<IPartyId> pids1 = sender1.getPartyIds();
            Collection<IPartyId> pids2 = sender2.getPartyIds();
            
            if(pids1 != null && pids1.size() > 0) {
                assertNotNull(pids2);
                assertEquals(pids1.size(), pids2.size());
                
                for(IPartyId pid : pids1) {
                    boolean b = false;
                    for(IPartyId pid2 : pids2) {
                        if (pid.getId().equals(pid2.getId()))
                            if (pid.getType() != null)
                                b |= pid.getType().equals(pid2.getType());
                    }
                    assertTrue(b);
                }
            } else
                assertTrue(pids2 == null || pids2.size() == 0);
        } else
            assertNull(sender2);
        
        ITradingPartner receiver1 = um1.getSender();
        ITradingPartner receiver2 = um2.getSender();
        if (receiver1 != null) {
            assertNotNull(receiver2);
            assertEquals(receiver1.getRole(), receiver2.getRole());
            
            Collection<IPartyId> pids1 = receiver1.getPartyIds();
            Collection<IPartyId> pids2 = receiver2.getPartyIds();
            
            if(pids1 != null && pids1.size() > 0) {
                assertNotNull(pids2);
                assertEquals(pids1.size(), pids2.size());
                
                for(IPartyId pid : pids1) {
                    boolean b = false;
                    for(IPartyId pid2 : pids2) {
                        if (pid.getId().equals(pid2.getId()))
                            if (pid.getType() != null)
                                b |= pid.getType().equals(pid2.getType());
                    }
                    assertTrue(b);
                }
            } else
                assertTrue(pids2 == null || pids2.size() == 0);
        } else
            assertNull(receiver2);
        
        ICollaborationInfo ci1 = um1.getCollaborationInfo();
        ICollaborationInfo ci2 = um2.getCollaborationInfo();
        
        if (ci1 != null) {
            assertNotNull(ci2);
            assertEquals(ci1.getAction(), ci2.getAction());
            assertEquals(ci1.getConversationId(), ci2.getConversationId());
            
            IService svc1 = ci1.getService();
            IService svc2 = ci2.getService();
            if (svc1 != null) {
                assertNotNull(svc2);
                assertEquals(svc1.getName(), svc2.getName());
                assertEquals(svc1.getType(), svc2.getType());
            } else
                assertNull(svc2);
            
            IAgreementReference ar1 = ci1.getAgreement();
            IAgreementReference ar2 = ci2.getAgreement();
            if (ar1 != null) {
                assertNotNull(ar2);
                assertEquals(ar1.getName(), ar2.getName());
                assertEquals(ar1.getType(), ar2.getType());
                assertEquals(ar1.getPModeId(), ar2.getPModeId());
            } else
                assertNull(ar2);            
        } else
            assertNull(ci2);

        Collection<IProperty> props1 = um1.getMessageProperties();
        Collection<IProperty> props2 = um2.getMessageProperties();

        if(props1 != null && props1.size() > 0) {
            assertNotNull(props2);
            assertEquals(props1.size(), props1.size());

            for(IProperty p1 : props1) {
                boolean b = false;
                for(IProperty p2 : props2) {
                    if (p1.getName().equals(p2.getName())) {
                        if (p1.getType() != null)
                            b |= (p1.getType().equals(p2.getType()) &&
                                  (p1.getValue() != null ? p1.getValue().equals(p2.getValue()) : p2.getValue() == null));
                        else
                            b |= ( p2.getType() == null &&
                                  (p1.getValue() != null ? p1.getValue().equals(p2.getValue()) : p2.getValue() == null));
                    }
                }
                assertTrue(b);
            }
        } else
            assertTrue(props2 == null || props2.size() == 0);
        
        Collection<IPayload> pl1 = um1.getPayloads();
        Collection<IPayload> pl2 = um2.getPayloads();
        
        if(pl1 != null && pl1.size() > 0) {
            assertNotNull(pl2);
            assertEquals(pl1.size(), pl2.size());

            for(IPayload p1 : pl1) {
                boolean b = false;
                for(IPayload p2 : pl2) {
                    boolean m = false;
                    if (p1.getContentLocation() != null && p1.getContentLocation().equals(p2.getContentLocation())) { 
                        m = (p1.getMimeType() != null ? p1.getMimeType().equals(p2.getMimeType()) : p2.getMimeType() == null);
                        m &= (p1.getPayloadURI() != null ? p1.getPayloadURI().equals(p2.getPayloadURI()) : p2.getPayloadURI() == null); 
                    
                        ISchemaReference schema1 = p1.getSchemaReference();
                        ISchemaReference schema2 = p2.getSchemaReference();
                        if (schema1 != null && schema2 != null) {
                            m &= (schema1.getLocation() != null ? schema1.getLocation().equals(schema2.getLocation()) : schema2.getLocation() == null);
                            m &= (schema1.getNamespace()!= null ? schema1.getNamespace().equals(schema2.getNamespace()) : schema2.getNamespace() == null);
                            m &= (schema1.getVersion()!= null ? schema1.getVersion().equals(schema2.getVersion()) : schema2.getVersion() == null);
                        } else
                            m &= (schema1 == schema2); // both should be null

                        IDescription d1 = p1.getDescription();
                        IDescription d2 = p2.getDescription();
                        if (m && d1 != null && d2 != null ) {
                            if (d1.getLanguage() != null && d1.equals(d2.getLanguage()))
                                m &= (d1.getText() != null  ? d1.getText().equals(d2.getText()) : d2.getText() == null);
                        } else
                            m &= (d1 == d2);

                        props1 = p1.getProperties();
                        props2 = p2.getProperties();
                        if(props1 != null && props1.size() > 0 && props2 != null && props2.size() > 0) {
                            for(IProperty prop1 : props1) {
                                boolean mp = false;
                                for(IProperty prop2 : props2) {
                                    if (prop1.getName().equals(prop2.getName())) {
                                        if (prop1.getType() != null)
                                            b |= (prop1.getType().equals(prop2.getType()) &&
                                                  (prop1.getValue() != null ? prop1.getValue().equals(prop2.getValue()) : prop2.getValue() == null));
                                        else
                                            b |= ( prop2.getType() == null &&
                                                  (prop1.getValue() != null ? prop1.getValue().equals(prop2.getValue()) : prop2.getValue() == null));
                                    }
                                }
                                m &= mp;
                            }
                        } else
                            m &= (props2 == null || props2.size() == 0);                        
                        
                        b |= m;
                    } 
                }
                assertTrue(b);
            }
        } else
            assertTrue(pl2 == null || pl2.size() == 0);
    }
    
}