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
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 15:18 19.02.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ServiceElementTest extends AbstractPackagingTest {

    private static final QName SERVICE_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Service");

    @Test
    public void testCreateElementWithTypeAttr() throws Exception {
    	Service service = new Service("some_name", "some_type");

        OMElement sElement = ServiceElement.createElement(createParent(), service);

        assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());
        assertEquals("some_type", sElement.getAttributeValue(new QName("type")));
        assertEquals("some_name", sElement.getText());
    }

    @Test
    public void testCreateElementNoTypeAttr() throws Exception {
    	Service service = new Service("some_name");
    	
    	OMElement sElement = ServiceElement.createElement(createParent(), service);
    	
    	assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());
    	assertNull(sElement.getAttributeValue(new QName("type")));
    	assertEquals("some_name", sElement.getText());
    }
    
    @Test
    public void testGetElement() throws Exception {
        OMElement sElement = ServiceElement.getElement(createXML(
        		"<parent>" +
				"	<eb3:Service type=\"org:holodeckb2b:services\"" +
				"        xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" + 
				"PackagingTest</eb3:Service>\n" + 
        		"</parent>"));

        assertNotNull(sElement);
        assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());
    }

    @Test
    public void testReadElementWithTypeAttr() throws Exception {
    	String svcType = "org:holodeckb2b:services:test";
    	String svcName = "Packaging";
    	
    	Service svc = ServiceElement.readElement(createXML(
				"	<eb3:Service type=\"" + svcType + "\"" +
				"        xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
				svcName + "</eb3:Service>"));
    	
    	assertNotNull(svc);
    	assertEquals(svcType, svc.getType());
    	assertEquals(svcName, svc.getName());
    }
    
    @Test
    public void testReadElementNoTypeAttr() throws Exception {
    	String svcName = "Packaging";
    	
    	Service svc = ServiceElement.readElement(createXML(
				"	<eb3:Service xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
				svcName + "</eb3:Service>"));
    	
    	assertNotNull(svc);
    	assertNull(svc.getType());
    	assertEquals(svcName, svc.getName());
    	
    	
    }
}