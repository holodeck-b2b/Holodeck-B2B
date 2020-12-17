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
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.junit.Test;

/**
 * Created at 17:41 29.01.17
 *
 * Checked for cases coverage (27.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PayloadInfoElementTest extends AbstractPackagingTest {

    private static final QName PAYLOAD_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PayloadInfo");
    private static final QName PART_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");

    @Test
    public void testCreateElement() throws Exception {
    	Collection<IPayload> payloads = new ArrayList<>();
    	Payload p = new Payload();
    	p.setPayloadURI("cid:as_attachment");
    	p.addProperty(new Property("p1", "v1"));
    	payloads.add(p);
    	
    	OMElement piElement = PayloadInfoElement.createElement(createParent(), payloads);
    	
        assertNotNull(piElement);
        assertEquals(PAYLOAD_INFO_ELEMENT_NAME, piElement.getQName());
        Iterator it = piElement.getChildrenWithName(PART_INFO_ELEMENT_NAME);
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());        
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement piElement = PayloadInfoElement.getElement(createXML(        		
        		"<parent>" +
        		"<eb3:PayloadInfo " +
        		"	xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
        		"   <eb3:PartInfo href=\"cid:8fba80a5-2a4b-42c4-9c8f-f53207d21673-142514300@gecko.fritz.box\"/>" + 
        		"</eb3:PayloadInfo>" +
        		"</parent>"));
        
        assertNotNull(piElement);
        assertEquals(PAYLOAD_INFO_ELEMENT_NAME, piElement.getQName());
    }

    @Test
    public void testReadElement() throws Exception {
    	Payload p1 = new Payload();
    	p1.setPayloadURI("cid:first_attachment");
    	Payload p2 = new Payload();
    	p2.setPayloadURI("#second_attachment");
    			
        Collection<IPayload> payloads = PayloadInfoElement.readElement(createXML(
        		"<eb3:PayloadInfo " +
            	"	xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">" +
            	"   <eb3:PartInfo href=\"" + p1.getPayloadURI() + "\"/>" + 
            	"   <eb3:PartInfo href=\"" + p2.getPayloadURI() + "\"/>" + 
            	"</eb3:PayloadInfo>"
                ));
        
        assertFalse(Utils.isNullOrEmpty(payloads));
        Iterator<IPayload> payloadsIt = payloads.iterator();
        assertTrue(payloadsIt.hasNext());
        IPayload p = payloadsIt.next();
        assertEquals(IPayload.Containment.ATTACHMENT, p.getContainment());
        assertTrue(payloadsIt.hasNext());
        p = payloadsIt.next();
        assertEquals(IPayload.Containment.BODY, p.getContainment());
        assertFalse(payloadsIt.hasNext());
    }
}