/*
 * Copyright (C) 2015 The Holodeck B2B Team
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

import org.holodeckb2b.pmode.xml.EncryptionConfiguration;
import java.io.File;
import org.holodeckb2b.common.security.X509ReferenceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class EncryptionConfigurationTest {
    
    public EncryptionConfigurationTest() {
    }
    
    
    /**
     * Create an EncryptionConfiguration from file.
     * 
     * @param fName The filename for the EncryptionConfiguration
     * @return EncryptionConfiguration or NULL in case of an error
     * @throws Exception 
     */
    private EncryptionConfiguration createFromFile(String fName) throws Exception {
    
        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/encr/" + fName).getPath());
            
            Serializer  serializer = new Persister();
            return serializer.read(EncryptionConfiguration.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    /**
     * Test complete EncryptionConfiguration.
     */
    @Test
    public void testEncryptionConfigurationComplete() {
        try {
            EncryptionConfiguration ec = createFromFile("encryptionConfigComplete.xml");
        
            assertNotNull(ec);
            assertEquals("partyc", ec.getKeystoreAlias());
            assertEquals("ExampleC", ec.getCertificatePassword());
            assertEquals("http://www.w3.org/2001/04/xmlenc#aes128-cbc", ec.getAlgorithm() );
            
            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", ec.getKeyTransport().getAlgorithm());
            assertEquals("http://www.w3.org/2009/xmlenc11#mgf1sha1", ec.getKeyTransport().getMGFAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", ec.getKeyTransport().getDigestAlgorithm());
            assertEquals("BSTReference", ec.getKeyTransport().getKeyReferenceMethod().toString() );
           
        } catch (Exception e) {
            fail();
        }            
    }

    /**
     * Test minimal EncryptionConfiguration.
     */

    @Test
    public void testEncryptionConfigurationMinimal() {
        try {
            EncryptionConfiguration ec = createFromFile("encryptionConfigMinimal.xml");
        
            assertNotNull(ec);
            assertEquals("partyc", ec.getKeystoreAlias());
            assertEquals("ExampleC", ec.getCertificatePassword());
            assertNull(ec.getAlgorithm() );
            assertNull(ec.getKeyTransport());
           
        } catch (Exception e) {
            fail();
        }            
    }
    
        /**
     * Test minimal EncryptionConfiguration.
     */

    @Test
    public void testEncryptionConfigurationKTOnly () {
        try {
            EncryptionConfiguration ec = createFromFile("encryptionConfigKTOnly.xml");
        
            assertNotNull(ec);
            assertEquals("partyc", ec.getKeystoreAlias());
            assertEquals("ExampleC", ec.getCertificatePassword());
            assertNull(ec.getAlgorithm() );
            
            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep", ec.getKeyTransport().getAlgorithm());
            assertEquals("http://www.w3.org/2009/xmlenc11#mgf1sha1", ec.getKeyTransport().getMGFAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", ec.getKeyTransport().getDigestAlgorithm());
            assertEquals(X509ReferenceType.IssuerAndSerial, ec.getKeyTransport().getKeyReferenceMethod());
           
        } catch (Exception e) {
            fail();
        }            
    }
    
}



    
    