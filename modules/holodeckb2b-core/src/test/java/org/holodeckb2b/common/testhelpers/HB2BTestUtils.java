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
package org.holodeckb2b.common.testhelpers;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Created at 15:22 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class HB2BTestUtils {

    public static PMode create1WaySendPushPMode() {
    	PMode pmode = new PMode();
    	pmode.setId(UUID.randomUUID().toString());
    	pmode.setMep(EbMSConstants.ONE_WAY_MEP);
    	pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
    	
        Leg leg = new Leg();
        pmode.addLeg(leg);
        
        Protocol prot = new Protocol();
        prot.setAddress("http://goes.no.where/msh");
        leg.setProtocol(prot);
        
        return pmode;
    }
    
    public static PMode create1WayReceivePushPMode() {
    	PMode pmode = new PMode();
    	pmode.setId(UUID.randomUUID().toString());
    	pmode.setMep(EbMSConstants.ONE_WAY_MEP);
    	pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
    	
    	Leg leg = new Leg();
    	pmode.addLeg(leg);
    	
    	return pmode;
    }
    
    /**
     *
     * @param elem
     * @param name
     * @param text
     * @param type
     */
    public static void checkPropertyElementContent(OMElement elem, String name,
                                             String text, String type) {
        assertEquals(PROPERTY_ELEMENT_NAME, elem.getQName());
        assertEquals(name, elem.getAttributeValue(new QName("name")));
        assertEquals(text, elem.getText());
        // todo see PropertyElement.createElement() method implementation
        //assertEquals(type, elem.getAttributeValue(new QName("type"))); //fail
    }

    /**
     *
     * @param p
     * @param name
     * @param value
     * @param type
     */
    public static void checkPropertyContent(IProperty p, String name,
                                      String value, String type) {
        assertEquals(name, p.getName());
        assertEquals(value, p.getValue());
        assertEquals(type, p.getType());
    }

    private static final QName PROPERTY_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Property");    
}
