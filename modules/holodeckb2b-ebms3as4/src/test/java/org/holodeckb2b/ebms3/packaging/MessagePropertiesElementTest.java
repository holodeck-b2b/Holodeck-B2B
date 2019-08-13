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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.junit.Test;

/**
 * Created at 15:16 19.02.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessagePropertiesElementTest extends AbstractPackagingTest {

    private static final QName Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "MessageProperties");
    private static final QName Q_PROP_EL_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Property");

    @Test
    public void testCreateElement() throws Exception {
        ArrayList<IProperty> properties = new ArrayList<>();
        properties.add(new Property("some_property01", "some_value01", "some_type01"));
        properties.add(new Property("some_property02", "some_value02", "some_type02"));
        OMElement mpElement = MessagePropertiesElement.createElement(createParent(), properties);
        
        assertNotNull(mpElement);
        assertEquals(Q_ELEMENT_NAME, mpElement.getQName());
        Iterator it = mpElement.getChildrenWithName(Q_PROP_EL_NAME);
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());        
    }

    @Test
    public void testGetElement() throws Exception {
    	OMElement propElement = MessagePropertiesElement.getElement(createXML(
        		"<parent>" +
        		" <eb3:MessageProperties " +
        		"   xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
				"   <eb3:Property name=\"original-file-name\">simple_document.xml</eb3:Property>" +
				" </eb3:MessageProperties> " +        		
				"</parent>"));
    	
        assertNotNull(propElement);
        assertEquals(Q_ELEMENT_NAME, propElement.getQName());
    }

    @Test
    public void testReadElement() throws Exception {
        Collection<IProperty> readProperties = MessagePropertiesElement.readElement(createXML(
        		" <eb3:MessageProperties " +
        		"   xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
				"   <eb3:Property name=\"original-file-name\">simple_document.xml</eb3:Property>" +
				"   <eb3:Property name=\"reference_number\">XREF_4828</eb3:Property>" +
				" </eb3:MessageProperties>"));
        
        assertNotNull(readProperties);
        assertEquals(2, readProperties.size());    
    }
}