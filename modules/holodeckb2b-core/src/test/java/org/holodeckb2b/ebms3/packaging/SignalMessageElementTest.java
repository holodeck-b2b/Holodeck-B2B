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
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 15:18 19.02.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SignalMessageElementTest {

    private static final QName SIGNAL_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "SignalMessage");

    private SOAPHeaderBlock headerBlock;

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(env);
    }

    @Test
    public void testCreateElement() throws Exception {
        OMElement smElement = SignalMessageElement.createElement(headerBlock);
        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, smElement.getQName());
    }

    @Test
    public void testGetElements() throws Exception {
        SignalMessageElement.createElement(headerBlock);
        Iterator<OMElement> it = SignalMessageElement.getElements(headerBlock);
        assertTrue(it.hasNext());
        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, it.next().getQName());
    }
}