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
package org.holodeckb2b.pmode.impl;

import java.io.File;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class ProtocolTest {
    
    public ProtocolTest() {
        
    }
    
    /**
     * Create an Protocol from file.
     * 
     * @param fName The filename for the Protocol
     * @return Protocol or NULL in case of an error
     * @throws Exception 
     */
    public Protocol createFromFile(String fName) throws Exception  {

        try {
            // retrieve the resource from the pmodetest directory.
            File f = new File(this.getClass().getClassLoader().getResource("pmodetest/protocol/" + fName).getPath());

            Serializer serializer = new Persister();
            return serializer.read(Protocol.class, f);
        }
        catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
        
    }
    
    
    /**
     * Test for Protocol not being NULL.
     */
    @Test
    public void testProtocolNotNull() {
        
        try {
            Protocol protocol;
            protocol = createFromFile("protocolEmpty.xml");
            
            assertNotNull(protocol);
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }

    
    /**
     * Test for the address of the Protocol not being NULL.
     */
    @Test
    public void testProtocolAddressNotNull() {
        
        try {
            Protocol protocol = createFromFile("protocolAddress1.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.getAddress());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    

    /**
     * Test for the address value of the Protocol.
     */
    @Test
    public void testProtocolAddressValue() {
        
        try {
            Protocol protocol = createFromFile("protocolAddress2.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.getAddress());
            assertEquals("http://www.holodeck-b2b.org/", protocol.getAddress());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }      
    
    /**
     * Test for the SOAP version of the Protocol not being NULL.
     */
    @Test
    public void testProtocolSoapVersionNotNull() {
        
        try {
            Protocol protocol = createFromFile("protocolSoapVersion1.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.getSOAPVersion());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    
    
    
    /**
     * Test for the SOAP version value of the Protocol.
     */
    @Test
    public void testProtocolSoapVersionValue() {
        
        try {
            Protocol protocol = createFromFile("protocolSoapVersion2.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.getSOAPVersion());
            assertEquals("1.2", protocol.getSOAPVersion());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }     
    
    /**
     * Test for chunking of the Protocol not being NULL.
     */
    @Test
    public void testProtocolUseChunkingNotNull() {
        
        try {
            Protocol protocol = createFromFile("protocolChunking1.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.useChunking());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }    
    
    /**
     * Test for chunking value of the Protocol.
     */
    @Test
    public void testProtocolUseChunkingValue() {
        
        try {
            Protocol protocol = createFromFile("protocolChunking2.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.useChunking());
            assertEquals(Boolean.FALSE, protocol.useChunking());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    } 
    
    /**
     * Test for HTTP compression of the Protocol not being NULL.
     */
    @Test
    public void testProtocolUseHttpCompressionNotNull() {
        
        try {
            Protocol protocol = createFromFile("protocolHttpCompression1.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.useHTTPCompression());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }      
    
    /**
     * Test for HTTP compression value of the Protocol.
     */
    @Test
    public void testProtocolUseHttpCompressionValue() {
        
        try {
            Protocol protocol = createFromFile("protocolHttpCompression2.xml");
            
            assertNotNull(protocol);
            assertNotNull(protocol.useHTTPCompression());
            
        } catch (Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        
    }          
    
}
