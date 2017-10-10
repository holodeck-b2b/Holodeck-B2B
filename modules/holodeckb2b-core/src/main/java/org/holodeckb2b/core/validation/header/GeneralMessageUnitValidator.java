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
package org.holodeckb2b.core.validation.header;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Provides validation of the ebMS header information that should be available in all message units.
 *
 * @author Sander Fieten <sander at chasquis-services.com>
 * @since  HB2B_NEXT_VERSION
 */
abstract class GeneralMessageUnitValidator implements IHeaderValidator {

    /**
     * Performs the requested validation of the generic ebMS header meta-data.
     *
     * @param messageUnit           The message unit which header must be validated
     * @param useStrictValidation   Indicates whether a strict validation should be performed or just a basic one
     * @return                      A string containing a description of the validation errors found, or an empty string
     *                              if no problems where found.
     */
    @Override
    public String validate(final IMessageUnit messageUnit, final boolean useStrictValidation) {
        StringBuilder    validationErrors = new StringBuilder();

        // Always perform the basic validation
        doBasicValidation(messageUnit, validationErrors);
        // And if needed perform strict validation as well
        if (useStrictValidation)
            doStrictValidation(messageUnit, validationErrors);

        // Return trimmed string so it is empty when nothing is reported
        return  validationErrors.toString().trim();
    }

    /**
     * Performs the basic validation of the generic ebMS header meta-data.
     * <p>Checks a MessageId and timestamp are available in the given message unit.
     *
     * @param messageUnit           The message unit which header must be validated
     * @param validationErrors      The string that is being build containing a description of all validation errors
     *                              found for the header in the message header
     */
    protected void doBasicValidation(final IMessageUnit messageUnit, final StringBuilder validationErrors) {

        if (Utils.isNullOrEmpty(messageUnit.getMessageId()))
            validationErrors.append("MessageId is missing\n");
        if (messageUnit.getTimestamp() == null)
            validationErrors.append("Timestamp is missing\n");
    }

    /**
     * Performs the strict validation of the generic ebMS header meta-data.
     * <p>
     *
     * @param messageUnit           The message unit which header must be validated
     * @param validationErrors      The string that is being build containing a description of all validation errors
     *                              found for the header in the message header
     */
    protected void doStrictValidation(final IMessageUnit messageUnit, final StringBuilder validationErrors) {
    }
}
