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
package org.holodeckb2b.pmode.xml;

import java.io.File;
import org.holodeckb2b.interfaces.pmode.ILeg;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class LegTest {
    
    public LegTest() {
        
    }
    
   /**
     * Create an Leg from file.
     * 
     * @param fName The filename for the leg
     * @return Leg or NULL in case of an error
     */
    public Leg createFromFile(String fName) {

        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/leg/" + fName).getPath());

            Serializer serializer = new Persister();
            return serializer.read(Leg.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }
    
    /**
     * Test leg being present (but can be empty).
     */
    @Test
    public void testLegNotNull() {
        
        try {
            Leg leg = createFromFile("leg1.xml");
            
            // when NULL no leg present
            assertNotNull(leg);
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }
    
    /**
     * Test leg with label attribute.
     */
    @Test
    public void testLegWithLabelAttribute() {
        
        try {
            Leg leg = createFromFile("leg2.xml");
            
            assertNotNull(leg);
            assertNotNull(leg.getLabel());
            assertEquals("REQUEST", ILeg.Label.REQUEST.toString());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    
    
    /**
     * Test leg without label attribute.
     */
    @Test
    public void testLegWithoutLabelAttribute() {
        
        try {
            Leg leg = createFromFile("leg3.xml");
            
            assertNotNull(leg);
            // test for missing label attribute
            assertNull(leg.getLabel());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }
    
    /**
     * Test leg WITH:
     * 
     * - label (=attribute)
     * - Protocol 
     * - Receipt 
     * - ReceptionAwareness 
     * - UserMessageFlow 
     * 
     * And WITHOUT: DefaultDelivery and PullRequestFlow
     * 
     */
    @Test
    public void testLegWithLabelAndElements1() {
        
        try {
            Leg leg = createFromFile("leg4.xml");
            
            assertNotNull(leg);
            assertNotNull(leg.getLabel());
            assertEquals("REQUEST", ILeg.Label.REQUEST.toString());
            assertNotNull(leg.getProtocol());
            assertNotNull(leg.getReceiptConfiguration());
            assertNotNull(leg.getReceptionAwareness());
            assertNotNull(leg.getUserMessageFlow());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    

    /**
     * Test leg WITH:
     * 
     * - label (=attribute)
     * - Protocol 
     * - Receipt 
     * - ReceptionAwareness 
     * - UserMessageFlow 
     * - DefaultDelivery 
     * - PullRequestFlow
     * 
     */
    @Test
    public void testLegWithLabelAndElements2() {
        
        try {
            Leg leg = createFromFile("leg5.xml");
            
            assertNotNull(leg);
            // label is an attribute
            assertNotNull(leg.getLabel());
            assertEquals("REQUEST", ILeg.Label.REQUEST.toString());
            
            assertNotNull(leg.getProtocol());
            assertNotNull(leg.getReceiptConfiguration());
            assertNotNull(leg.getReceptionAwareness());
            assertNotNull(leg.getDefaultDelivery());
            assertNotNull(leg.getPullRequestFlows());
            assertNotNull(leg.getUserMessageFlow());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    
    
}
