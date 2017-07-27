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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SignatureConfigurationTest {

    public SignatureConfigurationTest() {
    }

    private SignatureConfiguration createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/sig/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(SignatureConfiguration.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    /**
     * Test complete configuration for validation
     */
    @Test
    public void testCompleteConfig1() {
        try {
            final SignatureConfiguration sigCfg = createFromFile("completeCfg1.xml");
            assertEquals("KeystoreAlias0", sigCfg.getKeystoreAlias());
            assertFalse(sigCfg.enableRevocationCheck());
        } catch (final Exception e) {
            fail();
        }
    }

    /**
     * Test of minimal configuration for validation
     */
    @Test
    public void testMinimalConfig1() {
        try {
            final SignatureConfiguration sigCfg = createFromFile("minimalCfg1.xml");
            assertEquals("KeystoreAlias1", sigCfg.getKeystoreAlias());
            assertNull(sigCfg.enableRevocationCheck());
        } catch (final Exception e) {
            fail();
        }
    }

    /**
     * Test complete configuration for signing
     */
    @Test
    public void testCompleteConfig2() {
        try {
            final SignatureConfiguration sigCfg = createFromFile("completeCfg2.xml");
            assertEquals("KeystoreAlias2", sigCfg.getKeystoreAlias());
            assertEquals("keypwd2$%'s;:@#$:!", sigCfg.getCertificatePassword());

            assertNull(sigCfg.enableRevocationCheck());

            assertEquals(X509ReferenceType.BSTReference, sigCfg.getKeyReferenceMethod());
            assertTrue(sigCfg.includeCertificatePath());
            assertEquals("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", sigCfg.getSignatureAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmldsig-more#sha384", sigCfg.getHashFunction());

        } catch (final Exception e) {
            fail();
        }
    }

    /**
     * Test of minimal configuration for siging
     */
    @Test
    public void testMinimalConfig2() {
        try {
            final SignatureConfiguration sigCfg = createFromFile("minimalCfg2.xml");
            assertEquals("KeystoreAlias3", sigCfg.getKeystoreAlias());
            assertEquals("727dhjkvdjk%%#^&%dgg", sigCfg.getCertificatePassword());

            assertNull(sigCfg.enableRevocationCheck());
            assertNull(sigCfg.getKeyReferenceMethod());
            assertNull(sigCfg.includeCertificatePath());
            assertNull(sigCfg.getSignatureAlgorithm());
            assertNull(sigCfg.getHashFunction());

        } catch (final Exception e) {
            fail();
        }
    }

    /**
     * Test of incorrect configuration missing keystore alias
     */
    @Test
    public void testMissingAliasConfig() {
        try {
            final SignatureConfiguration sigCfg = createFromFile("missingAlias.xml");

            assertNull(sigCfg);
        } catch (final Exception e) {
            fail();
        }
    }
}
