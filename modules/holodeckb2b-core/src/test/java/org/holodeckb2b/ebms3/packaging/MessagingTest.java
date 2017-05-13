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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created at 13:12 15.10.16
 *
 * Checked for cases coverage (25.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessagingTest {

    @Test
    public void testCreateElement() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        Messaging.createElement(env);
        // Check if header contains Messaging header block with mustUnderstand=true
        SOAPHeader header = env.getHeader();
        ArrayList blocks =
                header.getHeaderBlocksWithNSURI(EbMSConstants.EBMS3_NS_URI);
        assertTrue(blocks.size()>0);
        assertTrue(((SOAPHeaderBlock) blocks.get(0)).getMustUnderstand());
    }

    @Test
    public void testGetElement() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock soapHeaderBlock = Messaging.createElement(env);

        SOAPHeaderBlock newSoapHeaderBlock = Messaging.getElement(env);
        assertEquals(soapHeaderBlock.getMustUnderstand(),
                newSoapHeaderBlock.getMustUnderstand());
        assertEquals(soapHeaderBlock.getRelay(), newSoapHeaderBlock.getRelay());
        assertEquals(soapHeaderBlock.getRole(), newSoapHeaderBlock.getRole());
        assertEquals(soapHeaderBlock.getVersion(),
                newSoapHeaderBlock.getVersion());
    }
}
