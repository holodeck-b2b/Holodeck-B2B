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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.pmode.security.X509ReferenceType;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class KeyTransportTest {

    public KeyTransportTest() {
    }


    /**
     * Create an KeyTransport configuration from file.
     *
     * @param fName The filename for the EncryptionConfiguration
     * @return EncryptionConfiguration or NULL in case of an error
     * @throws Exception
     */
    private KeyTransport createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final String filePath = TestUtils.getPath(this.getClass(), "pmodetest/kt/" + fName);
            final File f = new File(filePath);

            final Serializer  serializer = new Persister();
            return serializer.read(KeyTransport.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    /**
     * Test complete Key Transport.
     */
    @Test
    public void testKeyTransportComplete() {
        try {
            final KeyTransport kt = createFromFile("keytransportComplete.xml");

            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep", kt.getAlgorithm() );
            assertEquals("http://www.w3.org/2009/xmlenc11#mgf1sha256", kt.getMGFAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha512", kt.getDigestAlgorithm());
            assertEquals(X509ReferenceType.BSTReference, kt.getKeyReferenceMethod());

        } catch (final Exception e) {
            fail();
        }
    }

    /**
     * Test at least one child requirement
     */
    @Test
    public void testKTAtLeastOneChild() {
        try {
            final KeyTransport kt = createFromFile("keytransportNoChild.xml");

            assertNull(kt);

        } catch (final Exception e) {

        }
    }

    /**
     * Test MGF required for RSA-OAEP
     */
    @Test
    public void testKTMGFRequired() {
        try {
            final KeyTransport kt = createFromFile("keytransportNoMGF_RSAOAEP.xml");

            assertNull(kt);

        } catch (final Exception e) {

        }
    }

    /**
     * Test with only KT algorithm specified
     */
    @Test
    public void testKTAlgoOnly() {
        try {
            final KeyTransport kt = createFromFile("keytransportAlgoOnly.xml");

            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", kt.getAlgorithm() );
            assertNull(kt.getMGFAlgorithm());
            assertNull(kt.getDigestAlgorithm());
            assertNull(kt.getKeyReferenceMethod());

        } catch (final Exception e) {
            fail();
        }
    }

    /**
     * Test with only KT algorithm specified
     */
    @Test
    public void testKTRefMethodOnly() {
        try {
            final KeyTransport kt = createFromFile("keytransportRefMethodOnly.xml");

            assertNull(kt.getAlgorithm() );
            assertNull(kt.getMGFAlgorithm());
            assertNull(kt.getDigestAlgorithm());
            assertEquals(X509ReferenceType.KeyIdentifier, kt.getKeyReferenceMethod());

        } catch (final Exception e) {
            fail();
        }
    }


}




