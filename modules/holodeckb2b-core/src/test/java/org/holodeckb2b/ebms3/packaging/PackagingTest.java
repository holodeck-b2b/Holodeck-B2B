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
package org.holodeckb2b.ebms3.packaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Tests of packaging ebMS meta data in SOAP header
 * 
 * @todo: Add more tests for variations in mmd completeness
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PackagingTest {
    
    private String    ebMSschemaFile;
    private String    SOAP11schemaFile;
    private String    SOAP12schemaFile;
    
    
    
    public PackagingTest() {
        ebMSschemaFile = this.getClass().getClassLoader().getResource("xsd/ebms-header-3_0-200704_refactored.xsd").getPath();
        SOAP11schemaFile = this.getClass().getClassLoader().getResource("xsd/soap11-envelope.xsd").getPath();
        SOAP12schemaFile = this.getClass().getClassLoader().getResource("xsd/soap12-envelope.xsd").getPath();
    }
    
    /**
     * Test of creating the SOAP header for one user message only 
     */
    @Test
    public void testUserMessageOnly() {
        
        SOAPEnvelope    env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        SOAPHeaderBlock messaging = Messaging.createElement(env);
        
        // Use correctly filled mmd document for testing
        String mmdPath = this.getClass().getClassLoader().getResource("packagetest/mmd_pcktest.xml").getPath();
        File   f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        
        OMElement umElement = UserMessage.createElement(messaging, mmd);
        
        // The SOAP enveloppe should be valid according to the ebMS schema, write the 
        // xml to file and validate it using Xerces
        String xmlPath = this.getClass().getClassLoader().getResource("packagetest").getPath() + "/xml_testUserMessageOnly.xml";
        File   xmlFile = new File(xmlPath);
        try {
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileWriter(xmlFile));
            env.serialize(writer); 
            writer.flush();
        } catch (XMLStreamException xse) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, xse);
            fail();
        } catch (IOException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        
        assertValidXML(xmlPath);
    }
    
    /*
     * Helper to assert that the generated XML document is valid according to XML Schema of the ebMS Spec
     */
    private void assertValidXML(String xmlFile) {
        try {        
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
            dbf.setNamespaceAware( true );  
            dbf.setValidating(true);
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", 
                                new String[] {ebMSschemaFile, SOAP11schemaFile, SOAP12schemaFile});
            Document xml = dbf.newDocumentBuilder().parse( new InputSource( new FileReader(xmlFile) ) );              
        } catch (SAXException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (IOException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }

    }
}