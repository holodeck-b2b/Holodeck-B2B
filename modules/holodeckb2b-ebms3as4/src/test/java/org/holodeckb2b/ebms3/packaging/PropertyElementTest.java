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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 15:16 19.02.17
 *
 * Checked for cases coverage (29.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PropertyElementTest extends AbstractPackagingTest {

    private static final QName Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Property");

    @Test
    public void testCreateElementWithType() throws Exception {
        Property property = new Property("some_new_property01", "some_new_value01", "some_new_type01");

        OMElement pElement = PropertyElement.createElement(createParent(), property);
        
        assertNotNull(pElement);
        assertEquals(Q_ELEMENT_NAME, pElement.getQName());
        assertEquals(property.getName(), pElement.getAttributeValue(new QName("name")));
        assertEquals(property.getType(), pElement.getAttributeValue(new QName("type")));
        assertEquals(property.getValue(), pElement.getText());
    }

    @Test
    public void testCreateElementNoType() throws Exception {
    	Property property = new Property("some_new_property01", "some_new_value01");
    	
    	OMElement pElement = PropertyElement.createElement(createParent(), property);
    	
    	assertNotNull(pElement);
    	assertEquals(Q_ELEMENT_NAME, pElement.getQName());
    	assertEquals(property.getName(), pElement.getAttributeValue(new QName("name")));
    	assertNull(pElement.getAttributeValue(new QName("type")));
    	assertEquals(property.getValue(), pElement.getText());
    }

    @Test
    public void testGetElements() throws Exception {
    	Iterator<OMElement> propElements = PropertyElement.getElements(createXML(
        		"<parent>" +
				"   <eb3:Property name=\"original-file-name\"" +
				    " xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" + 
				    "simple_document.xml</eb3:Property>" +
				"</parent>"));
    	
        assertNotNull(propElements);
        OMElement propElement = propElements.next();
        assertEquals(Q_ELEMENT_NAME, propElement.getQName());
        assertFalse(propElements.hasNext());
        
        propElements = PropertyElement.getElements(createXML(
        		"<parent" +
				" xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" + 
				"   <eb3:Property name=\"original-file-name\">simple_document.xml</eb3:Property>" +
				"   <eb3:Property name=\"reference_number\">XREF_4828</eb3:Property>" +
				"</parent>"));

        assertNotNull(propElements);
        propElement = propElements.next();
        assertEquals(Q_ELEMENT_NAME, propElement.getQName());
        assertTrue(propElements.hasNext());        
        propElement = propElements.next();
        assertEquals(Q_ELEMENT_NAME, propElement.getQName());
        assertFalse(propElements.hasNext());        
    }

    @Test
    public void testReadElementWithType() throws Exception {
    	String pName = "reference_number";
    	String pType = "CrossReferenceID";
    	String pValue = "XREF_4828";
    	
    	Property prop = PropertyElement.readElement(createXML(
				"<eb3:Property name=\"" + pName + "\" type=\"" + pType +"\"" +
				" xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" + 
				pValue + "</eb3:Property>" 
    			));
    			
        assertNotNull(prop);
        assertEquals(pName, prop.getName());
        assertEquals(pType, prop.getType());
        assertEquals(pValue, prop.getValue());
    }   
    
    @Test
    public void testReadElementNoType() throws Exception {
    	String pName = "reference_number";
    	String pValue = "XREF_4828";
    	
    	Property prop = PropertyElement.readElement(createXML(
    			"<eb3:Property name=\"" + pName + "\"" +
    					" xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" + 
    					pValue + "</eb3:Property>" 
    			));
    	
    	assertNotNull(prop);
    	assertEquals(pName, prop.getName());
    	assertNull(prop.getType());
    	assertEquals(pValue, prop.getValue());
    }   
}