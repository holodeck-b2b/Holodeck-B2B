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

import org.holodeckb2b.pmode.xml.Agreement;
import java.io.File;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class AgreementTest {
    
    public AgreementTest() {
        
    }
    
    /**
     * Create an agreement from file.
     * 
     * @param fName The filename for the agreement
     * @return Agreement or NULL in case of an error
     */
    public Agreement createFromFile(String fName) {

        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/agreement/" + fName).getPath());

            Serializer  serializer = new Persister();
            return serializer.read(Agreement.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }
    
    /**
     * Test agreement for not being null.
     */
    @Test
    public void testAgreementNotNull() {
        
        try {
            Agreement agreement = createFromFile("agreement2.xml");
            
            assertNotNull(agreement);
            
        } catch (Exception ex) {
            // Logger.getLogger(Agreement.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
        
    }
    
    /**
     * Test agreement for existing agreement type.
     */
    @Test
    public void testAgreementGetType() {
        
        try {
            Agreement agreement = createFromFile("agreement2.xml");
            
            // assertNotNull(agreement);
            assertEquals("type4", agreement.getType());
            
        } catch (Exception ex) {
            // Logger.getLogger(Agreement.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }

    /**
     * Test agreement for exiting agreement name.
     */
    @Test
    public void testAgreementGetName() {
        
        try {
            Agreement agreement = createFromFile("agreement2.xml");
            
            // assertNotNull(agreement);
            assertEquals("name2", agreement.getName());
            
        } catch (Exception ex) {
            // Logger.getLogger(Agreement.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    
    
    /**
     * Test agreement name (agreement1.xml contains agreement name only and no type).
     */
    @Test
    public void testAgreementGetNameOnly() {
        
        try {
            Agreement agreement = createFromFile("agreement1.xml");
            
            assertEquals("name6", agreement.getName());
            
        } catch (Exception ex) {
            // Logger.getLogger(Agreement.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }       
    
    /**
     * Test of setName method, of class Agreement.
     */
    @Test
    public void testAgreementSetName() {

        String setName = "";
        String getName = "";
        
        Agreement instance = new Agreement();
        instance.setName(setName);
        getName = instance.getName();
        assertEquals(setName, getName);
    }

    /**
     * Test of setType method, of class Agreement.
     */
    @Test
    public void testAgreementSetType() {
        
        String setType = "";
        String getType = "";
        
        Agreement instance = new Agreement();
        instance.setType(setType);
        getType = instance.getType();
        assertEquals(setType, getType);
    }
    
}
