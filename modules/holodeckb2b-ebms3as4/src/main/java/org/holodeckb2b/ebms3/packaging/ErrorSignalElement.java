/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Is a helper class for handling the ebMS Error Signal message units in the ebMS SOAP header. The Error Signal message
 * unit is a bit different from the other signal message units as it can contain multiple <code>eb:Error</code> elements
 * with one sibling <code>eb:MessageInfo</code> element under the parent <code>eb:SignalMessage</code> element.
 * <p>The Error signal message unit is specified in section 6.2 and 6.3 of the ebMS 3 Core specification.
 * <p>NOTE: The naming of this class differs from the other classes in this packages because <code>Error</code> is a
 * reserved class name.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ErrorSignalElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    public static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Error");

    /**
     * The fully qualified name of the ErrorDetail element as an {@link QName}
     */
    private static final QName  Q_ERROR_DETAIL = new QName(EbMSConstants.EBMS3_NS_URI, "ErrorDetail");

    /*
     * The local names of the attributes of the Error element
     */
    private static final String CATEGORY_ATTR = "category";
    private static final String REF_TO_ATTR = "refToMessageInError";
    private static final String ERROR_CODE_ATTR = "errorCode";
    private static final String ORIGIN_ATTR = "origin";
    private static final String SEVERITY_ATTR = "severity";
    private static final String SHORT_DESCR_ATTR = "shortDescription";

    /**
     * Creates a new <code>eb:SignalMessage</code> for an <i>Error Signal</i> message unit.
     *
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param errorMU       The information of the error signal to include
     * @return              The new element representing the error signal
     */
    public static OMElement createElement(final OMElement messaging, final IErrorMessage errorMU) {
        // First create the SignalMessage element that is the placeholder for
        // the Error elements containing the error info
        final OMElement signalmessage = SignalMessageElement.createElement(messaging);

        // Create the generic MessageInfo element
        MessageInfoElement.createElement(signalmessage, errorMU);

        // Now create an Error element for each error in the error message unit
        for(final IEbmsError error : errorMU.getErrors())
            createErrorElement(signalmessage, error);

        return signalmessage;
    }

    /**
     * Reads the information from a <code>eb:SignalMessage</code> and its child elements that contain the Error Signal
     * message unit and stores it a {@link ErrorMessage} object.
     *
     * @param sigElement    The parent <code>eb:SignalMessage</code> element that contains the <code>eb:Error</code>
     *                      and <code>MessageInfo</code> elements
     * @return              The {@link ErrorMessage} object containing the
     *                      information on the Error Signal
     */
    public static ErrorMessage readElement(final OMElement sigElement) {
        // Create a new EbmsError entity object to store the information in
        final ErrorMessage errData = new ErrorMessage();

        // First read general information from the MessageInfo child
        MessageInfoElement.readElement(MessageInfoElement.getElement(sigElement), errData);

        // Now get all child Error elements
        final Iterator<OMElement> it = sigElement.getChildrenWithName(Q_ELEMENT_NAME);
        while (it.hasNext())
            errData.addError(readErrorElement(it.next()));

        return errData;
    }

    /**
     * Gets an {@link Iterator} for all <code>eb:SignalMessage</code> elements from the given ebMS 3 Messaging header in
     * the SOAP message that represent <i>Error Signals</i>.
     *
     * @param   messaging  The SOAP Header block that contains the ebMS header,i.e. the <code>eb:Messaging</code> element
     * @return             An {@link Iterator} for all {@link OMElement}s representing a <code>eb:SignalMessage</code>
     *                     element that contains an Error Signal, i.e. has one or more <code>eb:Error</code> child
     *                     elements
     */
    public static Iterator<OMElement> getElements(final SOAPHeaderBlock messaging) {
        // Check all SignalMessage elements in the header
        final Iterator<OMElement> signals = SignalMessageElement.getElements(messaging);

        final ArrayList<OMElement>  errors = new ArrayList<>();
        while(signals.hasNext()) {
            final OMElement signal  = signals.next();
            // If this SignalMessage element has at least one Error child,
            //   it is an Error signal and should be returned
            if (signal.getFirstChildWithName(Q_ELEMENT_NAME) != null)
                errors.add(signal);
        }

        return errors.iterator();
    }

    /**
     * Helper method for creating an <code>eb:Error</code> element in the Error Signal message unit, i.e. the
     * <code>eb:SignalMessage</code> that contains  the errors.
     *
     * @param signalmessage     The {@link OMElement} parent object for the new <code>Error</code> element
     * @param error             The meta-data on the error to write to the element
     * @return                  An {@link OMElement} object representing the <code>eb:Error</code> element describing
     *                          the error
     */
    protected static OMElement createErrorElement(final OMElement signalmessage, final IEbmsError error) {
        final OMFactory f = signalmessage.getOMFactory();

        // Create the Error element
        final OMElement errorElement = f.createOMElement(Q_ELEMENT_NAME, signalmessage);

        // Fill it based on the given data

        // errorCode attribute is mandatory
        errorElement.addAttribute(ERROR_CODE_ATTR, error.getErrorCode(), null);
        // severity attribute is mandatory
        errorElement.addAttribute(SEVERITY_ATTR, error.getSeverity().toString(), null);

        // origin attribute
        final String origin = error.getOrigin();
        if (!Utils.isNullOrEmpty(origin))
            errorElement.addAttribute(ORIGIN_ATTR, origin, null);
        // category attribute
        final String category = error.getCategory();
        if (!Utils.isNullOrEmpty(category))
            errorElement.addAttribute(CATEGORY_ATTR, category, null);
        // refToMessageInError attribute
        final String refToMsg = error.getRefToMessageInError();
        if (!Utils.isNullOrEmpty(refToMsg))
            errorElement.addAttribute(REF_TO_ATTR, refToMsg, null);
        // shortDescription attribute
        final String errMsg = error.getMessage();
        if (!Utils.isNullOrEmpty(errMsg))
            errorElement.addAttribute(SHORT_DESCR_ATTR, errMsg, null);

        // Add ErrorDetail element directly as it very closely related to Error element
        final String errDetailText = error.getErrorDetail();
        if (!Utils.isNullOrEmpty(errDetailText)) {
            final OMElement errorDetail = f.createOMElement(Q_ERROR_DETAIL, errorElement);
            errorDetail.setText(errDetailText);
        }
        // Add Description element
        final IDescription errDescription = error.getDescription();
        if (errDescription != null && errDescription.getText() != null)
            DescriptionElement.createElement(errorElement, errDescription);

        return errorElement;
    }

    /**
     * Helper method for reading information from the <code>Error</code> element. This element contains the error
     * details which are stored in the {@link org.holodeckb2b.common.messagemodel.EbmsError} object.
     *
     * @param errorElement      The <code>Error</code> element to read the error details from
     * @return                  {@link org.holodeckb2b.common.messagemodel.EbmsError} object containing the data read
     *                          from the element
     */
    protected static EbmsError readErrorElement(final OMElement errorElement) {
        final EbmsError error = new EbmsError();

        // Read the attributes from the error element
        error.setCategory(errorElement.getAttributeValue(new QName(CATEGORY_ATTR)));
        error.setErrorCode(errorElement.getAttributeValue(new QName(ERROR_CODE_ATTR)));
        error.setOrigin(errorElement.getAttributeValue(new QName(ORIGIN_ATTR)));
        error.setMessage(errorElement.getAttributeValue(new QName(SHORT_DESCR_ATTR)));
        error.setRefToMessageInError(errorElement.getAttributeValue(new QName(REF_TO_ATTR)));
        // Convert text of attribute to enum value of entity object
        final String severity = errorElement.getAttributeValue(new QName(SEVERITY_ATTR));
        if (!Utils.isNullOrEmpty(severity))
            try {
            	error.setSeverity(IEbmsError.Severity.valueOf(severity.toLowerCase()));
            } catch (IllegalArgumentException unknownSeverity) {
            	// An unknown value has been provided for the Error's severity. 
            	// Ignore and don't use this attribute
            }

        // Read the ErrorDetail child element (if it exists)
        final OMElement errDetailElement = errorElement.getFirstChildWithName(Q_ERROR_DETAIL);
        if (errDetailElement != null)
            error.setErrorDetail(errDetailElement.getText());

        // Read the description element (if it exists)
        error.setDescription(DescriptionElement.readElement(
                DescriptionElement.getElement(errorElement)));

        return error;
    }
}
