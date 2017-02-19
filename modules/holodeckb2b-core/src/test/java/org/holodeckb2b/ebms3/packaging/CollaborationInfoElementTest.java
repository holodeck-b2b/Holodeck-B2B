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
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created at 23:17 29.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CollaborationInfoElementTest {

    private static final QName COLLABORATION_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "CollaborationInfo");
    private static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");
    private static final QName SERVICE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Service");
    private static final QName ACTION_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Action");
    private static final QName CONVERSATIONID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ConversationId");

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
        CollaborationInfo ciData = new CollaborationInfo();
        ciData.setAgreement(new AgreementReference());
        ciData.setService(new Service());
        ciData.setAction("some action");
        ciData.setConversationId("conv id");
        OMElement ciElement =
                CollaborationInfoElement.createElement(umElement, ciData);
        assertNotNull(ciElement);
        checkContainsInnerElements(ciElement);
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement ciElement = CollaborationInfoElement.getElement(umElement);
        assertNotNull(ciElement);
        checkContainsInnerElements(ciElement);
    }

    @Test
    public void testReadElement() throws Exception {
        CollaborationInfo ciData = new CollaborationInfo();
        AgreementReference agreementReference = new AgreementReference();
        String pmodeId = "some pmode id";
        agreementReference.setPModeId(pmodeId);
        ciData.setAgreement(agreementReference);
        ciData.setService(new Service());
        OMElement ciElement =
                CollaborationInfoElement.createElement(umElement, ciData);

        CollaborationInfo collaborationInfo =
                CollaborationInfoElement.readElement(ciElement);
        checkContainsData(collaborationInfo, pmodeId);
    }

    private void checkContainsInnerElements(OMElement ciElement) {
        assertEquals(COLLABORATION_INFO_ELEMENT_NAME, ciElement.getQName());
        OMElement arElement = AgreementRefElement.getElement(ciElement);
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, arElement.getQName());
        OMElement sElement = ServiceElement.getElement(ciElement);
        assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());

        assertNotNull(ciElement.getChildrenWithName(ACTION_ELEMENT_NAME).next());
        assertNotNull(ciElement.getChildrenWithName(
                CONVERSATIONID_ELEMENT_NAME).next());
    }

    private void checkContainsData(CollaborationInfo collaborationInfo,
                                   String pmodeId) {
        AgreementReference agreementReference =
                collaborationInfo.getAgreement();
        assertNotNull(agreementReference);
        assertEquals(pmodeId, agreementReference.getPModeId());
        assertNotNull(collaborationInfo.getService());
        assertNotNull(collaborationInfo.getAction());
        assertNotNull(collaborationInfo.getConversationId());
    }
}