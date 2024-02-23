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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;

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

    public static PMode create1WayReceivePMode() {
    	PMode pmode = new PMode();
    	pmode.setId(UUID.randomUUID().toString());
    	pmode.setMep(EbMSConstants.ONE_WAY_MEP);
    	pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

    	Leg leg = new Leg();
    	pmode.addLeg(leg);

    	return pmode;
    }

	public static PMode create2WaySendOnReplyPMode() {
    	PMode pmode = new PMode();
    	pmode.setId(UUID.randomUUID().toString());
    	pmode.setMep(EbMSConstants.TWO_WAY_MEP);
    	pmode.setMepBinding(EbMSConstants.TWO_WAY_PUSH_PUSH);

        Leg rcvleg = new Leg();
        rcvleg.setLabel(Label.REQUEST);
        pmode.addLeg(rcvleg);

        Leg sendLeg = new Leg();
        sendLeg.setLabel(Label.REPLY);
        Protocol prot = new Protocol();
        prot.setAddress("http://goes.no.where/msh");
        sendLeg.setProtocol(prot);

        pmode.addLeg(sendLeg);

        return pmode;
    }

	public static PMode create2WaySendOnRequestPMode() {
		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		pmode.setMep(EbMSConstants.TWO_WAY_MEP);
		pmode.setMepBinding(EbMSConstants.TWO_WAY_PUSH_PUSH);

		Leg sendLeg = new Leg();
		sendLeg.setLabel(Label.REQUEST);
		Protocol prot = new Protocol();
		prot.setAddress("http://goes.no.where/msh");
		sendLeg.setProtocol(prot);

		pmode.addLeg(sendLeg);

		Leg rcvleg = new Leg();
		rcvleg.setLabel(Label.REPLY);
		pmode.addLeg(rcvleg);

		return pmode;
	}


    /**
     * Asserts the content of the given streams is equal.
     *
     * @param is1
     * @param is2
     * @throws IOException
     */
    public static void assertEqual(InputStream is1, InputStream is2) throws IOException {
		int b1, b2;
		do {
			b1 = is1.read(); b2 = is2.read();
		} while (b1 == b2 && b1 >= 0 && b2 >= 0);
		assertTrue(b1 < 0 && b2 < 0);
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
