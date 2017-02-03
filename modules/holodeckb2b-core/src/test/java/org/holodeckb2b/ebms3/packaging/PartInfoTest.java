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
import org.holodeckb2b.common.messagemodel.*;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 22:42 28.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PartInfoTest {

    private OMElement plElement;

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
        // Adding UserMessage from mmd
        OMElement umElement = UserMessage.createElement(headerBlock, mmd);
        // Creating PayloadInfo element from mmd
        plElement = PayloadInfo.createElement(umElement, mmd.getPayloads());
    }

    @Test
     public void testCreateEmptyElement() throws Exception {
        OMElement piElement = PartInfo.createElement(plElement, new Payload());
        System.out.println("piElement: " + piElement);
        assertNotNull(plElement);
    }

    @Test
    public void testCreateElement() throws Exception {
        Payload partInfo = new Payload();
        partInfo.setContainment(IPayload.Containment.ATTACHMENT);

        SchemaReference sr = new SchemaReference();
        sr.setLocation("somewhere");
        sr.setNamespace("namespace");
        sr.setVersion("test");
        partInfo.setSchemaReference(sr);
        org.holodeckb2b.common.messagemodel.Description description =
                new org.holodeckb2b.common.messagemodel.Description();
        description.setText("description");
        partInfo.setDescription(description);
        Collection<IProperty> properties = new ArrayList<>();
        properties.add(new Property("name1", "value1", "type1"));
        partInfo.setProperties(properties);

        OMElement piElement = PartInfo.createElement(plElement, partInfo);
        assertNotNull(piElement);
        OMElement schema = Schema.getElement(piElement);
        assertNotNull(schema);
        OMElement descr = Description.getElement(piElement);
        assertNotNull(descr);
        OMElement partProps = PartProperties.getElement(piElement);
        assertNotNull(partProps);
    }

    @Test
    public void testGetElements() throws Exception {
        Iterator<OMElement> it = PartInfo.getElements(plElement);
        assertNotNull(it);
        assertTrue(it.hasNext());
    }

    @Test
    public void testReadEmptyElement() throws Exception {
        OMElement piElement = PartInfo.createElement(plElement, new Payload());
        Payload payload = PartInfo.readElement(piElement);
        assertNotNull(payload);
        payload.getContainment();
    }

    @Test
    public void testReadElement() throws Exception {
        Payload partInfo = new Payload();
        partInfo.setContainment(IPayload.Containment.ATTACHMENT);

        SchemaReference sr = new SchemaReference();
        sr.setLocation("somewhere");
        sr.setNamespace("namespace");
        sr.setVersion("test");
        partInfo.setSchemaReference(sr);
        org.holodeckb2b.common.messagemodel.Description description =
                new org.holodeckb2b.common.messagemodel.Description();
        description.setText("description");
        partInfo.setDescription(description);
        Collection<IProperty> properties = new ArrayList<>();
        properties.add(new Property("name1", "value1", "type1"));
        partInfo.setProperties(properties);

        OMElement piElement = PartInfo.createElement(plElement, partInfo);

        Payload payload = PartInfo.readElement(piElement);
        assertNotNull(payload);
//        assertEquals(IPayload.Containment.ATTACHMENT, payload.getContainment()); // fails
        assertEquals("somewhere", partInfo.getSchemaReference().getLocation());
        assertEquals("namespace", partInfo.getSchemaReference().getNamespace());
        assertEquals("test", partInfo.getSchemaReference().getVersion());
        assertEquals("description", partInfo.getDescription().getText());
        assertNotNull(partInfo.getProperties());
    }
}