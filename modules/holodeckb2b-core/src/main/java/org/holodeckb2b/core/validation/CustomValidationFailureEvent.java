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
import java.util.Map;

import org.holodeckb2b.common.events.impl.AbstractMessageProcessingEvent;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.events.ICustomValidationFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link ICustomValidationFailure} to indicate that the custom validation of a
 * message unit failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class CustomValidationFailureEvent extends AbstractMessageProcessingEvent implements ICustomValidationFailure {

    /**
     * Holds the indicator whether all specified validators were executed
     */
    private boolean executedAllValidators;
    /**
     * Holds the indicator whether the message unit was rejected due to the validation errors
     */
    private boolean isMessageRejected;
    /**
     * Holds the validation errors reported by the validators
     */
    private Map<String, Collection<MessageValidationError>> validationErrors;

    /**
     * Creates a new <code>CustomValidationFailureEvent</code> for the given message unit and validation information.
     *
     * @param subject               The message for which the validation failed
     * @param validationResult      The results of the validation
     */
    public CustomValidationFailureEvent(IMessageUnit subject, ValidationResult validationResult) {
        super(subject);
        if (Utils.isNullOrEmpty(validationResult.getValidationErrors()))
            throw new IllegalArgumentException("This event can only be raised if validation errors were found!");
        this.validationErrors = validationResult.getValidationErrors();
        this.executedAllValidators = validationResult.executedAllValidators();
        this.isMessageRejected = validationResult.shouldRejectMessage();
    }

    @Override
    public Map<String, Collection<MessageValidationError>> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public boolean executedAllValidators() {
        return executedAllValidators;
    }

    @Override
    public boolean isMessageRejected() {
        return isMessageRejected;
    }

}
