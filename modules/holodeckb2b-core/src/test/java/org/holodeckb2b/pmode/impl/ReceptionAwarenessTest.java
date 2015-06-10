/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class ReceptionAwarenessTest {
 
    public ReceptionAwarenessTest() {
    }
    
    /**
     * Create an ReceptionAwareness from file.
     * 
     * @param fName The filename for the ReceptionAwareness
     * @return ReceptionAwareness or NULL in case of an error
     * @throws Exception 
     */
    public ReceptionAwareness createFromFile(String fName) throws Exception  {

        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/receptionawareness/" + fName).getPath());

            Serializer  serializer = new Persister();
            return serializer.read(ReceptionAwareness.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }
 
    /**
     * Test minimal ReceptionAwareness.
     */
    @Test
    public void testReceptionAwarenessMinimal() {
        
        try {
            ReceptionAwareness ra;
            ra = createFromFile("receptionawarenessMinimal.xml");
            
            // Check ReceptionAwareness object
            assertNotNull(ra);
            assertEquals(2, ra.getMaxRetries());
            assertEquals(60L, ra.getRetryInterval().getLength());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    
    
    /**
     * Test for full ReceptionAwareness.
     */
    @Test
    public void testReceptionAwarenessFull() {
        
        try {
            ReceptionAwareness ra;
            ra = createFromFile("receptionawarenessFull.xml");
            
            // Check ReceptionAwareness object
            assertNotNull(ra);
            assertEquals(3, ra.getMaxRetries());
            assertEquals(30L, ra.getRetryInterval().getLength());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    

}
