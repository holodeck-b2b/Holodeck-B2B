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

import org.holodeckb2b.pmode.xml.KeyTransport;
import java.io.File;
import org.holodeckb2b.common.security.X509ReferenceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
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
    private KeyTransport createFromFile(String fName) throws Exception {
    
        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/kt/" + fName).getPath());
            
            Serializer  serializer = new Persister();
            return serializer.read(KeyTransport.class, f);
        }
        catch (Exception ex) {
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
            KeyTransport kt = createFromFile("keytransportComplete.xml");
        
            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep", kt.getAlgorithm() );
            assertEquals("http://www.w3.org/2009/xmlenc11#mgf1sha256", kt.getMGFAlgorithm());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha512", kt.getDigestAlgorithm());
            assertEquals(X509ReferenceType.BSTReference, kt.getKeyReferenceMethod());
           
        } catch (Exception e) {
            fail();
        }            
    }

    /**
     * Test at least one child requirement
     */
    @Test
    public void testKTAtLeastOneChild() {
        try {
            KeyTransport kt = createFromFile("keytransportNoChild.xml");
            
            assertNull(kt);
           
        } catch (Exception e) {
            
        }            
    }
    
    /**
     * Test MGF required for RSA-OAEP
     */
    @Test
    public void testKTMGFRequired() {
        try {
            KeyTransport kt = createFromFile("keytransportNoMGF_RSAOAEP.xml");
            
            assertNull(kt);
           
        } catch (Exception e) {

        }            
    }
    
    /**
     * Test with only KT algorithm specified
     */
    @Test
    public void testKTAlgoOnly() {
        try {
            KeyTransport kt = createFromFile("keytransportAlgoOnly.xml");
        
            assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", kt.getAlgorithm() );
            assertNull(kt.getMGFAlgorithm());
            assertNull(kt.getDigestAlgorithm());
            assertNull(kt.getKeyReferenceMethod());
            
        } catch (Exception e) {
            fail();
        }            
    }
    
    /**
     * Test with only KT algorithm specified
     */
    @Test
    public void testKTRefMethodOnly() {
        try {
            KeyTransport kt = createFromFile("keytransportRefMethodOnly.xml");
        
            assertNull(kt.getAlgorithm() );
            assertNull(kt.getMGFAlgorithm());
            assertNull(kt.getDigestAlgorithm());
            assertEquals(X509ReferenceType.KeyIdentifier, kt.getKeyReferenceMethod());
            
        } catch (Exception e) {
            fail();
        }            
    }
    
    
}



    
    