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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 23:16 29.01.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SchemaElementTest extends AbstractPackagingTest {

    private static final QName SCHEMA_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Schema");

    @Test
    public void testCreateElement() throws Exception {
        SchemaReference schema = new SchemaReference();
        schema.setLocation("somewhere");
        schema.setNamespace("namespace1");
        schema.setVersion("test");

        OMElement schemaElement = SchemaElement.createElement(createParent(), schema);
        assertNotNull(schemaElement);
        assertEquals(SCHEMA_ELEMENT_NAME, schemaElement.getQName());
        assertEquals(schema.getLocation(),
                schemaElement.getAttributeValue(new QName("location")));
        assertEquals(schema.getNamespace(),
                schemaElement.getAttributeValue(new QName("namespace")));
        assertEquals(schema.getVersion(),
                schemaElement.getAttributeValue(new QName("version")));
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement schemaElement = SchemaElement.getElement(createXML(
        		"<parent>"
        		+ "<eb3:Schema xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\""
        		+ " location=\"http://dev.holodeck-b2b.com/notreal/schema\"" 
        		+ " namespace=\"http://dev.holodeck-b2b.com/notreal/schema\"" 
        		+ " version=\"2.0\""
        		+ "/>"
				+ "</parent>"));

        assertNotNull(schemaElement);
        assertEquals(SCHEMA_ELEMENT_NAME, schemaElement.getQName());
    }

    @Test
    public void testReadElement() throws Exception {
    	String location = "http://dev.holodeck-b2b.com/notreal/schemafile";
    	String ns = "http://dev.holodeck-b2b.com/notreal/schema";
    	String version = "2.0";
    	
        SchemaReference schema = SchemaElement.readElement(createXML(
        		"<eb3:Schema xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\""
        		+ " location=\"" + location + "\"" 
        		+ " namespace=\"" + ns + "\"" 
        		+ " version=\"" + version + "\""
        		+ "/>"));
        
        assertNotNull(schema);
        assertEquals(location, schema.getLocation());
        assertEquals(ns, schema.getNamespace());
        assertEquals(version, schema.getVersion());
    }
}