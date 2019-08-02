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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 15:15 19.02.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessageInfoElementTest {

    private MessageMetaData mmd;
    private SOAPEnvelope soapEnvelope;
    private SOAPHeaderBlock headerBlock;

    private static final QName MESSAGE_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageInfo");
    private static final QName TIMESTAMP_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Timestamp");
    private static final QName MESSAGE_ID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "MessageId");
    private static final QName RECEIPT_CHILD_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ReceiptChild");

    @Before
    public void setUp() throws Exception {
        mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Creating SOAP envelope
        soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        headerBlock = Messaging.createElement(soapEnvelope);
    }

    @Test
    public void testCreateElement() throws Exception {
        // Test MessageInfoElement.createElement on all possible variants
        // of the MessageUnit implementations

        // UserMessage

        UserMessage userMessage = new UserMessage();
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        OMElement miElement = MessageInfoElement.createElement(umElement, userMessage);
        verifyMessageInfoElement(miElement);

        // PullRequest

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);
        miElement = MessageInfoElement.createElement(prSignalElement, pullRequest);
        verifyMessageInfoElement(miElement);

        // Receipt

        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);
        miElement = MessageInfoElement.createElement(rElement, receipt);
        verifyMessageInfoElement(miElement);

        // ErrorMessage

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);
        miElement = MessageInfoElement.createElement(esElement, errorMessage);
        verifyMessageInfoElement(miElement);
    }

    @Test
    public void testGetElement() throws Exception {
        // Test MessageInfoElement.getElement on all possible variants
        // of the MessageUnit implementations

        // UserMessage

        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        OMElement miElement = MessageInfoElement.getElement(umElement);
        verifyMessageInfoElement(miElement);

        // PullRequest

        PullRequest pullRequest = new PullRequest();
        pullRequest.setMPC("some_mpc");
        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);
        miElement = MessageInfoElement.getElement(prSignalElement);
        verifyMessageInfoElement(miElement);

        // Receipt

        Receipt receipt = new Receipt();
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);
        miElement = MessageInfoElement.getElement(rElement);
        verifyMessageInfoElement(miElement);

        // ErrorMessage

        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);
        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);
        miElement = MessageInfoElement.getElement(esElement);
        verifyMessageInfoElement(miElement);
    }

    @Test
    public void testReadElement() throws Exception {
        // Test MessageInfoElement.readElement on all possible variants
        // of the MessageUnit implementations

        // UserMessage

        UserMessage userMessage = new UserMessage();
        userMessage.setTimestamp(new Date());
        userMessage.setMessageId("some_id");
        userMessage.setRefToMessageId("some_ref_id");
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);
        OMElement miElement = MessageInfoElement.createElement(umElement, userMessage);

        userMessage = new UserMessage();
        MessageInfoElement.readElement(miElement, userMessage);

        verifyMessageUnit(userMessage, "some_id", "some_ref_id");

        // PullRequest

        PullRequest pullRequest = new PullRequest();
        pullRequest.setTimestamp(new Date());
        pullRequest.setMessageId("some_id");
        pullRequest.setRefToMessageId("some_ref_id");
        OMElement prSignalElement =
                PullRequestElement.createElement(headerBlock, pullRequest);
        miElement = MessageInfoElement.createElement(prSignalElement, pullRequest);

        pullRequest = new PullRequest();
        MessageInfoElement.readElement(miElement, pullRequest);

        verifyMessageUnit(pullRequest, "some_id", "some_ref_id");

        // Receipt

        Receipt receipt = new Receipt();
        receipt.setTimestamp(new Date());
        receipt.setMessageId("some_id");
        receipt.setRefToMessageId("some_ref_id");
        OMElement receiptChildElement =
                soapEnvelope.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        ArrayList<OMElement> content = new ArrayList<>();
        content.add(receiptChildElement);
        receipt.setContent(content);
        OMElement rElement = ReceiptElement.createElement(headerBlock, receipt);
        miElement = MessageInfoElement.createElement(rElement, receipt);

        receipt = new Receipt();
        MessageInfoElement.readElement(miElement, receipt);

        verifyMessageUnit(receipt, "some_id", "some_ref_id");

        // ErrorMessage

        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setTimestamp(new Date());
        errorMessage.setMessageId("some_id");
        errorMessage.setRefToMessageId("some_ref_id");

        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);
        miElement = MessageInfoElement.createElement(esElement, errorMessage);

        errorMessage = new ErrorMessage();
        MessageInfoElement.readElement(miElement, errorMessage);

        verifyMessageUnit(errorMessage, "some_id", "some_ref_id");
    }

    /**
     * Verifies the structure of the MessageInfo OMElement
     * @param miElement
     */
    private void verifyMessageInfoElement(OMElement miElement) {
        assertEquals(MESSAGE_INFO_ELEMENT_NAME, miElement.getQName());
        Iterator<OMElement> it = miElement.getChildElements();
        assertTrue(it.hasNext());
        OMElement tsElement = it.next();
        assertEquals(TIMESTAMP_ELEMENT_NAME, tsElement.getQName());
        OMElement midElement = it.next();
        assertEquals(MESSAGE_ID_ELEMENT_NAME, midElement.getQName());
    }

    /**
     * Verifies that the MessageUnit fields are initialised
     * @param mu
     * @param muId
     */
    private void verifyMessageUnit(MessageUnit mu, String muId, String muRefId) {
        assertNotNull(mu.getTimestamp());
        assertEquals(muId, mu.getMessageId());
        assertEquals(muRefId, mu.getRefToMessageId());
    }
}