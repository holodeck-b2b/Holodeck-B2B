/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.headervalidation;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Defines the interface of a <i>ebMS header validator</i> that checks whether the ebMS message header of a specific
 * type of message unit conforms to the ebMS specifications.
 * <p>The interface contains just one method to validate the message header. There is no separate initialization, the
 * method should be self contained and not use class variables. The {@link HeaderValidatorFactory} will create just one
 * instance of each header validator.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public interface IHeaderValidator {

    /**
     * Validates the ebMS header meta-data of the provided message unit.
     * <p>Validators support two modes of validation, <i>basic</i> and <i>strict</i> with the first only checking that
     * the meta-data enables processing of the message unit by Holodeck B2B and strict applying all rules from the ebMS
     * Specifications.
     *
     * @param messageUnit           The message unit which header must be validated
     * @param useStrictValidation   Indicates whether a strict validation should be performed or just a basic one
     * @return                      A string containing a description of the validation errors found, or an empty string
     *                              if no problems where found.
     */
    public String validate(final IMessageUnit messageUnit, final boolean useStrictValidation);
}
