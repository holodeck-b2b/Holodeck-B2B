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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.SelectivePullRequest;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

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
    private static final QName S_REF_TO_MSG_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "RefToMessageId");
    private static final QName S_CONV_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ConversationId");
    private static final QName S_ACTION_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Action");

    private static final String T_REFD_MSG_ID = "test-01@holodeck-b2b.org";
    private static final String T_CONV_ID = "test-01@holodeck-b2b.org";
    private static final String T_AGREEMENT = "edelivery";
    private static final String T_SERVICE = "selectablePull";
    private static final String T_ACTION = "selector";

    private SOAPHeaderBlock headerBlock;

    @Before
    public void setUp() throws Exception {
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
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
    public void testCreateSelectivePullELement() throws Exception {
        SelectivePullRequest pullRequest = new SelectivePullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setReferencedMessageId(T_REFD_MSG_ID);
        pullRequest.setConversationId(T_CONV_ID);
        pullRequest.setAgreementRef(new AgreementReference(T_AGREEMENT, null, null));
        pullRequest.setService(new Service(T_SERVICE));
        pullRequest.setAction(T_ACTION);

        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);

        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, prSignalElement.getQName());
        OMElement miElement = prSignalElement.getFirstElement();
        assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());

        Iterator it = prSignalElement.getChildrenWithName(PULLREQUEST_ELEMENT_NAME);
        assertTrue(it.hasNext());
        OMElement pr = (OMElement)it.next();
        assertEquals("some_mpc", pr.getAttributeValue(new QName("mpc")));

        assertNotNull(pr.getFirstChildWithName(S_REF_TO_MSG_ID_ELEMENT_NAME));
        assertEquals(T_REFD_MSG_ID, pr.getFirstChildWithName(S_REF_TO_MSG_ID_ELEMENT_NAME).getText());
        assertNotNull(pr.getFirstChildWithName(S_CONV_ID_ELEMENT_NAME));
        assertEquals(T_CONV_ID, pr.getFirstChildWithName(S_CONV_ID_ELEMENT_NAME).getText());
        assertNotNull(AgreementRefElement.getElement(pr));
        assertEquals(T_AGREEMENT, AgreementRefElement.readElement(AgreementRefElement.getElement(pr)).getName());
        assertNotNull(ServiceElement.getElement(pr));
        assertEquals(T_SERVICE, ServiceElement.readElement(ServiceElement.getElement(pr)).getName());
        assertNotNull(pr.getFirstChildWithName(S_ACTION_ELEMENT_NAME));
        assertEquals(T_ACTION, pr.getFirstChildWithName(S_ACTION_ELEMENT_NAME).getText());
    }

    @Test
    public void testReadSelectivePullElement() throws Exception {
        SelectivePullRequest pullRequest = new SelectivePullRequest();
        pullRequest.setMPC("some_mpc");
        pullRequest.setReferencedMessageId(T_REFD_MSG_ID);
        pullRequest.setConversationId(T_CONV_ID);
        pullRequest.setAgreementRef(new AgreementReference(T_AGREEMENT, null, null));
        pullRequest.setService(new Service(T_SERVICE));
        pullRequest.setAction(T_ACTION);

        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);

        PullRequest readRequest = PullRequestElement.readElement((OMElement) prSignalElement.getChildrenWithName(PULLREQUEST_ELEMENT_NAME).next());
        assertTrue(readRequest instanceof SelectivePullRequest);

        pullRequest = (SelectivePullRequest) readRequest;
        assertEquals(T_REFD_MSG_ID, pullRequest.getReferencedMessageId());
        assertEquals(T_CONV_ID, pullRequest.getConversationId());
        assertNotNull(pullRequest.getAgreementRef());
        assertEquals(T_AGREEMENT, pullRequest.getAgreementRef().getName());
        assertNotNull(pullRequest.getService());
        assertEquals(T_SERVICE, pullRequest.getService().getName());
        assertEquals(T_ACTION, pullRequest.getAction());
    }

    @Test
    public void testReadElement() throws Exception {
        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        assertEquals("some_mpc", pullRequest.getMPC());
        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);

        pullRequest = PullRequestElement.readElement((OMElement) prSignalElement.getChildrenWithName(PULLREQUEST_ELEMENT_NAME).next());
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