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
import java.util.concurrent.TimeUnit;
import org.holodeckb2b.interfaces.general.Interval;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx (bram at holodeck-b2b.org)
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
    public ReceptionAwareness createFromFile(final String fName) throws Exception {
        // retrieve the resource from the pmodetest directory.
        final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/receptionawareness/" + fName).getPath());

        final Serializer  serializer = new Persister();
        return serializer.read(ReceptionAwareness.class, f);
    }

    /**
     * Test pre 4.0.0 configuration with fixed intervals
     */
    @Test
    public void testFixedIntervals() {

        try {
            ReceptionAwareness ra;
            ra = createFromFile("fixed_1.xml");

            // Check ReceptionAwareness object
            assertNotNull(ra);
            assertArrayEquals(new Interval[] { new Interval(60, TimeUnit.SECONDS),
                                               new Interval(60, TimeUnit.SECONDS),
                                               new Interval(60, TimeUnit.SECONDS)
                                             }, ra.getWaitIntervals());

            ra = createFromFile("fixed_2.xml");
            // Check ReceptionAwareness object
            assertNotNull(ra);
            assertArrayEquals(new Interval[] { new Interval(5, TimeUnit.SECONDS) }, ra.getWaitIntervals());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }

    /**
     * Test post 4.0.0 configuration with flexible intervals
     */
    @Test
    public void testFlexibleIntervals() {
        try {
            ReceptionAwareness ra;
            ra = createFromFile("flexible_1.xml");
            // Check ReceptionAwareness object
            assertArrayEquals(new Interval[] { new Interval(5, TimeUnit.SECONDS),
                                               new Interval(10, TimeUnit.SECONDS),
                                               new Interval(15, TimeUnit.SECONDS)
                                             }, ra.getWaitIntervals());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        try {
            ReceptionAwareness ra;
            ra = createFromFile("flexible_2.xml");
            // Check ReceptionAwareness object
            assertArrayEquals(new Interval[] { new Interval(5, TimeUnit.SECONDS),
                                               new Interval(10, TimeUnit.SECONDS),
                                               new Interval(15, TimeUnit.SECONDS),
                                               new Interval(20, TimeUnit.SECONDS)
                                             }, ra.getWaitIntervals());
        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testEmptyInterval() {
        try {
            createFromFile("flexible_empty_1.xml");
            fail("Empty interval should be rejected");
        } catch (final Exception invalid) {
        }
        try {
            createFromFile("flexible_empty_2.xml");
            fail("Empty interval should be rejected");
        } catch (final Exception invalid) {
        }
        try {
            createFromFile("flexible_empty_3.xml");
            fail("Empty interval should be rejected");
        } catch (final Exception invalid) {
        }
    }

    @Test
    public void testNegativeIntervals() {
        try {
            createFromFile("fixed_negative.xml");
            fail("Negative interval should be rejected");
        } catch (final Exception invalid) {
        }
        try {
            createFromFile("flexible_negative_1.xml");
            fail("Negative interval should be rejected");
        } catch (final Exception invalid) {
        }
        try {
            createFromFile("flexible_negative_2.xml");
            fail("Negative interval should be rejected");
        } catch (final Exception invalid) {
        }
        try {
            createFromFile("flexible_negative_3.xml");
            fail("Negative interval should be rejected");
        } catch (final Exception invalid) {
        }
    }
}
