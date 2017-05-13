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
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created at 23:18 29.01.17
 *
 * Checked for cases coverage (25.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class AgreementRefElementTest {

    private static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");

    private OMElement umElement;
    private OMElement ciElement;

    @Before
    public void setUp() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);
        // Adding UserMessage from mmd
        umElement = UserMessageElement.createElement(headerBlock, mmd);

        ciElement = CollaborationInfoElement.getElement(umElement);
    }

    @Test
    public void testCreateElement() throws Exception {
        String name = "agreement_name";
        String type = "agreement_type";
        String pmodeId = "some pmode id string";
        AgreementReference agreementReference =
                new AgreementReference(name, type, pmodeId);

        OMElement agreementRefElement =
                AgreementRefElement.createElement(ciElement, agreementReference);
        checkContainsInnerElements(agreementRefElement);
    }

    @Test
    public void testGetElement() throws Exception {
        OMElement agreementRefElement =
                AgreementRefElement.getElement(ciElement);
        checkContainsInnerElements(agreementRefElement);
    }

    @Test
    public void testReadElement() throws Exception {
        String name = "agreement_name";
        String type = "agreement_type";
        String pmodeId = "some pmode id string";
        AgreementReference agreementReference =
                new AgreementReference(name, type, pmodeId);

        OMElement agreementRefElement =
                AgreementRefElement.createElement(ciElement, agreementReference);

        agreementReference =
                AgreementRefElement.readElement(agreementRefElement);
        checkContainsData(agreementReference, name, type, pmodeId);
    }

    private void checkContainsInnerElements(OMElement arElement) {
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, arElement.getQName());
    }

    private void checkContainsData(AgreementReference agreementReference,
                                   String name, String type, String pmodeId) {
        assertNotNull(agreementReference);
        assertEquals(name, agreementReference.getName());
        assertEquals(type, agreementReference.getType());
        assertEquals(pmodeId, agreementReference.getPModeId());
    }
}