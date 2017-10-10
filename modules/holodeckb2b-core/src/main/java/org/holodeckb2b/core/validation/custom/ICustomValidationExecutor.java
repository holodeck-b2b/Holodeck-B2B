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
package org.holodeckb2b.core.validation.custom;

import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.events.ICustomValidationFailedEvent;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Defines the interface of the component responsible for performing the custom validation of a User Message message
 * unit as defined by its P-Mode. Implementations MUST also raise the {@link ICustomValidationFailedEvent} event when
 * the validation fails.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see IMessageValidationSpecification
 */
public interface ICustomValidationExecutor {

    /**
     * Executes the custom validations as specified in the P-Mode of the given User Message and returns the collection
     * of errors found by the validators and indicators whether all validations have been performed and if the message
     * unit should be rejected based on the validation specification.
     *
     * @param userMessage   The User Message message unit to be validated
     * @return              The validation result, or <code>null</code> when no validation was needed
     * @throws MessageValidationException   When a problem occurs during the validation of the User Message
     */
    ValidationResult validate(IUserMessage userMessage) throws MessageValidationException;
}
