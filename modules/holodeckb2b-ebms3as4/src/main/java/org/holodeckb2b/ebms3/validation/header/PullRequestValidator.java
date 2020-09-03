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
package org.holodeckb2b.ebms3.validation.header;

import java.util.Collection;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;

/**
 * Provides the validation of the ebMS header information specific for <i>Pull Request</i> message units.
 * <p>As there are no specific requirements on <i>Pull Request</i>s to process them, there is no override for the {@link
 * GeneralMessageUnitValidator#doBasicValidation(IMessageUnit, Collection)} method.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  4.0.0
 */
class PullRequestValidator extends GeneralMessageUnitValidator<IPullRequest>
                           implements IMessageValidator<IPullRequest> {

    PullRequestValidator(boolean useStrictValidation) {
        super(useStrictValidation);
    }

    /**
     * Performs the strict validation of the ebMS header meta-data specific for a Pull Request signal message unit
     * <p>In addition to the general checks for a message unit it ensures that there is no <i>refToMessageId</i> in the
     * header and also checks that the given MPC is a valid URI.
     *
     * @param messageUnit       The Pull Request message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    @Override
    protected void doStrictValidation(final IPullRequest messageUnit,
                                      Collection<MessageValidationError> validationErrors) {
        // First do general validation
        super.doStrictValidation(messageUnit, validationErrors);

        // Check that no RefToMessageId is included
        if (!Utils.isNullOrEmpty(messageUnit.getRefToMessageId()))
            validationErrors.add(new MessageValidationError("There must be no RefToMessageId"));
        if (!Utils.isValidURI(messageUnit.getMPC()))
            validationErrors.add(new MessageValidationError("Specified MPC [" + messageUnit.getMPC()
                                                            + "] is not a valid URI"));        
    }
}
