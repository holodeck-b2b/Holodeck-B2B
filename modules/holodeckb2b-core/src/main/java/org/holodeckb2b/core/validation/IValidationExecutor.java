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
package org.holodeckb2b.core.validation;

import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Defines the interface of the component responsible for performing the validation of message units. The input to the
 * executor are the message unit and the validation configuration containing the list of actual validators that must be
 * applied to the message unit.
 * <p>This executor is used for both the standard validation of the ebMS header and the custom validation of User
 * Message message units. For the first validation the validation configuration is static and built-in, for the custom
 * validation it is specified in the P-Mode.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see IMessageValidationSpecification
 */
public interface IValidationExecutor {

    /**
     * Executes the validations as specified and returns the collection of errors found by the validators and indicators
     * whether all validations have been performed and if the message  unit should be rejected based on the validation
     * specification.
     *
     * @param messageUnit       The message unit to be validated
     * @param validationSpec  The validation configuration specifying which validators should be executed
     * @return                  The validation result, or <code>null</code> when no validation was needed
     * @throws MessageValidationException   When a problem occurs during the validation of the User Message
     */
    ValidationResult validate(IMessageUnit messageUnit, IMessageValidationSpecification validationSpec)
                                                                                      throws MessageValidationException;
}
