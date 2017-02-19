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

import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;

/**
 * Is a helper class for handling the ebMS Pull Request signal message units in the ebMS SOAP header, i.e. the
 * <code>eb:PullRequest</code> element and its sibling <code>eb:MessageInfo</code>.
 * <p>This element is specified in section 5.2.3.1 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PullRequestElement {

    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PullRequest");

    /**
     * The local name of the mpc attribute
     */
    private static final String MPC_ATTR = "mpc";

    /**
     * Reads the information from <code>eb:PullRequest</code> element and sibling <code>eb:MessageInfo</code> element
     * that contains the Pull Request signal message unit and stores it a {@link
     * org.holodeckb2b.common.messagemodel.PullRequest} object.
     *
     * @param prElement     The <code>eb:PullRequest</code> element to read info from
     * @return              The {@link org.holodeckb2b.common.messagemodel.PullRequest} object containing the
     *                      information on the pull request
     */
    public static org.holodeckb2b.common.messagemodel.PullRequest readElement(final OMElement prElement) {
        // Create a new PullRequest entity object to store the information in
        final PullRequest prData = new PullRequest();

        // The PullRequest itself only contains the [optional] mpc attribute
        String  mpc = prElement.getAttributeValue(new QName(MPC_ATTR));

        // If there was no mpc attribute or it was empty (which formally is
        // illegal because the mpc should be a valid URI) it is set to the default MPC
        prData.setMPC(Utils.isNullOrEmpty(mpc) ? EbMSConstants.DEFAULT_MPC : mpc);

        // Beside the PullRequest element also the MessageInfo sibling should be
        //  processed to get complete set of information
        MessageInfoElement.readElement(
                MessageInfoElement.getElement(
                        (OMElement) prElement.getParent()), prData);

        return prData;
    }

    /**
     * Gets the <code>eb:PullRequest</code> element from the given ebMS 3 Messaging header in the SOAP message. This
     * method returns just one element because there SHOULD be only one pull request signal message per ebMS message as
     * described in section 5.2.3 of the ebMS V3 specification.
     *
     * @param messaging   The SOAP Header block that contains the ebMS header,i.e. the <code>eb:Messaging</code> element
     * @return      A {@link OMElement} representing the <code>eb:PullRequest</code> element when one was found in the
     *              given header, or<br>
     *              <code>null</code> if no such element was found
     */
    public static OMElement getElement(final SOAPHeaderBlock messaging) {
        // Before we can get the PullRequest element we first have to get the parent SignalMessage element. Because a
        // ebMS message can contain multiple signals we have to check each for the PullRequest
        final Iterator<?> signals = SignalMessageElement.getElements(messaging);

        // Search for the first PullRequest as there may only be one in an ebMS message
        OMElement pullReq = null;
        while((pullReq == null) && signals.hasNext())
            pullReq = ((OMElement) signals.next()).getFirstChildWithName(Q_ELEMENT_NAME);

        return pullReq;
    }

    /**
     * Creates a new <code>eb:SignalMessage</code> for a <i>PullRequest</i> signal message unit.
     *
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param pullRequest   The information to include in the pull request signal
     * @return              The new element representing the pull request signal
     */
    public static OMElement createElement(final OMElement messaging, final IPullRequest pullRequest) {
        // First create the SignalMessage element that is the placeholder for
        // the Receipt element containing the receipt info
        final OMElement signalmessage = SignalMessageElement.createElement(messaging);

        // Create the generic MessageInfo element
        MessageInfoElement.createElement(signalmessage, pullRequest);

        // Create the PullRequest element
        final OMElement prElement = signalmessage.getOMFactory().createOMElement(Q_ELEMENT_NAME, signalmessage);

        // The only information specific to the PullRequest is the MPC on which the pull takes place
        prElement.addAttribute(MPC_ATTR, pullRequest.getMPC(), null);

        return signalmessage;
    }

}
