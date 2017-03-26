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

import java.util.Iterator;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Provides basic validation of the ebMS header information specific for <i>Error</i> message units.
 * <p>This incudes a check on the general message meta-data and that the individual Error(s) contain(s) all required
 * information. The validation includes a check on the consistency of the referenced message which is defined in the
 * ebMS Core Specification (section 6.3) as follows:<br>
 * <i>"If <code>eb:RefToMessageId</code> is present as a child of <code>eb:SignalMessage/eb:MessageInfo</code>, then
 * every <code>eb:Error</code> element MUST be related to the ebMS message (message-in-error) identified by
 * <code>eb:RefToMessageId</code>.<br>
 * If the element <code>eb:SignalMessage/eb:MessageInfo</code> does not contain <code>eb:RefToMessageId</code>, then
 * the <code>eb:Error</code> element(s) MUST NOT be related to a particular ebMS message."</i>
 * <p>Holodeck B2B by default however allows a bit more flexibility by not checking on the second condition, i.e. it
 * allows an empty or absent <code>eb:SignalMessage/eb:RefToMessageId</code> together with <code>eb:Error</code>
 * element(s) that refer to a message unit. It does however require that all <code>eb:Error</code> elements refer the
 * same message unit, i.e. contain the same value for the <code>refToMessageInError</code> attribute.
 * <br>If you want to use the more strict check as defined in the specification you can turn this on by setting the
 * value of the <i>StrictErrorReferencesCheck</i> parameter in the Holodeck B2B configuration to <i>"true"</i>.
 * todo Rename to ErrorSignalValidator
 * @author Sander Fieten <sander at chasquis-services.com>
 * @since  2.2
 */
public class ErrorSignal {

    /**
     * Checks the meta-data on a Error signal to contain at least the required information for further processing.
     * <br>For the validation of the general meta-data like messageId and timestamp the {@link MessageUnit} is
     * used.
     *
     * @param errorInfo   The meta-data on the Error signal to check
     * @return            If a problem was found an {@link InvalidHeader} ebMS error is returned with the <code>
     *                    ErrorDetails</code> containing the list of found issues,<br>
     *                    If no problems are found <code>null</code> is returned.
     */
    public static InvalidHeader validate(final IErrorMessage errorInfo) {
        StringBuilder    errDetails = new StringBuilder();

        // First validate the general meta-data
        errDetails.append(MessageUnit.validate(errorInfo));

        // Then check that the individual errors contain the required information and have consistent references;
        // => If the RefToMessageId element from header:
        //      - contained a id: all individual ids should be the same to the signal level id or null;
        //      - is null: all individual ids should equal if non-strict checking, or should also be null if strict
        String refToMessageId = errorInfo.getRefToMessageId();
        final Iterator<IEbmsError> it = errorInfo.getErrors().iterator();
        boolean consistent = true;
        if (refToMessageId == null && !HolodeckB2BCoreInterface.getConfiguration().useStrictErrorRefCheck())
            // Signal level ref == null => all individual refs should be same, take first as leading
            refToMessageId = it.next().getRefToMessageInError();

        while (it.hasNext() && consistent)
            consistent = Utils.nullSafeEqual(refToMessageId, it.next().getRefToMessageInError());

        if (!consistent)
            errDetails.append("Error Signal contains inconsistent references\n");

        // Create the ebMS error if any problems were found
        String errorDetails = errDetails.toString().trim();
        if (!errorDetails.isEmpty())
            return new InvalidHeader(errorDetails);
        else
            return null;
    }
}
