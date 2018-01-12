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

import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Provides validation of the ebMS header information that should be available in all message units.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
abstract class GeneralMessageUnitValidator<M extends IMessageUnit> implements IMessageValidator<M> {
    /**
     * Indicator whether lax or strict validation should be applied
     */
    protected boolean useStrictValidation;

    /**
     * Create a new validator with the specified validation mode
     *
     * @param useStrictValidation   Indicates whether strict validation should be applied
     */
    GeneralMessageUnitValidator(final boolean useStrictValidation) {
        this.useStrictValidation = useStrictValidation;
    }

    /**
     * Performs the requested validation of the generic ebMS header meta-data.
     *
     * @param messageUnit   The message unit which header must be validated
     * @return              A Collection of {@link MessageValidationError}s when there are validation errors.<br>
     *                      When no problems were detected an empty Collection.
     * @throws MessageValidationException   When the validator can not complete the validation of the message unit
     */
    public Collection<MessageValidationError> validate(final M messageUnit) throws MessageValidationException {

        Collection<MessageValidationError>    validationErrors = new ArrayList<>();

        // Always perform the basic validation
        doBasicValidation(messageUnit, validationErrors);
        // And if needed perform strict validation as well
        if (useStrictValidation)
            doStrictValidation(messageUnit, validationErrors);

        return  validationErrors;
    }

    /**
     * Performs the basic validation of the generic ebMS header meta-data.
     * <p>Checks that <i>MessageId</i> and <i>Timestamp</i> are included in the given message unit. Note that although
     * the timestamp is missing from the <code>MessageUnit</code> object it may have been included in the message but
     * not in the correct format causing a parsing error when reading the header.
     *
     * @param messageUnit       The message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    protected void doBasicValidation(final M messageUnit, Collection<MessageValidationError> validationErrors) {

        if (Utils.isNullOrEmpty(messageUnit.getMessageId()))
            validationErrors.add(new MessageValidationError("MessageId is missing"));
        if (messageUnit.getTimestamp() == null)
            validationErrors.add(new MessageValidationError("Timestamp is missing or invalid"));
    }

    /**
     * Performs the strict validation of the ebMS header meta-data.
     * <p>Adds a check that values of the <i>MessageId</i> and <i>RefToMessageId</i> fields are according to the
     * message-id format as specified in RFC2822.
     *
     * @param messageUnit       The message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    protected void doStrictValidation(final M messageUnit,  Collection<MessageValidationError> validationErrors) {
        if (!MessageIdUtils.isCorrectFormat(messageUnit.getMessageId()))
            validationErrors.add(new MessageValidationError("MessageId value [" + messageUnit.getMessageId() +
                                                            "] does not conform to RCFC2822"));
        if (!Utils.isNullOrEmpty(messageUnit.getRefToMessageId())
           && !MessageIdUtils.isCorrectFormat(messageUnit.getRefToMessageId()))
            validationErrors.add(new MessageValidationError("RefToMessageId value [" + messageUnit.getRefToMessageId() +
                                                            "] does not conform to RCFC2822"));
    }
}
