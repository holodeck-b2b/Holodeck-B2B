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
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created at 15:17 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PullRequestElementTest {

    private static final QName SIGNAL_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "SignalMessage");
    private static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    private static final QName TIMESTAMP_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Timestamp");
    private static final QName MESSAGE_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");
    private static final QName PULLREQUEST_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "PullRequest");

    private SOAPHeaderBlock headerBlock;

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
//        System.out.println("headerBlock: " + headerBlock);
    }

    @Test
    public void testCreateElement() throws Exception {
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);

        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, prSignalElement.getQName());
        OMElement miElement = prSignalElement.getFirstElement();
        assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());
        Iterator it = miElement.getChildrenWithName(TIMESTAMP_ELEMENT_NAME);
        assertTrue(it.hasNext());
        it = miElement.getChildrenWithName(MESSAGE_ID_ELEMENT_NAME);
        assertTrue(it.hasNext());
        it = prSignalElement.getChildrenWithName(PULLREQUEST_ELEMENT_NAME);
        assertTrue(it.hasNext());
        OMElement pr = (OMElement)it.next();
        assertEquals("some_mpc", pr.getAttributeValue(new QName("mpc")));
    }

    @Test
    public void testReadElement() throws Exception {
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        assertEquals("some_mpc", pullRequest.getMPC());
        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);
        Iterator it = prSignalElement.getChildrenWithName(PULLREQUEST_ELEMENT_NAME);
        assertTrue(it.hasNext());
        OMElement prElement = (OMElement)it.next();
        assertEquals("some_mpc", prElement.getAttributeValue(new QName("mpc")));

        pullRequest = PullRequestElement.readElement(prElement);
        assertEquals("some_mpc", pullRequest.getMPC());
    }

    @Test
    public void testGetElement() throws Exception {
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        PullRequestElement.createElement(headerBlock, pullRequest);
        OMElement prElement = PullRequestElement.getElement(headerBlock);
        assertEquals(PULLREQUEST_ELEMENT_NAME, prElement.getQName());
        assertEquals("some_mpc", prElement.getAttributeValue(new QName("mpc")));
    }
}