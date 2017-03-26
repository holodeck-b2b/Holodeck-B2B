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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.*;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 15:15 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessageInfoElementTest {

    private OMElement umElement;

    @Before
    public void setUp() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);
        // Adding UserMessage from mmd
        umElement = UserMessageElement.createElement(headerBlock, mmd);
    }

    @Test
    public void testCreateElement() throws Exception {
        UserMessage userMessage = new UserMessage();
        OMElement miElement = MessageInfoElement.createElement(umElement, userMessage);

        assertNotNull(miElement);
        Iterator<OMElement> it = miElement.getChildElements();
        assertTrue(it.hasNext());
        OMElement tsElement = it.next();
        assertNotNull(tsElement);
        OMElement midElement = it.next();
        assertNotNull(midElement);

        // todo test all possible variants of MessageUnit implementations
        PullRequest pullRequest = new PullRequest();
        Receipt receipt = new Receipt();
        ErrorMessage errorMessage = new ErrorMessage();
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement miElement = MessageInfoElement.getElement(umElement);

        assertNotNull(miElement);
        Iterator<OMElement> it = miElement.getChildElements();
        assertTrue(it.hasNext());
        OMElement tsElement = it.next();
        assertNotNull(tsElement);
        OMElement midElement = it.next();
        assertNotNull(midElement);
    }

    @Test
    public void testReadElement() throws Exception {
        UserMessage userMessage = new UserMessage();
        userMessage.setTimestamp(new Date());
        userMessage.setMessageId("some_id");
        OMElement miElement = MessageInfoElement.createElement(umElement, userMessage);

        userMessage = new UserMessage();
        assertNull(userMessage.getTimestamp());
        assertNull(userMessage.getMessageId());
        MessageInfoElement.readElement(miElement, userMessage);

        assertNotNull(userMessage.getTimestamp());
        assertEquals("some_id", userMessage.getMessageId());
    }
}