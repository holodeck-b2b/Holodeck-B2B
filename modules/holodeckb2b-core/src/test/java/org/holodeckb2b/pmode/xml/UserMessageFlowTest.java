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

import org.holodeckb2b.pmode.xml.UserMessageFlow;
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
public class UserMessageFlowTest {
    
    public UserMessageFlowTest() {
        
    }
    
        /**
     * Create an UserMessageFlow from file.
     * 
     * @param fName The filename for the UserMessageFlow
     * @return UserMessageFlow or NULL in case of an error
     */
    public UserMessageFlow createFromFile(String fName)  {

        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/usermessageflow/" + fName).getPath());

            Serializer  serializer = new Persister();
            return serializer.read(UserMessageFlow.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }
    
    /**
     * Test for UserMessageFlow minimal.
     */
    @Test
    public void testUserMessageFlowMinimal() {
        
        try {
            UserMessageFlow umFlow = createFromFile("userMessageFlowMinimal.xml");
            
            // Check UserMessageFlow object
            assertNotNull(umFlow);
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    

    /**
     * Test for UserMessageFlow full.
     */
    @Test
    public void testUserMessageFlowFull() {
        
        try {
            UserMessageFlow umFlow = createFromFile("userMessageFlowFull.xml");
            
            // Check UserMessageFlow object
            assertNotNull(umFlow);
            assertNotNull(umFlow.getBusinessInfo());
            assertEquals("StoreMessage", umFlow.getBusinessInfo().getAction());
            assertEquals("Examples", umFlow.getBusinessInfo().getService().getName());
            assertEquals("org:holodeckb2b:services", umFlow.getBusinessInfo().getService().getType());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }        

}

