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

import java.util.Collection;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;

/**
 * Provides the validation of the ebMS header information specific for <i>Pull Request</i> message units.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
class PullRequestValidator extends GeneralMessageUnitValidator<IPullRequest>
                           implements IMessageValidator<IPullRequest> {

    PullRequestValidator(boolean useStrictValidation) {
        super(useStrictValidation);
    }

    /**
     * Performs the basic validation of the ebMS header meta-data specific for a Pull Request signal message unit.
     * <p>The basic validation only ensures that there is no <i>refToMessageId</i> in the header.
     *
     * @param messageUnit       The Pull Request message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    @Override
    protected void doBasicValidation(final IPullRequest messageUnit,
                                     Collection<MessageValidationError> validationErrors) {
        // First do genereal validation
        super.doBasicValidation(messageUnit, validationErrors);

        // Check that no RefToMessageId is included
        if (!Utils.isNullOrEmpty(messageUnit.getRefToMessageId()))
            validationErrors.add(new MessageValidationError("There must be no RefToMessageId"));
    }

    /**
     * Performs the strict validation of the ebMS header meta-data specific for a Pull Request signal message unit
     * <p>
     *
     * @param messageUnit       The Pull Request message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    @Override
    protected void doStrictValidation(final IPullRequest messageUnit,
                                      Collection<MessageValidationError> validationErrors) {
        // First do general validation
        super.doStrictValidation(messageUnit, validationErrors);
    }
}
