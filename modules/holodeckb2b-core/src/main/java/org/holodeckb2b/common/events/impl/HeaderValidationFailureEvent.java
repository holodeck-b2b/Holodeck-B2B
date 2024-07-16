/*
 * Copyright (C) 2018 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.events.impl;

import java.util.Collection;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.events.IHeaderValidationFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link IHeaderValidationFailure} to indicate that the header of the received message
 * unit is not valid.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public class HeaderValidationFailureEvent extends AbstractMessageProcessingFailureEvent<MessageValidationException>
																				implements IHeaderValidationFailure {

    /**
     * Holds the validation errors
     */
    private Collection<MessageValidationError> validationErrors;

    /**
     * Creates a new <code>HeaderValidationFailureEvent</code> to indicate that header validation of the given message
     * unit could not be completed due to an error that occurred during the validation.
     *
     * @param subject           The message unit for which the validation failed to complete
     * @param failureReason		{@link MessageValidationException} that caused the failure
     */
    public HeaderValidationFailureEvent(IMessageUnit subject, MessageValidationException failureReason) {
    	super(subject, failureReason);
    }

    /**
     * Creates a new <code>HeaderValidationFailureEvent</code> for the given message unit and validation information.
     *
     * @param subject               The message for which the validation failed
     * @param validationErrors      The errors found during the validation
     */
    public HeaderValidationFailureEvent(final IMessageUnit subject,
    									final Collection<MessageValidationError> validationErrors) {
        super(subject, null);
        if (Utils.isNullOrEmpty(validationErrors))
            throw new IllegalArgumentException("This event can only be raised if validation errors were found!");
        this.validationErrors = validationErrors;
    }

    @Override
    public Collection<MessageValidationError> getValidationErrors() {
        return validationErrors;
    }
}
