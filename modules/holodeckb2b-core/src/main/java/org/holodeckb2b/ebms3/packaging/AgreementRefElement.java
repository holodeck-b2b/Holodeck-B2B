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

import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;

/**
 * Is a helper class for handling the ebMS <code>AgreementRef</code> element in the ebMS SOAP header.
 * <p>This element is specified in section 5.2.2.7 of the ebMS 3 Core specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class AgreementRefElement {

    /**
     * The fully qualified name of the element as an {@link QName}
     */
    public static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");

    // The local name of the type attribute
    public static final String LN_ATTR_TYPE = "type";

    // The local name of the pmode attribute
    public static final String LN_ATTR_PMODE = "pmode";

    /**
     * Creates a <code>AgreementRef</code> element and adds it to the given <code>CollaborationInfo</code> element.
     *
     * @param ciElement     The <code>CollaborationInfo</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement ciElement, final IAgreementReference data) {
        final OMFactory f = ciElement.getOMFactory();

        // Create the element
        final OMElement agreementRef = f.createOMElement(Q_ELEMENT_NAME, ciElement);

        // Fill it based on the given data
        agreementRef.setText(data.getName());

        // Set attributes if data is specified for it
        final String type = data.getType();
        if ( type != null && !type.isEmpty())
            agreementRef.addAttribute(LN_ATTR_TYPE, type, null);
        final String pmode = data.getPModeId();
        if ( pmode != null && !pmode.isEmpty())
            agreementRef.addAttribute(LN_ATTR_PMODE, pmode, null);

        return agreementRef;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>AgreementRef</code> child element of the given <code>
     * CollaborationInfo</code> element.
     *
     * @param ciElement     The parent <code>CollaborationInfo</code> element
     * @return              The {@link OMElement} object representing the <code>AgreementRef</code> element, or<br>
     *                      <code>null</code> when there is no <code>AgreementRef</code> element as child of the
     *                      given element.
     */
    public static OMElement getElement(final OMElement ciElement) {
        return ciElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the information from the <code>AgreementRef</code> element and returns it in a new {@link
     * AgreementReference} object.
     *
     * @param element  The <code>AgreementRef</code> element to read the info from
     * @return         A new {@link }
     *                               object containing the service info from the
     *                               element
     */
    public static AgreementReference readElement(final OMElement element) {
        if (element == null)
            return null;

        // Read agreement info, i.e. name and type of reference
        final String agreement = element.getText();
        final String type = element.getAttributeValue(new QName(LN_ATTR_TYPE));
        // Read P-Mode id
        final String pmode = element.getAttributeValue(new QName(LN_ATTR_PMODE));

        // Create and return the object
        return new AgreementReference(agreement, type, pmode);
    }
}
