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
import static org.junit.Assert.fail;

import java.io.File;

import org.holodeckb2b.core.testhelpers.TestUtils;
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
     */
    public ReceptionAwareness createFromFile(final String fName) {

        try {
            // retrieve the resource from the pmodetest directory.
            final String filePath = TestUtils.getPath(this.getClass(), "pmodetest/receptionawareness/" + fName);
            final File f = new File(filePath);
//            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/receptionawareness/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(ReceptionAwareness.class, f);
        }
        catch (final Exception ex) {
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

        } catch (final Exception ex) {
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

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }

}
