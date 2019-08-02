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
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.junit.Before;
import org.junit.Test;

/**
 * Created at 23:19 29.01.17
 *
 * Checked for cases coverage (29.04.2017)
 *
 * todo There are many different IEbmsErrors
 * todo (file:///Users/timur/IdeaProjects/freedom/mockertim/Holodeck-B2B/javadoc/index.html)
 * todo Maybe it's worth to test all of them ?
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ErrorSignalElementTest {

    private static final QName SIGNAL_MESSAGE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "SignalMessage");

    static final QName  ERROR_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Error");

    static final QName  Q_ERROR_DETAIL =
            new QName(EbMSConstants.EBMS3_NS_URI, "ErrorDetail");

    static final QName  ERROR_CODE_ATTR_NAME = new QName("errorCode");
    static final QName  SEVERITY_ATTR_NAME = new QName("severity");

    static final QName CATEGORY_ATTR_NAME = new QName("category");
    static final QName REF_TO_ATTR_NAME = new QName("refToMessageInError");
    static final QName ORIGIN_ATTR_NAME = new QName("origin");
    static final QName SHORT_DESCR_ATTR_NAME = new QName("shortDescription");

    private static final QName DESCRIPTION_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Description");
    private static final QName LANG_ATTR_NAME =
            new QName("http://www.w3.org/XML/1998/namespace", "lang");

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
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);

        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, esElement.getQName());
        OMElement eElement =
                (OMElement)esElement.getChildrenWithName(ERROR_ELEMENT_NAME).next();
        assertEquals(ERROR_ELEMENT_NAME, eElement.getQName());
        assertEquals(IEbmsError.Severity.failure.toString(),
                eElement.getAttributeValue(SEVERITY_ATTR_NAME));
        assertEquals("some_error_code",
                eElement.getAttributeValue(ERROR_CODE_ATTR_NAME));
    }

    @Test
    public void testReadElement() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setCategory("");
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);

        errorMessage = ErrorSignalElement.readElement(esElement);

        assertNotNull(errorMessage);
        Iterator<IEbmsError> it = errorMessage.getErrors().iterator();
        assertTrue(it.hasNext());
        ebmsError = (EbmsError)it.next();
        assertEquals("some_error_code", ebmsError.getErrorCode());
        assertEquals(IEbmsError.Severity.failure, ebmsError.getSeverity());
    }

    @Test
    public void testGetElements() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setCategory("");
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        ErrorSignalElement.createElement(headerBlock, errorMessage);

        Iterator<OMElement> it = ErrorSignalElement.getElements(headerBlock);
        assertTrue(it.hasNext());
        OMElement esElement = it.next();

        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, esElement.getQName());
        OMElement eElement =
                (OMElement)esElement.getChildrenWithName(ERROR_ELEMENT_NAME).next();
        assertEquals(ERROR_ELEMENT_NAME, eElement.getQName());
        assertEquals(IEbmsError.Severity.failure.toString(),
                eElement.getAttributeValue(SEVERITY_ATTR_NAME));
        assertEquals("some_error_code",
                eElement.getAttributeValue(ERROR_CODE_ATTR_NAME));
    }

    @Test
    public void testCreateErrorElement() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        // Set required attributes
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");

        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);

        EbmsError newEbmsError = new EbmsError();
        // Set required attributes
        newEbmsError.setSeverity(IEbmsError.Severity.failure);
        newEbmsError.setErrorCode("some_new_error_code");
        // Set optonal attributes
        newEbmsError.setCategory("some_category");
        newEbmsError.setOrigin("some_error_origin");
        newEbmsError.setMessage("some_message");
        newEbmsError.setRefToMessageInError("ref_to_some_message");
        newEbmsError.setErrorDetail("some_error_detail");
        Description description = new Description("some_text", "en-CA");
        newEbmsError.setDescription(description);

        OMElement eElement =
                ErrorSignalElement.createErrorElement(esElement, newEbmsError);

        System.out.println("eElement: " + eElement);

        assertEquals(ERROR_ELEMENT_NAME, eElement.getQName());

        assertEquals(IEbmsError.Severity.failure.toString(),
                eElement.getAttributeValue(SEVERITY_ATTR_NAME));
        assertEquals("some_new_error_code",
                eElement.getAttributeValue(ERROR_CODE_ATTR_NAME));

        assertEquals("some_category",
                eElement.getAttributeValue(CATEGORY_ATTR_NAME));
        assertEquals("some_error_origin",
                eElement.getAttributeValue(ORIGIN_ATTR_NAME));
        assertEquals("some_message",
                eElement.getAttributeValue(SHORT_DESCR_ATTR_NAME));
        assertEquals("ref_to_some_message",
                eElement.getAttributeValue(REF_TO_ATTR_NAME));
        OMElement errDetailElement = eElement.getFirstElement();
        assertEquals(Q_ERROR_DETAIL, errDetailElement.getQName());
        assertEquals("some_error_detail", errDetailElement.getText());
        OMElement dElement = DescriptionElement.getElement(eElement);
        assertEquals(DESCRIPTION_ELEMENT_NAME, dElement.getQName());
        assertEquals("some_text", dElement.getText());
        assertEquals("en-CA",
                dElement.getAttributeValue(LANG_ATTR_NAME));
    }

    @Test
    public void testReadErrorElement() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        // Required attributes
        ebmsError.setSeverity(IEbmsError.Severity.failure);
        ebmsError.setErrorCode("some_error_code");
        // Optional attributes
        ebmsError.setCategory("some_category");
        ebmsError.setOrigin("some_error_origin");
        ebmsError.setMessage("some_message");
        ebmsError.setRefToMessageInError("ref_to_some_message");
        ebmsError.setErrorDetail("some_error_detail");
        Description description = new Description("some_text", "en-CA");
        ebmsError.setDescription(description);

        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);
        OMElement eElement =
                (OMElement) esElement.getChildrenWithName(ERROR_ELEMENT_NAME).next();

        ebmsError = ErrorSignalElement.readErrorElement(eElement);

        assertEquals(IEbmsError.Severity.failure, ebmsError.getSeverity());
        assertEquals("some_error_code", ebmsError.getErrorCode());

        assertEquals("some_category", ebmsError.getCategory());
        assertEquals("some_error_origin", ebmsError.getOrigin());
        assertEquals("some_message", ebmsError.getMessage());
        assertEquals("ref_to_some_message", ebmsError.getRefToMessageInError());
        assertEquals("some_error_detail", ebmsError.getErrorDetail());
        assertEquals("some_text", ebmsError.getDescription().getText());
        assertEquals("en-CA", ebmsError.getDescription().getLanguage());
    }
}
