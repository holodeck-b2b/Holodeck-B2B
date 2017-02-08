/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.File;

import static org.junit.Assert.*;

/**
 * Created at 23:16 29.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SchemaTest {

    private static final QName SCHEMA_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Schema");

    private OMElement plElement; // PayloadInfo

    @Before
    public void setUp() throws Exception {
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("packagetest/mmd_pcktest.xml").getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);

        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        // Creating PayloadInfo element from mmd
        plElement = PayloadInfo.createElement(umElement, mmd.getPayloads());
    }

    @Test
    public void testCreateElement() throws Exception {
        OMElement piElement = PartInfoElement.createElement(plElement, new Payload());
        SchemaReference schema = new SchemaReference();
        schema.setLocation("somewhere");
        schema.setNamespace("namespace1");
        schema.setVersion("test");

        OMElement schemaElement = Schema.createElement(piElement, schema);
        assertNotNull(schemaElement);
        assertEquals(SCHEMA_ELEMENT_NAME, schemaElement.getQName());
        assertEquals("somewhere",
                schemaElement.getAttributeValue(new QName("location")));
        assertEquals("namespace1",
                schemaElement.getAttributeValue(new QName("namespace")));
        assertEquals("test",
                schemaElement.getAttributeValue(new QName("version")));
    }

    @Test
    public void testGetElement() throws Exception {
        Payload payload = new Payload();
        SchemaReference schema = new SchemaReference();
        schema.setLocation("somewhere");
        schema.setNamespace("namespace1");
        schema.setVersion("test");
        payload.setSchemaReference(schema);
        OMElement piElement = PartInfoElement.createElement(plElement, payload);

        OMElement schemaElement = Schema.getElement(piElement);
        assertNotNull(schemaElement);
        assertEquals("somewhere",
                schemaElement.getAttributeValue(new QName("location")));
        assertEquals("namespace1",
                schemaElement.getAttributeValue(new QName("namespace")));
        assertEquals("test",
                schemaElement.getAttributeValue(new QName("version")));
    }

    @Test
    public void testReadElement() throws Exception {
        Payload payload = new Payload();
        SchemaReference testSchema = new SchemaReference();
        testSchema.setLocation("somewhere");
        testSchema.setNamespace("namespace1");
        testSchema.setVersion("test");
        payload.setSchemaReference(testSchema);
        OMElement piElement = PartInfoElement.createElement(plElement, payload);
        OMElement schemaElement = Schema.createElement(piElement, testSchema);

        SchemaReference schema = Schema.readElement(schemaElement);
        assertNotNull(schema);
        assertEquals("somewhere", schema.getLocation());
        assertEquals("namespace1", schema.getNamespace());
        assertEquals("test", schema.getVersion());
    }
}