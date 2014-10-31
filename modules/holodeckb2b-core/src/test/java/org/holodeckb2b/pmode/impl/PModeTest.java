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
package org.holodeckb2b.pmode.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.general.IPartyId;
import org.holodeckb2b.common.general.ReplyPattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class PModeTest {
    
    private static PMode pmode = null;
    private static File f = null;
    private static String path = "";
    

    public PModeTest() {
        
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    
    @Test
    public void test01_CreateFromFile() throws Exception {
    
        try {
            pmode = new PMode();
            
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // The PMode itself cannot be NULL.           
            assertNotNull(pmode);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    @Test
    public void test02_PMode() throws Exception {
    
        try {
            pmode = new PMode();
            
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // The PMode itself cannot be NULL.           
            assertNotNull(pmode);
            
             // Check PMode ID
            assertNotNull(pmode.getId());
            assertEquals("id0", pmode.getId());
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    
    @Test
    public void test03_PModeMep() throws Exception {
    
        try {
            pmode = new PMode();
            
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // The PMode itself cannot be NULL.           
            assertNotNull(pmode);
            
             // Check for the correct PMode ID and possible NULL value
            assertNotNull(pmode.getId());
            assertEquals("id0", pmode.getId());
            
            // Check PMode Mep and possible NULL value
            assertNotNull(pmode.getMep());
            assertEquals("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay", pmode.getMep());
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    
    @Test
    public void test04_PModeMepBinding() throws Exception {
    
        try {
            pmode = new PMode();
            
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // The PMode itself cannot be NULL.           
            assertNotNull(pmode);
            
             // Check PMode ID
            assertNotNull(pmode.getId());
            assertEquals("id0", pmode.getId());
            
            // Check PMode Mep
            assertNotNull(pmode.getMep());
            assertEquals("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay", pmode.getMep());
            
            // Check PMode MepBinding
            assertNotNull(pmode.getMepBinding());
            assertEquals("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push", pmode.getMepBinding());
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }    
    
    @Test
    public void test05_Initiator() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check initiator for NULL value
            assertNotNull(pmode.getInitiator());
            
            // Check for the actual PartyId's names (there are two here)
            // Note that collection has NO concept of order!
            
            // setup array for later assertArrayEquals check.
            String[] exptectedInitiatorArray = {"PartyId0","PartyId1"};
            String[] resultInitiatorArray = {"",""};
            
            Collection<IPartyId> partyIDInitiator = pmode.getInitiator().getPartyIds();
            Iterator iterator2 = partyIDInitiator.iterator();
            
            int i = 0; // array index counter in order to fill resultArray
            
            while (iterator2.hasNext()) {
                PartyId partyId = (PartyId) iterator2.next();
                resultInitiatorArray[i] = partyId.getId();
                i++;
            }
            
            // check to see if the two expected values for PartyId's match
            assertArrayEquals(exptectedInitiatorArray, resultInitiatorArray);
            
            // check the initiator role
            assertEquals("Role0",pmode.getInitiator().getRole());
            
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    
    @Test
    public void test06_Responder() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check responder for NULL value
            assertNotNull(pmode.getResponder() );
            
            // Check for the actual PartyId's names (there are two here)
            // Note that collection has NO concept of order!
            
            // setup array for later assertArrayEquals check.
            String[] exptectedResponderArray = {"PartyId2","PartyId3"};
            String[] resultResponderArray = {"",""};
            
            Collection<IPartyId> partyIDResponder = pmode.getResponder().getPartyIds();
            Iterator iterator2 = partyIDResponder.iterator();
            
            int i = 0; // array index counter in order to fill resultArray
            
            while (iterator2.hasNext()) {
                PartyId partyId = (PartyId) iterator2.next();
                resultResponderArray[i] = partyId.getId();
                i++;
            }
            
            // check to see if the two expected values for PartyId's match
            assertArrayEquals(exptectedResponderArray, resultResponderArray);
            
            // check the responder role
            assertEquals("Role1",pmode.getResponder().getRole());
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    
    @Test
    public void test07_Agreement() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check agreement for NULL value
            assertNotNull(pmode.getAgreement());
            
            // check agrement name
            assertNotNull(pmode.getAgreement().getName());
            assertEquals("name0", pmode.getAgreement().getName());
            
            // check agrement type            
            assertNotNull(pmode.getAgreement().getType());
            assertEquals("type0", pmode.getAgreement().getType());
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    @Test
    public void test08_Leg() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check legs for NULL value
            assertNotNull(pmode.getLegs());
            
            // check legs, there is just one leg here
            assertEquals(1, pmode.getLegs().size());
            
            List<Leg> legs = pmode.getLegs();
            
            for (Leg leg : legs) {
                
                // check legs for Protocol
                assertNotNull(leg.getProtocol());
                
                // check legs for Receipt
                assertNotNull(leg.getReceiptConfiguration());

                // check legs for ReceptionAwareness
                assertNotNull(leg.getReceptionAwareness());

                // check legs for DefaultDelivery
                assertNotNull(leg.getDefaultDelivery());

                // check legs for PullRequestFlow (multiple here)
                assertNotNull(leg.getPullRequestFlows());

                // check legs for UserMessageFlow
                assertNotNull(leg.getUserMessageFlow());

                // check legs for Label
                assertNotNull(leg.getLabel());
            }
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }    
    
    @Test
    public void test09_Protocol() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check legs for NULL value
            assertNotNull(pmode.getLegs());
            
            // get the list of legs (can be multiple legs)
            List<Leg> legs = pmode.getLegs();
            
            for (Leg leg : legs) {
                
                // check legs for Protocol
                assertNotNull(leg.getProtocol());
                
                // get the protocol
                Protocol protocol = (Protocol) leg.getProtocol();
                
                // check the address of the leg
                assertEquals("http://www.oxygenxml.com/", protocol.getAddress());
                
                // check the soapversion of the leg
                assertEquals("1.2", protocol.getSOAPVersion());
                
                // check the usechunking setting of the leg
                assertEquals(false, protocol.useChunking());
                
                // check the UseHTTPCompression setting of the leg
                assertEquals(false, protocol.useHTTPCompression());
                
            }
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
    @Test
    public void test10_Receipt() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check legs for NULL value
            assertNotNull(pmode.getLegs());
            
            // get the list of legs (can be multiple legs)
            List<Leg> legs = pmode.getLegs();
            
            for (Leg leg : legs) {
                
                // check legs for ReceiptConfiguration
                assertNotNull(leg.getReceiptConfiguration());
                
                // get the ReceiptConfiguration
                ReceiptConfiguration receiptConfiguration = (ReceiptConfiguration) leg.getReceiptConfiguration();
                
                // check the ReplyPattern of the ReceiptConfiguration
                assertEquals("RESPONSE", receiptConfiguration.getPattern().toString());
                
                // check the To of the ReceiptConfiguration
                assertEquals("To0", receiptConfiguration.getTo());
                
            }
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void test11_ReceptionAwareness() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check legs for NULL value
            assertNotNull(pmode.getLegs());
            
            // get the list of legs (can be multiple legs)
            List<Leg> legs = pmode.getLegs();
            
            for (Leg leg : legs) {
                
                // check legs for ReceptionAwareness
                assertNotNull(leg.getReceptionAwareness());
                
                // get the ReceptionAwareness
                ReceptionAwareness receptionAwareness = (ReceptionAwareness) leg.getReceptionAwareness();
                
                // check the MaxRetries of the ReceptionAwareness
                assertEquals(0, receptionAwareness.getMaxRetries());
                
                // check the RetryInterval of the ReceptionAwareness
                assertEquals(10, receptionAwareness.getRetryInterval().getLength());
                
            }
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }    
    
    @Test
    public void test11_DefaultDelivery() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check legs for NULL value
            assertNotNull(pmode.getLegs());
            
            // get the list of legs (can be multiple legs)
            List<Leg> legs = pmode.getLegs();
            
            for (Leg leg : legs) {
                
                // check legs for DefaultDelivery not NULL
                assertNotNull(leg.getDefaultDelivery());
                
                // get the DefaultDelivery
                DeliverySpecification deliverySpecification = (DeliverySpecification) leg.getDefaultDelivery();
                
                // check DeliveryMethod for DefaultDelivery 
                assertEquals("DeliveryMethod0", deliverySpecification.getFactory());
                
                // check multiple Parameter element for DefaultDelivery
                HashMap<String, String> parameters = (HashMap<String, String>) deliverySpecification.getSettings();
                
                // setup arrays for later assertArrayEquals check.
                String[] expectedParametersNameArray = {"name1","name2"};
                String[] expectedParametersValue = {"value0","value1"};
                String[] resultParametersNameArray = {"",""};
                String[] resultParametersValueArray = {"",""};
                
                // loop trough the items in the parameters HashMap
                int i = 0;
                
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    resultParametersNameArray[i] = entry.getKey();
                    resultParametersValueArray[i] = entry.getValue();
                    i++;
                }
                
                // compare retrieved names
                assertArrayEquals(expectedParametersNameArray, resultParametersNameArray);
                // compare retrieved values
                assertArrayEquals(expectedParametersValue, resultParametersValueArray);
                
            }
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }    
     
    
    @Test
    public void test12_PullRequestFlow() throws Exception {
        
        try {
            pmode = new PMode();
            // retrieve the resource from the pmodetest directory.
            path = this.getClass().getClassLoader().getResource("pmodetest/instance1.xml").getPath();
            f = new File(path);
            
            pmode = PMode.createFromFile(f);
            
            // check legs for NULL value
            assertNotNull(pmode.getLegs());
            
            // get the list of legs (can be multiple legs)
            List<Leg> legs = pmode.getLegs();
            
            for (Leg leg : legs) {
                
                // check legs for PullRequestFlow not NULL
                assertNotNull(leg.getPullRequestFlows());
                
                // get all the PullRequestFlows from the legs
                List<Flow> pullRequestFlows = leg.getPullRequestFlows();
                
                for (Flow flow : pullRequestFlows ) {

                    // check PullRequestFlows for NULL value of BusinessInfo
                    assertNotNull(flow.getBusinessInfo());
                    
                    // get the BusinessInfo object
                    BusinessInfo businessInfo = flow.getBusinessInfo();
                    
                    // check MPC element not NULL
                    assertNotNull(businessInfo.getMpc());
                    
                    // check MPC element value
                    assertEquals("http://www.oxygenxml.com/", businessInfo.getMpc());
                    
                    
                    // check PullRequestFlows for NULL value of ErrorHandling
                    assertNotNull(flow.getErrorHandlingConfiguration());
                    
                    // get the ErrorHandling object
                    ErrorHandling errorHandling = flow.getErrorHandlingConfiguration();
                    
                    // check ReplyPattern for NULL value
                    assertNotNull(errorHandling.getPattern());
                    
                    // check value of ReplyPattern
                    assertEquals(ReplyPattern.RESPONSE, errorHandling.getPattern());
                    
                    
                    // check NotifyErrorToBusinessApplication for NULL value
                    assertNotNull(errorHandling.shouldNotifyErrorToBusinessApplication());
                    
                    // check value of NotifyErrorToBusinessApplication
                    assertEquals(Boolean.FALSE, errorHandling.shouldNotifyErrorToBusinessApplication());
                    
                    
                    // check ErrorDelivery for NULL value
                    assertNotNull(errorHandling.getErrorDelivery());
                    
                    // get ErrorDelivery object
                    DeliverySpecification deliverySpecification = (DeliverySpecification) errorHandling.getErrorDelivery();
                            
                    // check for DeliveryMethod value
                    assertNotNull(deliverySpecification.getFactory());
                    
                    }

            }
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
    
}