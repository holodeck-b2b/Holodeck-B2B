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
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;

/**
 * Provides basic validation of the ebMS header information specific for <i>Pull Request</i> message units.
 * <p>For Pull Request this check only relates to the general message meta-data which must include the messageId and
 * timestamp, but must not include a reference to another message unit. Although a MPC is required for pulling it is not
 * required to be in the Pull Request (see section 5.2.3.1 of the ebMS V3 Core Spec). If an error is detected an ebMS
 * <i>InvalidHeader</i> is created and reported back.
 * todo Rename to PullRequestValidator
 * @author Sander Fieten <sander at chasquis-services.com>
 * @since  3.0.0
 */
public class PullRequest {

    /**
     * Checks the meta-data on a Pull Request to contain at least the required information for further processing.
     * <br>For the validation of the general meta-data like messageId and timestamp the {@link MessageUnit} is
     * used.
     *
     * @param pullReqInfo   The meta-data on the Pull Request to check
     * @return              If a problem was found an {@link InvalidHeader} ebMS error is returned with the <code>
     *                      ErrorDetails</code> containing the list of found issues,<br>
     *                      If no problems are found <code>null</code> is returned.
     */
    public static InvalidHeader validate(final IPullRequest pullReqInfo) {
        StringBuilder    errDetails = new StringBuilder();

        // First validate the general meta-data
        errDetails.append(MessageUnit.validate(pullReqInfo));

        // Check that no RefToMessageId is included
        if (!Utils.isNullOrEmpty(pullReqInfo.getRefToMessageId()))
            errDetails.append("There must be no RefToMessageId\n");

        // Create the ebMS error if any problems were found
        String errorDetails = errDetails.toString().trim();
        if (!errorDetails.isEmpty())
            return new InvalidHeader(errorDetails);
        else
            return null;
    }
}
