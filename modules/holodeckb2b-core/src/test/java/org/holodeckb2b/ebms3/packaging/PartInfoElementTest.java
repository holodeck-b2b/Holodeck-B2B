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
import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 22:42 28.01.17
 *
 * Checked for cases coverage (29.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PartInfoElementTest {

    private static final QName USER_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");
    private static final QName PAYLOAD_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");

    private OMElement plElement;

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);
        // Create the element
        OMElement umElement = headerBlock.getOMFactory()
                .createOMElement(USER_MESSAGE_ELEMENT_NAME, headerBlock);
        // Create the element
        plElement = headerBlock.getOMFactory()
                .createOMElement(PAYLOAD_INFO_ELEMENT_NAME, umElement);
    }

    @Test
     public void testCreateEmptyElement() throws Exception {
        OMElement piElement = PartInfoElement.createElement(plElement, new Payload());
        System.out.println("piElement: " + piElement);
        assertNotNull(piElement);
    }

    @Test
    public void testCreateElementForAttachmentContainmentType() throws Exception {
        Payload partInfo = new Payload();

        // check attachment
        partInfo.setContainment(IPayload.Containment.ATTACHMENT);
        String payloadURI = "path_to_file";
        partInfo.setPayloadURI(payloadURI);

        OMElement piElement = PartInfoElement.createElement(plElement, partInfo);
        assertNotNull(piElement);

        // check the presence of the href attribute
        assertEquals("cid:" + payloadURI,
                piElement.getAttribute(new QName("href")).getAttributeValue());
    }

    @Test
    public void testCreateElementForExternalContainmentType() throws Exception {
        Payload partInfo = new Payload();

        addOptionalAttributesToPartInfo(partInfo);

        // check attachment
        partInfo.setContainment(IPayload.Containment.EXTERNAL);
        String payloadURI = "path_to_file";
        partInfo.setPayloadURI(payloadURI);

        OMElement piElement = PartInfoElement.createElement(plElement, partInfo);
        assertNotNull(piElement);

        // check the presence of the href attribute
        assertEquals(payloadURI,
                piElement.getAttribute(new QName("href")).getAttributeValue());

        checkPresenceOfOptionalOMElements(piElement);
    }

    @Test
    public void testCreateElementForBodyContainmentType() throws Exception {
        Payload partInfo = new Payload();

        addOptionalAttributesToPartInfo(partInfo);

        // check attachment
        partInfo.setContainment(IPayload.Containment.BODY);
        String payloadURI = "path_to_file";
        partInfo.setPayloadURI(payloadURI);

        OMElement piElement = PartInfoElement.createElement(plElement, partInfo);
        assertNotNull(piElement);

        // check the presence of the href attribute
        assertEquals("#" + payloadURI,
                piElement.getAttribute(new QName("href")).getAttributeValue());

        checkPresenceOfOptionalOMElements(piElement);
    }

    @Test
    public void testGetElements() throws Exception {
        Payload partInfo = new Payload();

        PartInfoElement.createElement(plElement, partInfo);

        Iterator<OMElement> it = PartInfoElement.getElements(plElement);
        assertNotNull(it);
        assertTrue(it.hasNext());
    }

    @Test
    public void testReadEmptyElement() throws Exception {
        OMElement piElement = PartInfoElement.createElement(plElement, new Payload());
        Payload payload = PartInfoElement.readElement(piElement);
        assertNotNull(payload);
        payload.getContainment();
    }

    @Test
    public void testReadElementForAttachmentContainmentType() throws Exception {
        Payload partInfo = new Payload();

        // check attachment
        partInfo.setContainment(IPayload.Containment.ATTACHMENT);
        partInfo.setPayloadURI("path_to_file");

        OMElement piElement = PartInfoElement.createElement(plElement, partInfo);

        Payload payload = PartInfoElement.readElement(piElement);
        assertNotNull(payload);
        assertEquals(IPayload.Containment.ATTACHMENT, payload.getContainment());
    }

    @Test
    public void testReadElementForExternalContainmentType() throws Exception {
        Payload partInfo = new Payload();

        addOptionalAttributesToPartInfo(partInfo);

        // check external
        partInfo.setContainment(IPayload.Containment.EXTERNAL);
        partInfo.setPayloadURI("path_to_file");

        OMElement piElement = PartInfoElement.createElement(plElement, partInfo);

        Payload payload = PartInfoElement.readElement(piElement);
        assertNotNull(payload);
        checkPresenceOfOptionalAttributes(payload);
    }

    private void addOptionalAttributesToPartInfo(Payload partInfo) {
        SchemaReference sr = new SchemaReference();
        sr.setLocation("somewhere");
        sr.setNamespace("namespace");
        sr.setVersion("test");
        partInfo.setSchemaReference(sr);
        Description description = new Description();
        description.setText("description");
        partInfo.setDescription(description);
        Collection<IProperty> properties = new ArrayList<>();
        properties.add(new Property("name1", "value1", "type1"));
        partInfo.setProperties(properties);
    }

    private void checkPresenceOfOptionalOMElements(OMElement piElement) {
        OMElement schema = SchemaElement.getElement(piElement);
        assertNotNull(schema);
        OMElement descr = DescriptionElement.getElement(piElement);
        assertNotNull(descr);
        OMElement partProps = PartPropertiesElement.getElement(piElement);
        assertNotNull(partProps);
    }

    private void checkPresenceOfOptionalAttributes(Payload payload) {
        assertEquals(IPayload.Containment.EXTERNAL, payload.getContainment());
        assertEquals("somewhere", payload.getSchemaReference().getLocation());
        assertEquals("namespace", payload.getSchemaReference().getNamespace());
        assertEquals("test", payload.getSchemaReference().getVersion());
        assertEquals("description", payload.getDescription().getText());
        assertNotNull(payload.getProperties());
    }
}