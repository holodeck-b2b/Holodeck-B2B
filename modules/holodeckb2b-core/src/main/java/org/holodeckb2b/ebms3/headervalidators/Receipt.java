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
package org.holodeckb2b.ebms3.headervalidators;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;

/**
 * Provides basic validation of the ebMS header information specific for <i>Receipt</i> message units.
 * <p>This incudes a check on the general message meta-data, including the reference to another message unit and that
 * the Receipt contains information. Because the specification does not state which information the Receipt should
 * contain, this class only checks there is content. If an error is detected an ebMS <i>InvalidHeader</i> is created and
 * reported back.
 * todo Rename to ReceiptValidator
 * @author Sander Fieten (sander at chasquis-services.com)
 * @since  HB2B_NEXT_VERSION
 */
public class Receipt {

    /**
     * Checks the meta-data on a Receipt to contain at least the required information for further processing.
     * <br>For the validation of the general meta-data like messageId and timestamp the {@link MessageUnit} is
     * used.
     *
     * @param receiptInfo   The meta-data on the Receipt to check
     * @return              If a problem was found an {@link InvalidHeader} ebMS error is returned with the <code>
     *                      ErrorDetails</code> containing the list of found issues,<br>
     *                      If no problems are found <code>null</code> is returned.
     */
    public static InvalidHeader validate(final IReceipt receiptInfo) {
        StringBuilder    errDetails = new StringBuilder();
        // First validate the general meta-data
        errDetails.append(MessageUnit.validate(receiptInfo));

        // Check that a RefToMessageId is included
        if (Utils.isNullOrEmpty(receiptInfo.getRefToMessageId()))
            errDetails.append("RefToMessageId is missing\n");
        if (Utils.isNullOrEmpty(receiptInfo.getContent()))
            errDetails.append("Receipt content is missing\n");

        // Create the ebMS error if any problems were found
        String errorDetails = errDetails.toString().trim();
        if (!errorDetails.isEmpty())
            return new InvalidHeader(errorDetails);
        else
            return null;
    }
}
