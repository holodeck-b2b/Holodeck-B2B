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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class DeliverySpecificationTest {

    public DeliverySpecificationTest() {

    }

    /**
     * Create an DeliverySpecification from file.
     *
     * @param fName The filename for the DeliverySpecification
     * @return DeliverySpecification or NULL in case of an error
     */
    public DeliverySpecification createFromFile(final String fName)  {

        try {
            // retrieve the resource from the pmodetest directory.
            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/deliveryspecification/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(DeliverySpecification.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    /**
     * Test for minimal DeliverySpecification.
     */
    @Test
    public void testDeliverySpecificationMimimal() {

        try {
            final DeliverySpecification ds = createFromFile("deliverySpecificationMinimal.xml");

            // Check DeliverySpecification object
            assertNotNull(ds);
            assertNull(ds.getSettings());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }

    /**
     * Test for full DeliverySpecification.
     */
    @Test
    public void testDeliverySpecificationFull() {

        try {
            final DeliverySpecification ds = createFromFile("deliverySpecificationFull.xml");

            // Check DeliverySpecification object
            assertNotNull(ds);

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }
}
