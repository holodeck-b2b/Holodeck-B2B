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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ErrorHandlingTest {

    public ErrorHandlingTest() {
    }

    private ErrorHandling createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/eh/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(ErrorHandling.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    @Test
    public void testCompleteErrorHandling() {
        try {
            final ErrorHandling eh = createFromFile("completeEH.xml");

            assertNotNull(eh);
            assertEquals(ReplyPattern.CALLBACK, eh.getPattern());
            assertEquals("http://test.holodeck-b2b.org/errors", eh.getReceiverErrorsTo());
            assertFalse(eh.shouldReportErrorOnError());
            assertTrue(eh.shouldReportErrorOnReceipt());
            assertTrue(eh.shouldAddSOAPFault());
            assertTrue(eh.shouldNotifyErrorToBusinessApplication());

            assertNotNull(eh.getErrorDelivery());
            // We do not test for settings of the error delivery as that is already tested separately

        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testEmptyErrorHandling() {
        try {
            final ErrorHandling eh = createFromFile("emptyEH.xml");

            assertNotNull(eh);
            assertEquals(ReplyPattern.RESPONSE, eh.getPattern());
            assertNull(eh.getReceiverErrorsTo());
            assertNull(eh.shouldReportErrorOnError());
            assertNull(eh.shouldReportErrorOnReceipt());
            assertFalse(eh.shouldAddSOAPFault());
            assertFalse(eh.shouldNotifyErrorToBusinessApplication());

            assertNull(eh.getErrorDelivery());
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testNoURLForCallback() {
        try {
            final ErrorHandling eh = createFromFile("noURL-callbackEH.xml");
            // Reading the document should fail because there is no reply URL
            assertNull(eh);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testDeliveryOnly() {
        try {
            final ErrorHandling eh = createFromFile("deliveryOnly_EH.xml");

            assertNotNull(eh);
            assertEquals(ReplyPattern.RESPONSE, eh.getPattern());
            assertNull(eh.getReceiverErrorsTo());
            assertNull(eh.shouldReportErrorOnError());
            assertNull(eh.shouldReportErrorOnReceipt());
            assertFalse(eh.shouldAddSOAPFault());
            assertFalse(eh.shouldNotifyErrorToBusinessApplication());

            assertNotNull(eh.getErrorDelivery());
        } catch (final Exception e) {
            fail();
        }
    }
}
