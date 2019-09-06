/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
 * @author Bram Bakx (bram at holodeck-b2b.org)
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class AgreementTest extends AbstractBaseTest<Agreement> {

    @Test
    public void testReadComplete() {
        final Agreement agreement = createObject(
        		"<tns:Agreement xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
        		" 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
        		"    <tns:type>type4</tns:type>" + 
        		"    <tns:name>name2</tns:name>" + 
        		"</tns:Agreement>");
        assertNotNull(agreement);

        assertEquals("type4", agreement.getType());
        assertEquals("name2", agreement.getName());
    }

    @Test
    public void testReadNameOnly() {
    	final Agreement agreement = createObject(
				"<Agreement xmlns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
						" 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
						"    <name>name2</name>" + 
				"</Agreement>");
		assertNotNull(agreement);		
		assertNull(agreement.getType());
		assertEquals("name2", agreement.getName());

		final Agreement agreement2 = createObject(
				"<tns:Agreement xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
						" 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
						"<tns:type/>" +
						"<tns:name>anotherName</tns:name>" + 
				"</tns:Agreement>");
		assertNotNull(agreement2);		
		assertNull(agreement2.getType());
		assertEquals("anotherName", agreement2.getName());
    }
    
    @Test
    public void testReadNoName() {
    	final Agreement agreement = createObject(
    			"<tns:Agreement xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
    					" 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
    					"    <tns:type>type4</tns:type>" + 
    			"</tns:Agreement>");
    	
    	assertNull(agreement);
    }
        
    @Test
    public void testWriteComplete() {
        final Agreement agreement = new Agreement("name5", "http://holodeck-b2b.org/test/agreement");
        final String xml = createXML(agreement);
        
        assertFalse(Utils.isNullOrEmpty(xml));

        assertTrue(countElements(xml, "name") == 1);
        assertEquals(agreement.getName(), findElements(xml, "name").get(0).getTextContent());
        assertTrue(countElements(xml, "type") == 1);
        assertEquals(agreement.getType(), findElements(xml, "type").get(0).getTextContent());
    }

    @Test
    public void writeNameOnly() {
        final Agreement agreement = new Agreement("name7");
        final String xml = createXML(agreement);
        
        assertFalse(Utils.isNullOrEmpty(xml));

        assertTrue(countElements(xml, "name") == 1);
        assertEquals(agreement.getName(), findElements(xml, "name").get(0).getTextContent());
        assertTrue(countElements(xml, "type") == 0);
    }
    
    @Test
    public void testWriteNoName() {
        final Agreement agreement = new Agreement();
        final String xml = createXML(agreement);
        
        assertTrue(Utils.isNullOrEmpty(xml));

        final Agreement agreement2 = new Agreement();
        agreement2.setType("illegal");
        final String xml2 = createXML(agreement2);
        
        assertTrue(Utils.isNullOrEmpty(xml2));        
    }
    
    @Test
    public void testCopy() {
    	final Agreement source = new Agreement("someOtherAgreement", "org:holodeckb2b:test:refs");
    	final Agreement copy = new Agreement(source);
    	
    	assertEquals(source.getName(), copy.getName());
    	assertEquals(source.getType(), copy.getType());

    	final Agreement source2 = new Agreement();
    	source2.setName("someOtherAgreement");
    	final Agreement copy2 = new Agreement(source2);
    	
    	assertEquals(source2.getName(), copy2.getName());
    	assertNull(copy2.getType());    	

    	final Agreement source3 = new Agreement();
    	source3.setType("org:holodeckb2b:test:refs");
    	final Agreement copy3 = new Agreement(source3);
    	
    	assertNull(copy3.getName());    	
    	assertEquals(source3.getType(), copy3.getType());
    }
}
