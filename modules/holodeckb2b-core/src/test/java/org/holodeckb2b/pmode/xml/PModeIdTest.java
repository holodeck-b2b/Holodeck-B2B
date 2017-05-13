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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.holodeckb2b.core.testhelpers.TestUtils;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class PModeIdTest {

    public PModeIdTest() {

    }

    /**
     * Create an PMode from file.
     *
     * @param fName The filename for the PMode
     * @return PMode or NULL in case of an error
     */
    public PMode createFromFile(final String fName) {

        try {
            // retrieve the resource from the pmodetest directory.
            final String filePath = TestUtils.getPath(this.getClass(), "pmodetest/pmodeid/" + fName);
            final File f = new File(filePath);

            final Serializer  serializer = new Persister();
            return serializer.read(PMode.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    /**
     * Test for PMode ID.
     */
    @Test
    public void testPModeID() {

        try {
            final PMode pmode = createFromFile("minimalPModeID1.xml");

            // Check PMode ID object
            assertNotNull(pmode.getId());
            assertEquals("id0", pmode.getId());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }


    /**
     * Test for existing include attribute with PModeID.
     */
    @Test
    public void testPModeIDInclude() {

        try {
            final PMode pmode = createFromFile("minimalPModeID1.xml");

            // Check PMode ID
            assertNotNull(pmode.getId());
            assertEquals("id0", pmode.getId());

            // check the PMode include parameter
            assertTrue("true", pmode.includeId());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }

    /**
     * Test for non-existing include attribute with PModeID.
     */
    @Test
    public void testPModeIDNoInclude() {

        try {
            final PMode pmode = createFromFile("minimalPModeID2.xml");

            // Check PMode ID
            assertNotNull(pmode.getId());
            assertEquals("id0", pmode.getId());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }

}
