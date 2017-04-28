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

import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UsernameTokenTest {


    public UsernameTokenTest() {
    }

    private UsernameToken createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/ut/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(UsernameToken.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    @Test
    public void testCompleteUT() {
        try {
            final UsernameToken ut = createFromFile("completeUT.xml");

            assertNotNull(ut);
            assertEquals("ebms", ut.target);
            assertEquals("captain", ut.getUsername());
            assertEquals("1234567890", ut.getPassword());
            assertEquals(IUsernameTokenConfiguration.PasswordType.DIGEST, ut.getPasswordType());
            Assert.assertTrue(ut.includeNonce());
            Assert.assertFalse(ut.includeCreated());

        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testDefaultUT() {
        try {
            final UsernameToken ut = createFromFile("defaultUT.xml");

            assertNotNull(ut);
            assertNull(ut.target);
            assertEquals("captain", ut.getUsername());
            assertEquals("0987654321", ut.getPassword());
            assertEquals(IUsernameTokenConfiguration.PasswordType.DIGEST, ut.getPasswordType());
            Assert.assertTrue(ut.includeNonce());
            Assert.assertTrue(ut.includeCreated());

        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testTextPwdUT() {
        try {
            final UsernameToken ut = createFromFile("textPwdUT.xml");

            assertNotNull(ut);
            assertEquals("captain", ut.getUsername());
            assertEquals("0129384756", ut.getPassword());
            assertEquals(IUsernameTokenConfiguration.PasswordType.TEXT, ut.getPasswordType());
            Assert.assertTrue(ut.includeNonce());
            Assert.assertTrue(ut.includeCreated());

        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testTextPwdOnlyUT() {
        try {
            final UsernameToken ut = createFromFile("textPwdOnlyUT.xml");

            assertNotNull(ut);
            assertEquals("captain", ut.getUsername());
            assertEquals("0129384756", ut.getPassword());
            assertEquals(IUsernameTokenConfiguration.PasswordType.TEXT, ut.getPasswordType());
            Assert.assertFalse(ut.includeNonce());
            Assert.assertFalse(ut.includeCreated());

        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void testNoPassword() {
        try {
            UsernameToken ut = createFromFile("noPwdUT.xml");
            assertNull(ut);

            ut = createFromFile("emptyPwdUT.xml");
            assertNull(ut);
        } catch (final Exception e) {
            fail();
        }
    }


}
