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
import org.holodeckb2b.interfaces.pmode.security.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class SecurityConfigurationTest {

    public SecurityConfigurationTest() {
    }

    /**
     * Create an SecurityConfiguration from file.
     *
     * @param fName The filename for the SecurityConfiguration
     * @return SecurityConfiguration or NULL in case of an error
     * @throws Exception
     */
    private SecurityConfiguration createFromFile(final String fName) throws Exception {

        try {
            // retrieve the resource from the pmodetest directory.
            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/sec/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(SecurityConfiguration.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    @Test
    public void testCompleteConfig() {
        try {
            final SecurityConfiguration sc = createFromFile("full_SC.xml");

            assertNotNull(sc);

            // Check the default UT
            final IUsernameTokenConfiguration defUT =
                                sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT);
            assertNotNull(defUT);
            assertEquals("FBXBcTNKQugmw4HyW0.aM", defUT.getUsername());
            assertEquals("q2TxAH81QeG", defUT.getPassword());
            assertEquals(UTPasswordType.DIGEST, defUT.getPasswordType());
            assertTrue(defUT.includeNonce());
            assertTrue(defUT.includeCreated());

            // Check the ebms UT
            final IUsernameTokenConfiguration ebmsUT =
                                sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS);
            assertNotNull(ebmsUT);
            assertEquals("DmTnJLxXFgEsuQX", ebmsUT.getUsername());
            assertEquals("ymjl8hCo0BdTvcRf", ebmsUT.getPassword());
            assertEquals(UTPasswordType.TEXT, ebmsUT.getPasswordType());
            assertFalse(ebmsUT.includeNonce());
            assertFalse(ebmsUT.includeCreated());

            // Check the Signature config
            final ISigningConfiguration signatureCfg = sc.getSignatureConfiguration();
            assertNotNull(signatureCfg);
            assertEquals("KeystoreAlias2" ,signatureCfg.getKeystoreAlias());
            assertEquals("http://www.w3.org/2001/04/xmldsig-more#sha384", signatureCfg.getHashFunction());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }

    }

    @Test
    public void testMoreThanTwoUT() {
        try {
            final SecurityConfiguration sc = createFromFile("threeUT_SC.xml");

            assertNull(sc);
        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testSameTargetUTs() {
        try {
            final SecurityConfiguration sc = createFromFile("sameTargetUT_SC.xml");

            assertNull(sc);
        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testOnlyEbMSUT() {
        try {
            final SecurityConfiguration sc = createFromFile("onlyEbmsUT_SC.xml");

            assertNotNull(sc);

            // Check the default UT
            final IUsernameTokenConfiguration defUT =
                                sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT);
            assertNull(defUT);

            // Check the ebms UT
            final IUsernameTokenConfiguration ebmsUT =
                                sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS);
            assertNotNull(ebmsUT);
            assertEquals("DmTnJLxXFgEsuQX", ebmsUT.getUsername());
            assertEquals("ymjl8hCo0BdTvcRf", ebmsUT.getPassword());
            assertEquals(UTPasswordType.TEXT, ebmsUT.getPasswordType());
            assertFalse(ebmsUT.includeNonce());
            assertFalse(ebmsUT.includeCreated());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testSignatureOnly() {
        try {
            final SecurityConfiguration sc = createFromFile("signatureOnlySC.xml");

            assertNotNull(sc);

            // Check that there are no UT
            assertNull(sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS));
            assertNull(sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT));

            // Check the Signature config
            final ISigningConfiguration signatureCfg = sc.getSignatureConfiguration();
            assertNotNull(signatureCfg);
            assertEquals("KeystoreAlias3" ,signatureCfg.getKeystoreAlias());
            assertEquals("http://www.w3.org/2001/04/xmldsig-more#sha384", signatureCfg.getHashFunction());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }

    @Test
    public void testEncryptionOnly() {
        try {
            final SecurityConfiguration sc = createFromFile("encryptionOnlySC.xml");

            assertNotNull(sc);

            // Check that there are no UT
            assertNull(sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS));
            assertNull(sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT));

            // Check the Encryption config
            final IEncryptionConfiguration encryptionCfg = sc.getEncryptionConfiguration();
            assertNotNull(encryptionCfg);

            assertEquals("partyc", encryptionCfg.getKeystoreAlias());
            assertEquals("ExampleC", encryptionCfg.getCertificatePassword());
            assertEquals("http://www.w3.org/2001/04/xmlenc#aes128-cbc", encryptionCfg.getAlgorithm() );

            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", encryptionCfg.getKeyTransport().getAlgorithm());
            assertEquals("http://www.w3.org/2009/xmlenc11#mgf1sha1", encryptionCfg.getKeyTransport().getMGFAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", encryptionCfg.getKeyTransport().getDigestAlgorithm());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }


    @Test
    public void testSignatureAndEncryption() {
        try {
            final SecurityConfiguration sc = createFromFile("signatureAndEncryptionSC.xml");

            assertNotNull(sc);

            // Check that there are no UT
            assertNull(sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS));
            assertNull(sc.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT));

            // Check the Signature config
            final ISigningConfiguration signatureCfg = sc.getSignatureConfiguration();
            assertNotNull(signatureCfg);
            assertEquals("KeystoreAlias3" ,signatureCfg.getKeystoreAlias());
            assertEquals("http://www.w3.org/2001/04/xmldsig-more#sha384", signatureCfg.getHashFunction());

            // Check the Encryption config
            final IEncryptionConfiguration encryptionCfg = sc.getEncryptionConfiguration();
            assertNotNull(encryptionCfg);

            assertEquals("partyc", encryptionCfg.getKeystoreAlias());
            assertEquals("ExampleC", encryptionCfg.getCertificatePassword());
            assertEquals("http://www.w3.org/2001/04/xmlenc#aes128-cbc", encryptionCfg.getAlgorithm() );

            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", encryptionCfg.getKeyTransport().getAlgorithm());
            assertEquals("http://www.w3.org/2009/xmlenc11#mgf1sha1", encryptionCfg.getKeyTransport().getMGFAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", encryptionCfg.getKeyTransport().getDigestAlgorithm());

        } catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
    }
}
