/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.holodeckb2b.common.pmode.BusinessInfo;
import org.holodeckb2b.common.pmode.Property;
import org.holodeckb2b.common.pmode.Service;
import org.holodeckb2b.common.util.Utils;
import org.junit.Test;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class BusinessInfoTest extends AbstractBaseTest<BusinessInfo> {

	@Test
	public void readComplete() {
		final BusinessInfo bi = createObject(
				"<BusinessInfo  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
				"    <Action>StoreMessage</Action>\n" +
				"    <Mpc>http://test.holodeck-b2b.org/mpc/other</Mpc>\n" +
				"    <Service>\n" + 
				"        <name>Examples</name>\n" + 
				"        <type>org:holodeckb2b:services</type>\n" + 
				"    </Service>\n" + 
				"    <Property>\n" + 
				"    	<name>msgP1</name>\n" + 
				"    	<value>some_Extra/Info</value>\n" + 
				"    </Property>\n" + 
				"    <Property>\n" + 
				"    	<name>msgP2</name>\n" + 
				"    	<value>some_more/Info</value>\n" + 
				"    </Property>\n" + 
				"</BusinessInfo>");
		
		assertNotNull(bi);		
		assertEquals("StoreMessage", bi.getAction());
		assertEquals("http://test.holodeck-b2b.org/mpc/other", bi.getMpc());		
		assertNotNull(bi.getService());
		assertNotNull(bi.getProperties());
		assertTrue(bi.getProperties().size() == 2);
	}
	
	@Test
	public void readEmpty() {
		final BusinessInfo bi = createObject
				("<BusinessInfo  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>"); 
		
		assertNotNull(bi);		
		assertNull(bi.getAction());
		assertNull(bi.getMpc());		
		assertNull(bi.getService());
		assertTrue(Utils.isNullOrEmpty(bi.getProperties()));
	}
	
	@Test
	public void readCActionOnly() {
		final BusinessInfo bi = createObject(
				"<BusinessInfo  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
				" 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
				"    <Action>StoreMessage</Action>\n" +
				"</BusinessInfo>");
		
		assertNotNull(bi);		
		assertEquals("StoreMessage", bi.getAction());
		assertNull(bi.getMpc());		
		assertNull(bi.getService());
		assertTrue(Utils.isNullOrEmpty(bi.getProperties()));
	}
	
	@Test
	public void readMPCOnly() {
		final BusinessInfo bi = createObject(
				"<BusinessInfo  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
				"    <Mpc>http://test.holodeck-b2b.org/mpc/other</Mpc>\n" +
				"</BusinessInfo>");
		
		assertNotNull(bi);		
		assertNull(bi.getAction());
		assertEquals("http://test.holodeck-b2b.org/mpc/other", bi.getMpc());		
		assertNull(bi.getService());
		assertTrue(Utils.isNullOrEmpty(bi.getProperties()));
	}

	@Test
	public void readServiceOnly() {
		final BusinessInfo bi = createObject(
				"<BusinessInfo  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
				"    <Service>\n" + 
				"        <name>Examples</name>\n" + 
				"        <type>org:holodeckb2b:services</type>\n" + 
				"    </Service>\n" + 
				"</BusinessInfo>");
		
		assertNotNull(bi);		
		assertNotNull(bi.getService());		
		assertNull(bi.getAction());
		assertNull(bi.getMpc());		
		assertTrue(Utils.isNullOrEmpty(bi.getProperties()));
	}
	
	@Test
	public void readPropertyOnly() {
		final BusinessInfo bi = createObject(
				"<BusinessInfo  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
				"    <Property>\n" + 
				"    	<name>msgP1</name>\n" + 
				"    	<value>some_Extra/Info</value>\n" + 
				"    </Property>\n" + 
				"</BusinessInfo>");
		
		assertNotNull(bi);		
		assertNull(bi.getAction());
		assertNull(bi.getMpc());		
		assertNull(bi.getService());
		assertFalse(Utils.isNullOrEmpty(bi.getProperties()));
	}
	
	@Test
	public void writeComplete() {
		final BusinessInfo bi = new BusinessInfo();
		bi.setAction("ExecuteService");
		bi.setMpc("http://test.holodeck-b2b.org/someMPC");
		bi.setService(new Service("RequestedSvc"));
		bi.addProperty(new Property("msgP1", "value1"));
		bi.addProperty(new Property("msgP2", "value2"));
		bi.addProperty(new Property("msgP3", "value3"));
		
		final String xml = createXML(bi);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "Action") == 1);
		assertEquals(bi.getAction(), findElements(xml, "Action").get(0).getTextContent());
		assertTrue(countElements(xml, "Mpc") == 1);
		assertEquals(bi.getMpc(), findElements(xml, "Mpc").get(0).getTextContent());
		assertTrue(countElements(xml, "Service") == 1);
		assertTrue(countElements(xml, "Property") == 3);		
	}
	
	@Test
	public void writeNoAction() {
		final BusinessInfo bi = new BusinessInfo();
		bi.setMpc("http://test.holodeck-b2b.org/someMPC");
		bi.setService(new Service("RequestedSvc"));
		bi.addProperty(new Property("msgP1", "value1"));
		bi.addProperty(new Property("msgP2", "value2"));
		bi.addProperty(new Property("msgP3", "value3"));
		
		final String xml = createXML(bi);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "Action") == 0);
		assertTrue(countElements(xml, "Mpc") == 1);
		assertTrue(countElements(xml, "Service") == 1);
		assertTrue(countElements(xml, "Property") == 3);		
	}
	
	@Test
	public void writeNoMpc() {
		final BusinessInfo bi = new BusinessInfo();
		bi.setAction("ExecuteService");
		bi.setService(new Service("RequestedSvc"));
		bi.addProperty(new Property("msgP1", "value1"));
		bi.addProperty(new Property("msgP2", "value2"));
		bi.addProperty(new Property("msgP3", "value3"));
		
		final String xml = createXML(bi);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "Action") == 1);
		assertTrue(countElements(xml, "Mpc") == 0);
		assertTrue(countElements(xml, "Service") == 1);
		assertTrue(countElements(xml, "Property") == 3);		
	}

	@Test
	public void writeNoService() {
		final BusinessInfo bi = new BusinessInfo();
		bi.setAction("ExecuteService");
		bi.setMpc("http://test.holodeck-b2b.org/someMPC");
		bi.addProperty(new Property("msgP1", "value1"));
		bi.addProperty(new Property("msgP2", "value2"));
		
		final String xml = createXML(bi);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "Action") == 1);
		assertTrue(countElements(xml, "Mpc") == 1);
		assertTrue(countElements(xml, "Service") == 0);
		assertTrue(countElements(xml, "Property") == 2);		
	}	

	@Test
	public void writeNoProps() {
		final BusinessInfo bi = new BusinessInfo();
		bi.setAction("ExecuteService");
		bi.setMpc("http://test.holodeck-b2b.org/someMPC");
		bi.setService(new Service("RequestedSvc"));
		
		final String xml = createXML(bi);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "Action") == 1);
		assertTrue(countElements(xml, "Mpc") == 1);
		assertTrue(countElements(xml, "Service") == 1);
		assertTrue(countElements(xml, "Property") == 0);		
	}	
	
	@Test
	public void testCopy()	{
		final BusinessInfo source = new BusinessInfo();
		source.setAction("ExecuteService");
		source.setMpc("http://test.holodeck-b2b.org/someMPC");
		source.setService(new Service("RequestedSvc"));
		source.addProperty(new Property("msgP1", "value1"));
		source.addProperty(new Property("msgP2", "value2"));
		source.addProperty(new Property("msgP3", "value3"));
		
		final BusinessInfo copy = new BusinessInfo(source);
		
		assertEquals(source.getAction(), copy.getAction());
		assertEquals(source.getMpc(), copy.getMpc());
		assertNotNull(copy.getService());
		assertEquals(source.getService().getName(), copy.getService().getName());
		assertFalse(Utils.isNullOrEmpty(copy.getProperties()));
		assertEquals(source.getProperties().size(), copy.getProperties().size());		
	}
}
