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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Tests of packaging ebMS meta data in SOAP header
 *
 * @todo: Add more tests for variations in mmd completeness
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PackagingTest {

    private final File    ebMSschemaFile;
    private final File    SOAP11schemaFile;
    private final File    SOAP12schemaFile;
    private final File    xmlSchemaFile;

    public PackagingTest() {
        ebMSschemaFile = new File(this.getClass().getClassLoader()
                                                 .getResource("xsd/ebms-header-3_0-200704_refactored.xsd").getPath());
        SOAP11schemaFile = new File(this.getClass().getClassLoader().getResource("xsd/soap11-envelope.xsd").getPath());
        SOAP12schemaFile = new File(this.getClass().getClassLoader().getResource("xsd/soap12-envelope.xsd").getPath());
        xmlSchemaFile = new File(this.getClass().getClassLoader().getResource("xsd/xml.xsd").getPath());
    }

    /**
     * Test of creating the SOAP header for one user message only
     */
    @Test
    public void testUserMessageOnly() {

        final SOAPEnvelope    env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        final SOAPHeaderBlock messaging = Messaging.createElement(env);

        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);

        final OMElement umElement = UserMessageElement.createElement(messaging, mmd);

        // The SOAP enveloppe should be valid according to the ebMS schema, write the
        // xml to file and validate it using Xerces
        final String xmlPath = TestUtils.getPath(this.getClass(), "packagetest") + "/xml_testUserMessageOnly.xml";
        final File   xmlFile = new File(xmlPath);
        try {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileWriter(xmlFile));
            env.serialize(writer);
            writer.flush();
        } catch (final XMLStreamException xse) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, xse);
            fail();
        } catch (final IOException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }

        assertValidXML(xmlPath);
    }

    /*
     * Helper to assert that the generated XML document is valid according to XML Schema of the ebMS Spec
     */
    private void assertValidXML(final String xmlFile) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
            dbf.setValidating(true);
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                                "http://www.w3.org/2001/XMLSchema");
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource",
                                new File[] {ebMSschemaFile, SOAP11schemaFile, SOAP12schemaFile, xmlSchemaFile});
            final Document xml = dbf.newDocumentBuilder().parse( new File( xmlFile)  );
        } catch (final SAXException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (final FileNotFoundException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (final IOException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (final ParserConfigurationException ex) {
            Logger.getLogger(PackagingTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }

    }
}