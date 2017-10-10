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
 * Provides the validation of the ebMS header information specific for <i>Pull Request</i> message units.
 *
 * @author Sander Fieten <sander at chasquis-services.com>
 * @since  HB2B_NEXT_VERSION
 */
class PullRequestValidator extends GeneralMessageUnitValidator  {

    /**
     * Performs the basic validation of the ebMS header meta-data specific for a Pull Request signal message unit.
     * <p>The basic validation only ensures that there is no <i>refToMessageId</i> in the header.
     *
     * @param messageUnit           The message unit which header must be validated
     * @param validationErrors      The string that is being build containing a description of all validation errors
     *                              found for the header in the message header
     */
    @Override
    protected void doBasicValidation(final IMessageUnit messageUnit, final StringBuilder validationErrors) {
        // First do genereal validation
        super.doBasicValidation(messageUnit, validationErrors);

        // Check that no RefToMessageId is included
        if (!Utils.isNullOrEmpty(messageUnit.getRefToMessageId()))
            validationErrors.append("There must be no RefToMessageId\n");
    }

    /**
     * Performs the strict validation of the ebMS header meta-data specific for a Pull Request signal message unit
     * <p>
     *
     * @param messageUnit           The message unit which header must be validated
     * @param validationErrors      The string that is being build containing a description of all validation errors
     *                              found for the header in the message header
     */
    @Override
    protected void doStrictValidation(final IMessageUnit messageUnit, final StringBuilder validationErrors) {
        // First do genereal validation
        super.doStrictValidation(messageUnit, validationErrors);


    }

}
