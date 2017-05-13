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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.holodeckb2b.core.testhelpers.TestUtils;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PullRequestFlowTest {

    public PullRequestFlowTest() {
    }

    /**
     * Create an PullRequestFlow from file.
     *
     * @param fName The filename for the PullRequestFlow
     * @return PullRequestFlow or NULL in case of an error
     * @throws Exception
     */
    private PullRequestFlow createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final String filePath = TestUtils.getPath(this.getClass(), "pmodetest/prf/" + fName);
            final File f = new File(filePath);

            final Serializer  serializer = new Persister();
            return serializer.read(PullRequestFlow.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    @Test
    public void testCompletePullRequestFlow() {
        try {
            final PullRequestFlow prf = createFromFile("complete_PRF.xml");

            assertNotNull(prf);
            assertEquals("http://test.holodeck-b2b.org/mpc/0", prf.getMPC());

            // We only check that error handling and security configuration were there, content is tested in separate
            // tests
            assertNotNull(prf.getErrorHandlingConfiguration());
            assertNotNull(prf.getSecurityConfiguration());
        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testEmptyPullRequestFlow() {
        try {
            final PullRequestFlow prf = createFromFile("empty_PRF.xml");

            assertNull(prf);
        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testMinimalPullRequestFlow() {
        try {
            final PullRequestFlow prf = createFromFile("minimal_PRF.xml");

            assertNotNull(prf);
            assertEquals("http://test.holodeck-b2b.org/mpc/0", prf.getMPC());

            assertNull(prf.getErrorHandlingConfiguration());
            assertNull(prf.getSecurityConfiguration());
        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
}
