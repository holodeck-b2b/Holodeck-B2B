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

import org.holodeckb2b.common.util.Utils;
import org.junit.Test;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class DeliveryConfigurationTest extends AbstractBaseTest<DeliveryConfiguration> {

	@Test
	public void readComplete() {
		final DeliveryConfiguration dc = createObject(
				"<DefaultDelivery xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 				  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"   <DeliveryMethod>org.holodeckb2b.test.FakeVDelivery</DeliveryMethod>" +
				"	<Parameter>" +
				"		<name>param-1</name>" +
				"		<value>setting1</value>" +
				"	</Parameter>" +
				"	<Parameter>" +
				"		<name>param2-2</name>" +
				"		<value>http://someurl.holodeck-b2b.org</value>" +
				"	</Parameter>" +
				"</DefaultDelivery>");
		
		assertNotNull(dc);
		assertEquals("org.holodeckb2b.test.FakeVDelivery", dc.getFactory());
		assertFalse(Utils.isNullOrEmpty(dc.getSettings()));
		assertTrue(dc.getSettings().size() == 2);
	}
	
	@Test
	public void readFactoryOnly() {
		final DeliveryConfiguration dc = createObject(
				"<DefaultDelivery xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 				  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"   <DeliveryMethod>org.holodeckb2b.test.FakeVDelivery</DeliveryMethod>" +
				"</DefaultDelivery>");
		
		assertNotNull(dc);
		assertEquals("org.holodeckb2b.test.FakeVDelivery", dc.getFactory());
		assertTrue(Utils.isNullOrEmpty(dc.getSettings()));		
	}
	
	@Test
	public void readNoFactory() {
		final DeliveryConfiguration dc = createObject(
				"<DefaultDelivery xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 				  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"	<Parameter>" +
				"		<name>param-1</name>" +
				"		<value>setting1</value>" +
				"	</Parameter>" +
				"</DefaultDelivery>");
		
		assertNull(dc);
	}	
	
	@Test
	public void writeComplete() {
		final DeliveryConfiguration dc = new DeliveryConfiguration();
		dc.setId("useless-id");
		dc.setFactory("org.holodeckb2b.nothing.NoRealClass");
		dc.addSetting("param-1", "configuresSomething");
		dc.addSetting("param-2", "configuresSomethingElse");
		
		final String xml = createXML(dc);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "DeliveryMethod") == 1);
		assertEquals(dc.getFactory(), findElements(xml, "DeliveryMethod").get(0).getTextContent());
		assertTrue(countElements(xml, "Parameter") == 2);		
	}

	
	@Test
	public void writeFactoryOnly() {
		final DeliveryConfiguration dc = new DeliveryConfiguration();
		dc.setId("useless-id");
		dc.setFactory("org.holodeckb2b.nothing.NoRealClass");
		
		final String xml = createXML(dc);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "DeliveryMethod") == 1);
		assertEquals(dc.getFactory(), findElements(xml, "DeliveryMethod").get(0).getTextContent());
		assertTrue(countElements(xml, "Parameter") == 0);		
	}
	
	@Test
	public void writeNoFactory() {
		final DeliveryConfiguration dc = new DeliveryConfiguration();
		dc.setId("useless-id");
		dc.addSetting("param-1", "configuresSomething");
		dc.addSetting("param-2", "configuresSomethingElse");
		
		final String xml = createXML(dc);
		
		assertTrue(Utils.isNullOrEmpty(xml));
	}
	
	@Test
	public void testCopy() {
		final DeliveryConfiguration source = new DeliveryConfiguration();
		source.setId("some-id");
		source.setFactory("org.holodeckb2b.nothing.NoRealClass");
		source.addSetting("param-1", "configuresSomething");
		source.addSetting("param-2", "configuresSomethingElse");
		
		final DeliveryConfiguration copy = new DeliveryConfiguration(source);
		
		assertEquals(source.getId(), copy.getId());
		assertEquals(source.getFactory(), copy.getFactory());
		assertNotNull(copy.getSettings());
		assertTrue(copy.getSettings().size() == 2);
	}
}
