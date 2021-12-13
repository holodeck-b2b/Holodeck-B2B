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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;

/**
 * Is a container holding the validation results for a message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class ValidationResult {

    /**
     * The collection of validation errors found in the message unit, grouped by validator
     */
    private Map<String, Collection<MessageValidationError>> validationErrors;
    /**
     * Indicator whether all specified validations have been performed or if validations was aborted after an error was
     * found
     */
    private boolean executedAllValidators;
    /**
     * Indicator whether the message unit should be rejected based on the validation errors found and the given
     * validation specification
     */
    private boolean shouldRejectMessage;

    /**
     * Creates a new <code>ValidationResult</code> that represents situation that all validations have been performed
     * and no errors were found.
     */
    public ValidationResult() {
        this.validationErrors = null;
        this.executedAllValidators = true;
        this.shouldRejectMessage = false;
    }

    /**
     * Get the collection of validation errors found in the message unit, grouped by validator
     *
     * @return The validation errors
     */
    public Map<String, Collection<MessageValidationError>> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Set the collection of validation errors found in the message unit, grouped by validator
     *
     * @param validationErrors The validation errors to set
     */
    public void setValidationErrors(final Map<String, Collection<MessageValidationError>> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Adds collection of validation errors found by a validator to the result.
     *
     * @param validator     The id of the validator that found the errors
     * @param errors        The errors found by the validator
     */
    public void addValidationErrors(final String validator, Collection<MessageValidationError> errors) {
        // Only add if there are errors
        if (!Utils.isNullOrEmpty(errors)) {
            if (validationErrors == null)
                validationErrors = new HashMap<>();
            validationErrors.put(validator, errors);
        }
    }

    /**
     * Indicates whether all specified validations have been performed or if validations was aborted after an error was
     * found
     *
     * @return <code>true</code> when all validators have run,<br><code>false</code> if validation stopped after error
     *         was found
     */
    public boolean executedAllValidators() {
        return executedAllValidators;
    }

    /**
     * Sets the indicator whether all specified validations have been performed or if validations was aborted after an
     * error was found
     *
     * @param executedAllValidators The indicator whether all specified validators have run
     */
    public void setExecutedAllValidators(final boolean executedAllValidators) {
        this.executedAllValidators = executedAllValidators;
    }

    /**
     * Indicates whether the message unit should be rejected based on the validation errors found and the given
     * validation specification
     *
     * @return <code>true</code> if the message unit should be rejected based on found errors and applied validation
     *         specification,<br><code>false</code> otherwise
     *
     */
    public boolean shouldRejectMessage() {
        return shouldRejectMessage;
    }

    /**
     * Set the indicator whether the message unit should be rejected based on the validation errors found and the given
     * validation specification
     *
     * @param shouldRejectMessage The indicator whether message unit should be rejected or not
     */
    public void setShouldRejectMessage(final boolean shouldRejectMessage) {
        this.shouldRejectMessage = shouldRejectMessage;
    }
}
