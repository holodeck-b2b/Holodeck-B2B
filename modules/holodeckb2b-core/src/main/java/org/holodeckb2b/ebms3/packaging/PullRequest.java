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

import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.general.Constants;

/**
 * Is a helper class for handling the ebMS PullRequest signal message elements 
 * in the ebMS SOAP header, i.e. the <code>eb:PullRequest</code> element and its
 * sibling <code>eb:MessageInfo</code>.
 * <p>This element is specified in section 5.2.3.1 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PullRequest {
    
    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(Constants.EBMS3_NS_URI, "PullRequest");
    
    /**
     * The local name of the mpc attribute
     */
    private static final String MPC_ATTR = "mpc";
    
    /**
     * Reads the information from <code>eb:PullRequest</code> element and sibling
     * <code>eb:MessageInfo</code> element that contains the PullRequest signal 
     * message unit and stores it a {@see PullRequest} object. 
     * <p><b>NOTE 1:</b> The {@see PullRequest} object also contains authentication
     * information to authorize the request. This info however is not part of the
     * <code>eb:SignalMessage</code> element and is therefor not filled here. 
     * <p><b>NOTE 2:</b> The information is stored in an entity object, but this
     * method will NOT persist the object.
     * 
     * @param prElement     The <code>eb:PullRequest</code> element 
     * @return              The {@see PullRequest} object containing the information
     *                      on the pull request
     * @throws PackagingException   When the given element does not conform to
     *                              ebMS specification and can therefor not be
     *                              read completely
     */
    public static org.holodeckb2b.ebms3.persistent.message.PullRequest readElement(OMElement prElement) throws PackagingException {
        // Create a new PullRequest entity object to store the information in
        org.holodeckb2b.ebms3.persistent.message.PullRequest prData = new org.holodeckb2b.ebms3.persistent.message.PullRequest();
        
        // The PullRequest itself only contains the [optional] mpc attribute
        String  mpc = prElement.getAttributeValue(new QName(MPC_ATTR));
        
        // If there was no mpc attribute or it was empty (which formally is 
        // illegal because the mpc should be a valid URI) it is set to the default MPC
        if (mpc == null || mpc.isEmpty())
            mpc = Constants.DEFAULT_MPC;
        prData.setMPC(mpc);

        // Beside the PullRequest element also the MessageInfo sibling should be 
        //  processed to get complete set of information
        MessageInfo.readElement(MessageInfo.getElement((OMElement) prElement.getParent()), prData);
        
        return prData;
    }
    
    /**
     * Gets the <code>eb:PullRequest</code> element from the given ebMS 3 Messaging 
     * header in the SOAP message. This method returns just one element because
     * there SHOULD be only one pull request signal message per ebMS message as 
     * described in section 5.2.3 of the ebMS V3 specification.
     * 
     * @param messaging   The SOAP Header block that contains the ebMS header,
     *                    i.e. the <code>eb:Messaging</code> element
     * @return      A {@see OMElement} representing the <code>eb:PullRequest</code> element 
     *              when one was found in the given header
     *              <code>null</code> if no such element was found
     */
    public static OMElement getElement(SOAPHeaderBlock messaging) {
        // Before we can get the PullRequest element we first have to get the parent
        //  SignalMessage element. Because a ebMS message can contain multiple signals
        //  we have to check each for the PullRequest
        Iterator<?> signals = org.holodeckb2b.ebms3.packaging.SignalMessage.getElements(messaging);
        
        // Search for the first PullRequest as there may only be one in an ebMS message
        OMElement pullReq = null;
        while((pullReq == null) && signals.hasNext())
            pullReq = ((OMElement) signals.next()).getFirstChildWithName(Q_ELEMENT_NAME);
        
        return pullReq;
    }       
    
    /**
     * Creates a new <code>eb:SignalMessage</code> for an <i>PullRequest</i> signal.
     * 
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param receipt       The information to include in the pull request signal
     * @return              The new element representing the pull request signal
     */
    public static OMElement createElement(OMElement messaging, org.holodeckb2b.ebms3.persistent.message.PullRequest pullRequest) {
        // First create the SignalMessage element that is the placeholder for
        // the Receipt element containing the receipt info
        OMElement signalmessage = SignalMessage.createElement(messaging);
        
        // Create the generic MessageInfo element
        MessageInfo.createElement(signalmessage, pullRequest);
        
        // Create the PullRequest element
        OMElement prElement = signalmessage.getOMFactory().createOMElement(Q_ELEMENT_NAME, signalmessage);
        
        // The only information specific to the PullRequest is the MPC on which the pull takes place
        prElement.addAttribute(MPC_ATTR, pullRequest.getMPC(), null);
        
        return signalmessage;        
    }
    
}
