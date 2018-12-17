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
import java.util.Iterator;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Provides the validation of the ebMS header information specific for <i>Error Signal</i> message units.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  4.0.0
 */
public class ErrorSignalValidator extends GeneralMessageUnitValidator<IErrorMessage>
                           implements IMessageValidator<IErrorMessage> {

    public ErrorSignalValidator(boolean useStrictValidation) {
        super(useStrictValidation);
    }

    /**
     * Checks the meta-data on a Error signal to contain at least the required information for further processing.
     * <p>This incudes a check on the consistency of the referenced message so that the Error Signal referenced at most
     * one message unit. Note that the ebMS Core Specification also describes a consistency check (in section 6.3):<br>
     * <i>"If <code>eb:RefToMessageId</code> is present as a child of <code>eb:SignalMessage/eb:MessageInfo</code>, then
     * every <code>eb:Error</code> element MUST be related to the ebMS message (message-in-error) identified by
     * <code>eb:RefToMessageId</code>.<br>
     * If the element <code>eb:SignalMessage/eb:MessageInfo</code> does not contain <code>eb:RefToMessageId</code>, then
     * the <code>eb:Error</code> element(s) MUST NOT be related to a particular ebMS message."</i>
     * <p>Holodeck B2B by default however allows a bit more flexibility by not checking on the second condition, i.e. it
     * allows an empty or absent <code>eb:SignalMessage/eb:RefToMessageId</code> together with <code>eb:Error</code>
     * element(s) that refer to a message unit. It does however require that all <code>eb:Error</code> elements refer
     * the same message unit, i.e. contain the same value for the <code>refToMessageInError</code> attribute.
     * <br>The more strict check as defined in the ebMS Specification is only used in the strict validation mode.
     *
     * @param messageUnit   The Error Signal message unit which header must be validated
     * @return              A Collection of {@link MessageValidationError}s when there are validation errors.<br>
     *                      When no problems were detected an empty Collection.
     * @throws MessageValidationException   When the validator can not complete the validation of the message unit
     */
    @Override
    public Collection<MessageValidationError> validate(final IErrorMessage messageUnit)
                                                                                    throws MessageValidationException {
        // First do generic validations
        Collection<MessageValidationError> validationErrors = super.validate(messageUnit);

        // When in strict validation mode check that the error code and severity are available
        if (useStrictValidation && !messageUnit.getErrors().stream()
                                     .allMatch(e -> !Utils.isNullOrEmpty(e.getErrorCode()) && e.getSeverity() != null))
            validationErrors.add(new MessageValidationError("Error code or severity not set for all Errors!"));

        // Then check that the signal reference at most one other message unit
        // @todo: Remove use of specific config parameter on reference check
        if (!checkReferences(messageUnit, useStrictValidation
                                          || HolodeckB2BCoreInterface.getConfiguration().useStrictErrorRefCheck()))
            validationErrors.add(new MessageValidationError("Error Signal contains inconsistent references"));

        // Return validation results
        return validationErrors;
    }

    /**
     * Helper method to check the consistency of the references in the Error Signal.
     * <p>If the RefToMessageId element from header:<ul>
     * <li>contained an id: all individual ids should be the same to the signal level id or null;</li>
     * <li>is null: all individual ids should be equal for basic validations or null for strict validation.</li>
     * </ul>
     *
     * @param errorSignal   The error signal to check
     * @param strict        Indicator whether to use strict validation
     * @return              <code>true</code> if references are consistent,<code>false</code> if not
     */
    private boolean checkReferences(final IErrorMessage errorSignal, final boolean strict) {
        String refToMessageId = errorSignal.getRefToMessageId();
        final Iterator<IEbmsError> it = errorSignal.getErrors().iterator();
        boolean consistent = true;
        boolean allowNull = false;
        if (Utils.isNullOrEmpty(refToMessageId)
                && !HolodeckB2BCoreInterface.getConfiguration().useStrictErrorRefCheck()) {
            // Signal level ref == null => all individual refs should be same, take first as leading
            refToMessageId = it.next().getRefToMessageInError();
        } else
            allowNull = true;

        while (it.hasNext() && consistent) {
            final String errorRefTo = it.next().getRefToMessageInError();
            consistent = (allowNull && Utils.isNullOrEmpty(errorRefTo))
                                                                    || Utils.nullSafeEqual(refToMessageId, errorRefTo);
        }

        // Return the validation results
        return consistent;
    }
}
