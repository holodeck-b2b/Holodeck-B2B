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
import java.util.List;
import org.holodeckb2b.interfaces.general.IPartyId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class TradingPartnerConfigurationTest {
    
    public TradingPartnerConfigurationTest() {
    }
    
    private TradingPartnerConfiguration createFromFile(String fName) throws Exception {
    
        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/tp/" + fName).getPath());
            
            Serializer  serializer = new Persister();
            return serializer.read(TradingPartnerConfiguration.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }
    
    @Test
    public void testCompleteTP() {
        try {
            TradingPartnerConfiguration tpc = createFromFile("completeTP.xml");
            
            assertNotNull(tpc);
            
            List<IPartyId> pids = (List<IPartyId>) tpc.getPartyIds();
            assertNotNull(pids); assertFalse(pids.isEmpty());
            
            assertEquals("org:holodeck-b2b:party-id-1" , pids.get(0).getId()); assertNull(pids.get(0).getType());
            assertEquals("PartyId3" , pids.get(1).getId()); assertEquals("hb2b-test-pid", pids.get(1).getType());
            
            assertEquals("Tester", tpc.getRole());
            
            assertNotNull(tpc.getSecurityConfiguration());            
        } catch (Exception e) {
            fail();
        } 
    }
    
    @Test
    public void testMinimalTP() {
        try {
            TradingPartnerConfiguration tpc = createFromFile("minimalTP.xml");
            assertNotNull(tpc);
            
            List<IPartyId> pids = (List<IPartyId>) tpc.getPartyIds();
            assertNotNull(pids); assertFalse(pids.isEmpty());
            
            assertEquals("org:holodeck-b2b:party-id-2" , pids.get(0).getId()); assertNull(pids.get(0).getType());
            
            assertNull(tpc.getRole());
            assertNull(tpc.getSecurityConfiguration());
        } catch (Exception e) {
            fail();
        } 
    }
    
    @Test
    public void testNoRoleTP() {
        try {
            TradingPartnerConfiguration tpc = createFromFile("noRoleTP.xml");
            assertNotNull(tpc);
            
            List<IPartyId> pids = (List<IPartyId>) tpc.getPartyIds();
            assertNotNull(pids); assertFalse(pids.isEmpty());
            
            assertEquals("PartyId3" , pids.get(0).getId()); assertEquals("hb2b-test-pid", pids.get(0).getType());
            
            assertNull(tpc.getRole());
            assertNotNull(tpc.getSecurityConfiguration());
        } catch (Exception e) {
            fail();
        } 
    }
    
}
