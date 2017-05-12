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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TradingPartnerConfigurationTest {

    public TradingPartnerConfigurationTest() {
    }

    private TradingPartnerConfiguration createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final String filePath = TestUtils.getPath(this.getClass(), "pmodetest/tp/" + fName);
            final File f = new File(filePath);

            final Serializer  serializer = new Persister();
            return serializer.read(TradingPartnerConfiguration.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    @Test
    public void testCompleteTP() {
        try {
            final TradingPartnerConfiguration tpc = createFromFile("completeTP.xml");

            assertNotNull(tpc);

            final List<IPartyId> pids = (List<IPartyId>) tpc.getPartyIds();
            assertNotNull(pids); assertFalse(pids.isEmpty());

            assertEquals("org:holodeck-b2b:party-id-1" , pids.get(0).getId()); assertNull(pids.get(0).getType());
            assertEquals("PartyId3" , pids.get(1).getId()); assertEquals("hb2b-test-pid", pids.get(1).getType());

            assertEquals("Tester", tpc.getRole());

            assertNotNull(tpc.getSecurityConfiguration());
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testMinimalTP() {
        try {
            final TradingPartnerConfiguration tpc = createFromFile("minimalTP.xml");
            assertNotNull(tpc);

            final List<IPartyId> pids = (List<IPartyId>) tpc.getPartyIds();
            assertNotNull(pids); assertFalse(pids.isEmpty());

            assertEquals("org:holodeck-b2b:party-id-2" , pids.get(0).getId()); assertNull(pids.get(0).getType());

            assertNull(tpc.getRole());
            assertNull(tpc.getSecurityConfiguration());
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testNoRoleTP() {
        try {
            final TradingPartnerConfiguration tpc = createFromFile("noRoleTP.xml");
            assertNotNull(tpc);

            final List<IPartyId> pids = (List<IPartyId>) tpc.getPartyIds();
            assertNotNull(pids); assertFalse(pids.isEmpty());

            assertEquals("PartyId3" , pids.get(0).getId()); assertEquals("hb2b-test-pid", pids.get(0).getType());

            assertNull(tpc.getRole());
            assertNotNull(tpc.getSecurityConfiguration());
        } catch (final Exception e) {
            fail();
        }
    }

}
