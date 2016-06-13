/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.multihop;

import java.io.File;
import java.util.Iterator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Test if correct ebint:RoutingInput element is created
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class RoutingInputTest {
    
    public RoutingInputTest() {
    }

    @Test
    public void testFullUserMessageHeader() {
        // Use filled mmd document for testing
        String mmdPath = this.getClass().getClassLoader().getResource("multihop/ri/full_mmd.xml").getPath();
        File   f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        
        // Create a SOAP envelope that should contain the RoutingInput element 
        SOAPEnvelope    env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        
        OMElement ri = RoutingInput.createElement(env, mmd);
        
        assertEquals(MultiHopConstants.ROUTING_INPUT_NS_URI, ri.getNamespaceURI());
        Iterator umChilds = ri.getChildrenWithLocalName("UserMessage");
        assertTrue(umChilds.hasNext());
        OMElement umChild = (OMElement) umChilds.next();
        assertEquals(MultiHopConstants.ROUTING_INPUT_NS_URI, umChild.getNamespaceURI());
        
        Iterator ciChilds = umChild.getChildrenWithLocalName("CollaborationInfo");
        assertTrue(ciChilds.hasNext());
        OMElement ciChild = (OMElement) ciChilds.next();
        assertEquals(EbMSConstants.EBMS3_NS_URI, ciChild.getNamespaceURI());
    }
    
}
