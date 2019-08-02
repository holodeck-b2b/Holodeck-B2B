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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 15:16 19.02.17
 *
 * Checked for cases coverage (29.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PropertyElementTest {

    private static final QName Q_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "UserMessage");
    private static final QName PROPERTY_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Property");

    private OMElement umElement;

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);
        // Create the element
        umElement = headerBlock.getOMFactory()
                .createOMElement(Q_ELEMENT_NAME, headerBlock);
    }

    @Test
    public void testCreateElementAsPartPropertiesChild() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        properties.add(new Property("some_property02", "some_value02", "some_type02"));
        OMElement ppElement = PartPropertiesElement.createElement(umElement, properties);

        Property property =
                new Property("some_new_property01", "some_new_value01", "some_new_type01");
        OMElement pElement = PropertyElement.createElement(ppElement, property);

        TestUtils.checkPropertyElementContent(pElement, "some_new_property01",
                "some_new_value01", "some_new_type01");
    }

    @Test
    public void testCreateElementAsMessagePropertiesChild() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        properties.add(new Property("some_property02", "some_value02", "some_type02"));
        OMElement mpElement = MessagePropertiesElement.createElement(umElement, properties);

        Property property =
                new Property("some_new_property01", "some_new_value01", "some_new_type01");
        OMElement pElement = PropertyElement.createElement(mpElement, property);

        TestUtils.checkPropertyElementContent(pElement, "some_new_property01",
                "some_new_value01", "some_new_type01");
    }

    @Test
    public void testGetElementsAsPartPropertiesChild() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        properties.add(new Property("some_property02", "some_value02", "some_type02"));
        OMElement ppElement = PartPropertiesElement.createElement(umElement, properties);

        Iterator<OMElement> it = PropertyElement.getElements(ppElement);

        assertTrue(it.hasNext());
        OMElement pElem = it.next();
        TestUtils.checkPropertyElementContent(pElem, "some_property01", "some_value01", "some_type01");
        pElem = it.next();
        TestUtils.checkPropertyElementContent(pElem, "some_property02", "some_value02", "some_type02");
    }

    @Test
    public void testGetElementsAsMessagePropertiesChild() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        properties.add(new Property("some_property02", "some_value02", "some_type02"));
        OMElement mpElement = MessagePropertiesElement.createElement(umElement, properties);

        Iterator<OMElement> it = PropertyElement.getElements(mpElement);

        assertTrue(it.hasNext());
        OMElement pElem = it.next();
        TestUtils.checkPropertyElementContent(pElem, "some_property01", "some_value01", "some_type01");
        pElem = it.next();
        TestUtils.checkPropertyElementContent(pElem, "some_property02", "some_value02", "some_type02");
    }

    @Test
    public void testReadElementAsPartPropertiesChild() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        OMElement ppElement = PartPropertiesElement.createElement(umElement, properties);

        Property property =
                new Property("some_new_property01", "some_new_value01", "some_new_type01");
        OMElement pElement = PropertyElement.createElement(ppElement, property);
        property = PropertyElement.readElement(pElement);
        TestUtils.checkPropertyContent(property, "some_new_property01",
                "some_new_value01", "some_new_type01");
    }

    @Test
    public void testReadElementAsMessagePropertiesChild() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        OMElement mpElement = MessagePropertiesElement.createElement(umElement, properties);

        Property property =
                new Property("some_new_property01", "some_new_value01", "some_new_type01");
        OMElement pElement = PropertyElement.createElement(mpElement, property);

        property = PropertyElement.readElement(pElement);
        TestUtils.checkPropertyContent(property, "some_new_property01",
                "some_new_value01", "some_new_type01");
    }
}