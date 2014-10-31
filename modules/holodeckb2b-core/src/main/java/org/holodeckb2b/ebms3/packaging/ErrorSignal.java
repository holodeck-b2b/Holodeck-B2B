/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.general.IDescription;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.common.messagemodel.IErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;

/**
 * Is a helper class for handling the ebMS error signals in the ebMS SOAP header.
 * The error signal message unit is a bit different from the other signal message 
 * units as it can contain multiple <code>eb:Error</code> elements with one sibling
 * <code>eb:MessageInfo</code> element under the parent <code>eb:SignalMessage</code>
 * element.
 * <p>The Error signal message unit is specified in section 6.2 and 6.3 of the 
 * ebMS 3 Core specification.
 * <p>NOTE: The naming of this class differs from the other classes in this packages
 * because <code>Error</code> is a reserved class name.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ErrorSignal {
    
    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(Constants.EBMS3_NS_URI, "Error");
    
    /**
     * The fully qualified name of the ErrorDetail element as an {@see QName}
     */
    private static final QName  Q_ERROR_DETAIL = new QName(Constants.EBMS3_NS_URI, "ErrorDetail");

    /*
     * The local names of the attributes of the Error element
     */
    private static final String CATEGORY_ATTR = "category";
    private static final String REF_TO_ATTR = "refToMessageInError";
    private static final String ERROR_CODE_ATTR = "errorCode";
    private static final String ORIGIN_ATTR = "origin";
    private static final String SEVRITY_ATTR = "severity";
    private static final String SHORT_DESCR_ATTR = "shortDescription";
    
    /**
     * Creates a new <code>eb:SignalMessage</code> for an <i>Error</i> signal.
     * 
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param errorMU       The information of the error signal to include 
     * @return              The new element representing the error signal
     */
    public static OMElement createElement(OMElement messaging, IErrorMessage errorMU) {
        // First create the SignalMessage element that is the placeholder for
        // the Error elements containing the error info
        OMElement signalmessage = SignalMessage.createElement(messaging);
        
        // Create the generice MessageInfo element
        MessageInfo.createElement(signalmessage, errorMU);
        
        // Now create an Error element for each error in the error message unit
        for(IEbmsError error : errorMU.getErrors())
            createErrorElement(signalmessage, error);
        
        return signalmessage;        
    }
    
    /**
     * Reads the information from a <code>eb:SignalMessage</code> and its child
     * elements that contain the Error signal message unit and stores it a 
     * {@see ErrorMessage} object. 
     * <p><b>NOTE:</b> The information is stored in an entity object, but this
     * method will NOT persist the object.
     * 
     * @param sigElement    The parent <code>eb:SignalMessage</code> element that contains the <code>eb:Error</code> elements 
     * @return              The {@link ErrorMessage} object containing the information on the Error signal
     */
    public static org.holodeckb2b.ebms3.persistent.message.ErrorMessage readElement(OMElement sigElement) throws PackagingException {
        // Create a new EbmsError entity object to store the information in
        org.holodeckb2b.ebms3.persistent.message.ErrorMessage errData = new org.holodeckb2b.ebms3.persistent.message.ErrorMessage();
        
        // First read general information from the MessageInfo child 
        MessageInfo.readElement(MessageInfo.getElement(sigElement), errData);
        
        // Now get all child Error elements
        Iterator it = sigElement.getChildrenWithName(Q_ELEMENT_NAME);
        while (it.hasNext()) {
            OMElement errorElement = (OMElement) it.next();
            errData.addError(readErrorElement(errorElement));
        }    
        
        return errData;
    }
    
    /**
     * Gets an {@see Iterator} for all <code>eb:SignalMessage</code> elements 
     * from the given ebMS 3 Messaging header in the SOAP message that represent
     * <i>Error</i> signals.
     * 
     * @param messaging   The SOAP Header block that contains the ebMS header,
     *                    i.e. the <code>eb:Messaging</code> element
     * @return      An {@see Iterator} for all {@see OMElement}s representing a 
     *              <code>eb:SignalMessage</code> element that contains an 
     *              Error signal, i.e. has one or more <code>eb:Error</code> 
     *              child elements  
     */
    public static Iterator getElements(SOAPHeaderBlock messaging) {
        // Check all SignalMessage elements in the header
        Iterator signals = org.holodeckb2b.ebms3.packaging.SignalMessage.getElements(messaging);
        
        ArrayList<OMElement>  errors = new ArrayList<OMElement>();
        while(signals.hasNext()) {
            OMElement signal  = (OMElement) signals.next();
            // If this SignalMessage element has at least one Error child, 
            //   it is an Error signal and should be returned
            if (signal.getFirstChildWithName(Q_ELEMENT_NAME) != null)
                errors.add(signal);
        }
        
        return errors.iterator();
    }       
    
    /**
     * Helper method for creating an <code>eb:Error</code> element in the 
     * Error signal message, i.e. the <code>eb:SignalMessage</code> that contains
     * the errors. 
     * 
     * @param signalmessage     The {@see OMElement} parent object for the new
     *                          element
     * @param error             The data to write to the element
     * @return                  An {@see OMElement} object representing the 
     *                          <code>eb:Error</code> element containing the error 
     *                          data 
     */
    protected static OMElement createErrorElement(OMElement signalmessage, IEbmsError error) {
        OMFactory f = signalmessage.getOMFactory();
        
        // Create the Error element
        OMElement errorElement = f.createOMElement(Q_ELEMENT_NAME, signalmessage);
        
        // Fill it based on the given data
        
        // errorCode attribute is mandatory
        errorElement.addAttribute(ERROR_CODE_ATTR, error.getErrorCode(), null);
        // severity attribute is mandatory
        errorElement.addAttribute(SEVRITY_ATTR, error.getSeverity().toString(), null);
        
        // origin attribute
        String origin = error.getOrigin();
        if (origin != null && !origin.isEmpty())
            errorElement.addAttribute(ORIGIN_ATTR, origin, null);
        // category attribute
        String category = error.getCategory();
        if (category != null && !category.isEmpty())
            errorElement.addAttribute(CATEGORY_ATTR, category, null);
        // refToMessageInError attribute
        String refToMsg = error.getRefToMessageInError();
        if (refToMsg != null && !refToMsg.isEmpty())
            errorElement.addAttribute(REF_TO_ATTR, refToMsg, null);
        // shortDescription attribute
        String errMsg = error.getMessage();
        if (errMsg != null && !errMsg.isEmpty())
            errorElement.addAttribute(SHORT_DESCR_ATTR, errMsg, null);
        
        // Add ErrorDetail element directly as it very closely related to Error element
        String errDetailText = error.getErrorDetail();
        if (errDetailText != null && !errDetailText.isEmpty()) {
            OMElement errorDetail = f.createOMElement(Q_ERROR_DETAIL, errorElement);
            errorDetail.setText(errDetailText);
        }
        // Add Description element
        IDescription errDescription = error.getDescription();
        if (errDescription != null && errDescription.getText() != null)
            Description.createElement(errorElement, errDescription);
        
        return errorElement;
    }
    
    /**
     * Helper method for reading information from the <code>Error</code> element.
     * This element contains the error details which are stored in the {@see EbmsError}
     * entity object.
     * 
     * @param errorElement      The <code>Error</code> element to read the error details from
     * @return                  {@see EbmsError} object containing the data read
     *                          from the element
     */
    protected static EbmsError readErrorElement(OMElement errorElement) {
        EbmsError   error = new EbmsError();
        
        // Read the attributes from the error element
        error.setCategory(errorElement.getAttributeValue(new QName(CATEGORY_ATTR)));
        error.setErrorCode(errorElement.getAttributeValue(new QName(ERROR_CODE_ATTR)));
        error.setOrigin(errorElement.getAttributeValue(new QName(ORIGIN_ATTR)));
        error.setRefToMessageInError(errorElement.getAttributeValue(new QName(REF_TO_ATTR)));
        // Convert text of attribute to enum value of entity object
        error.setSeverity( EbmsError.Severity.FAILURE.toString()
                                .equalsIgnoreCase(errorElement.getAttributeValue(new QName(SEVRITY_ATTR))) 
                           ? EbmsError.Severity.FAILURE : EbmsError.Severity.WARNING);
        
        // Read the ErrorDetail child element (if it exists)
        OMElement errDetailElement = errorElement.getFirstChildWithName(Q_ERROR_DETAIL);
        if (errDetailElement != null)
            error.setErrorDetail(errDetailElement.getText());
        
        // Read the description element (if it exists)
        error.setDescription(Description.readElement(Description.getElement(errorElement)));
        
        return error;
    }
}
