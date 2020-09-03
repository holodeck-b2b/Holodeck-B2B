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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 23:18 29.01.17
 *
 * Checked for cases coverage (27.04.2017)
 *
 * We should test both use cases of the Description element
 * 1. inside Signal element
 * 2. inside PartInfo element
 * See DescriptionElement javadoc for details
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DescriptionElementTest extends AbstractPackagingTest {

    private static final QName Q_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Description");
    private static final QName LANG_ATTR_NAME =
            new QName("http://www.w3.org/XML/1998/namespace", "lang");

    @Test
    public void testCreateElementWithLang() {
    	Description description = new Description("Some descriptive text", "en-CA");
    	OMElement dElement = DescriptionElement.createElement(createParent(), description);
        
    	assertNotNull(dElement);
        assertEquals(Q_ELEMENT_NAME, dElement.getQName());
        assertEquals(description.getLanguage(), dElement.getAttributeValue(LANG_ATTR_NAME));
        assertEquals(description.getText(), dElement.getText());
    }
    
    @Test
    public void testCreateElementNoLang() {
    	Description description = new Description("Some descriptive text");
    	OMElement dElement = DescriptionElement.createElement(createParent(), description);
    	
    	assertNotNull(dElement);
    	assertEquals(Q_ELEMENT_NAME, dElement.getQName());
    	assertNull(description.getLanguage(), dElement.getAttributeValue(LANG_ATTR_NAME));
    	assertEquals(description.getText(), dElement.getText());
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement dElement = DescriptionElement.getElement(createXML(
        		"<parent>" +
        		" <eb3:Description xml:lang=\"en-US\"" +
        		"   xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
				"This is just a test" +
				"</eb3:Description> " +        		
				"</parent>"));

        assertNotNull(dElement);
        assertEquals(Q_ELEMENT_NAME, dElement.getQName());
    }

    @Test
    public void testReadElementWithLang() {
    	String text = "This is just a test";
    	String lang = "en-US";
    	
        Description descr = DescriptionElement.readElement(createXML(
        		" <eb3:Description xml:lang=\"" + lang + "\"" +
        		"   xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
				text +
				"</eb3:Description>"));        		

        assertNotNull(descr);
        assertEquals(text, descr.getText());
        assertEquals(lang, descr.getLanguage());    	
    }
    
    @Test
    public void testReadElementNoLang() {
    	String text = "This is just a test";
    	
    	Description descr = DescriptionElement.readElement(createXML(
    			" <eb3:Description" +
    					"   xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
    					text +
    			"</eb3:Description>"));        		
    	
    	assertNotNull(descr);
    	assertEquals(text, descr.getText());
    	assertNull(descr.getLanguage());    	
    }


}