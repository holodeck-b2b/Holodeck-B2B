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
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 23:19 29.01.17
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

    static final QName  ERROR_CODE_ATTR_NAME =
            new QName("errorCode");
    static final QName  SEVERITY_ATTR_NAME =
            new QName("severity");

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
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);

        assertEquals(SIGNAL_MESSAGE_ELEMENT_NAME, esElement.getQName());
        OMElement eElement =
                (OMElement)esElement.getChildrenWithName(ERROR_ELEMENT_NAME).next();
        assertEquals(ERROR_ELEMENT_NAME, eElement.getQName());
        assertEquals(IEbmsError.Severity.FAILURE.toString(),
                eElement.getAttributeValue(SEVERITY_ATTR_NAME));
        assertEquals("some_error_code",
                eElement.getAttributeValue(ERROR_CODE_ATTR_NAME));
    }

    @Test
    public void testReadElement() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
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
        assertEquals(IEbmsError.Severity.FAILURE, ebmsError.getSeverity());
    }

    @Test
    public void testGetElements() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
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
        assertEquals(IEbmsError.Severity.FAILURE.toString(),
                eElement.getAttributeValue(SEVERITY_ATTR_NAME));
        assertEquals("some_error_code",
                eElement.getAttributeValue(ERROR_CODE_ATTR_NAME));
    }

    @Test
    public void testCreateErrorElement() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);

        EbmsError newEbmsError = new EbmsError();
        newEbmsError.setSeverity(IEbmsError.Severity.FAILURE);
        newEbmsError.setErrorCode("some_new_error_code");

        OMElement eElement =
                ErrorSignalElement.createErrorElement(esElement, newEbmsError);

        assertEquals(ERROR_ELEMENT_NAME, eElement.getQName());
        assertEquals(IEbmsError.Severity.FAILURE.toString(),
                eElement.getAttributeValue(SEVERITY_ATTR_NAME));
        assertEquals("some_new_error_code",
                eElement.getAttributeValue(ERROR_CODE_ATTR_NAME));
    }

    @Test
    public void testReadErrorElement() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage();
        ArrayList<IEbmsError> errors = new ArrayList<>();
        EbmsError ebmsError = new EbmsError();
        ebmsError.setSeverity(IEbmsError.Severity.FAILURE);
        ebmsError.setErrorCode("some_error_code");
        errors.add(ebmsError);
        errorMessage.setErrors(errors);

        OMElement esElement =
                ErrorSignalElement.createElement(headerBlock, errorMessage);
        OMElement eElement =
                (OMElement) esElement.getChildrenWithName(ERROR_ELEMENT_NAME).next();

        ebmsError = ErrorSignalElement.readErrorElement(eElement);
        assertEquals(IEbmsError.Severity.FAILURE, ebmsError.getSeverity());
        assertEquals("some_error_code", ebmsError.getErrorCode());
    }
}
